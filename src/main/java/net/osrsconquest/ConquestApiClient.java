package net.osrsconquest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.osrsconquest.model.ClanEvent;
import net.osrsconquest.model.MemberInfo;
import net.osrsconquest.model.RegisterResponse;
import net.osrsconquest.model.StatSnapshot;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class ConquestApiClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final OSRSConquestConfig config;

	@Inject
	public ConquestApiClient(OkHttpClient httpClient, Gson gson, OSRSConquestConfig config)
	{
		this.config = config;
		this.gson = gson;
		this.httpClient = httpClient;
	}

	private String getApiUrl()
	{
		String url = config.apiUrl();
		if (!url.startsWith("https://"))
		{
			log.error("OSRS Conquest API URL must use HTTPS. Current URL: {}", url);
			return "https://conquest.projectonyx.net/api"; // Fall back to default
		}
		return url;
	}

	private Request.Builder authedRequest(String path)
	{
		return new Request.Builder()
			.url(getApiUrl() + path)
			.header("Authorization", "Bearer " + config.apiKey());
	}

	public void register(String rsn, String clanName, Consumer<RegisterResponse> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		body.addProperty("rsn", rsn);
		body.addProperty("clanName", clanName);

		Request request = new Request.Builder()
			.url(getApiUrl() + "/register")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Registration failed: {}", e.getMessage());
				onError.accept(e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						onError.accept("HTTP " + response.code());
						return;
					}
					RegisterResponse reg = gson.fromJson(responseBody.string(), RegisterResponse.class);
					onSuccess.accept(reg);
				}
			}
		});
	}

	public void submitEvents(List<ClanEvent> events)
	{
		if (events.isEmpty())
		{
			return;
		}

		JsonObject body = new JsonObject();
		body.add("events", gson.toJsonTree(events));

		Request request = authedRequest("/events")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to submit events: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
				if (!response.isSuccessful())
				{
					log.warn("Event submission returned HTTP {}", response.code());
				}
			}
		});
	}

	public void submitLogin(int world)
	{
		JsonObject body = new JsonObject();
		body.addProperty("world", world);

		Request request = authedRequest("/sessions/login")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Login report failed: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	public void submitLogout()
	{
		Request request = authedRequest("/sessions/logout")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Logout report failed: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	public void submitStats(List<StatSnapshot> stats)
	{
		if (stats.isEmpty())
		{
			return;
		}

		JsonObject body = new JsonObject();
		body.add("stats", gson.toJsonTree(stats));

		Request request = authedRequest("/stats")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Stat submission failed: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	public void syncMembers(List<MemberInfo> members)
	{
		if (members.isEmpty())
		{
			return;
		}

		JsonObject body = new JsonObject();
		body.add("members", gson.toJsonTree(members));

		Request request = authedRequest("/members/sync")
			.post(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Member sync failed: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	public void fetchRecentEvents(Consumer<List<Map<String, Object>>> onSuccess)
	{
		Request request = authedRequest("/events?limit=50")
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to fetch events: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						return;
					}
					JsonObject json = gson.fromJson(responseBody.string(), JsonObject.class);
					Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
					List<Map<String, Object>> events = gson.fromJson(json.get("events"), listType);
					onSuccess.accept(events);
				}
			}
		});
	}

	public void fetchMembers(Consumer<List<Map<String, Object>>> onSuccess)
	{
		Request request = authedRequest("/members")
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to fetch members: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						return;
					}
					JsonObject json = gson.fromJson(responseBody.string(), JsonObject.class);
					Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
					List<Map<String, Object>> members = gson.fromJson(json.get("members"), listType);
					onSuccess.accept(members);
				}
			}
		});
	}

	public void fetchLeaderboard(String type, String period, Consumer<List<Map<String, Object>>> onSuccess)
	{
		Request request = authedRequest("/leaderboard/" + type + "?period=" + period)
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to fetch leaderboard: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						return;
					}
					JsonObject json = gson.fromJson(responseBody.string(), JsonObject.class);
					Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
					List<Map<String, Object>> leaderboard = gson.fromJson(json.get("leaderboard"), listType);
					onSuccess.accept(leaderboard);
				}
			}
		});
	}

	// ─── Dashboard API ───

	public void generateManagementLink(Consumer<String> onSuccess, Consumer<String> onError)
	{
		Request request = authedRequest("/clan/management-link")
			.post(RequestBody.create(JSON, "{}"))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to generate management link: {}", e.getMessage());
				onError.accept(e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						String errorMsg = "HTTP " + response.code();
						if (responseBody != null)
						{
							try
							{
								JsonObject err = gson.fromJson(responseBody.string(), JsonObject.class);
								if (err.has("error"))
								{
									errorMsg = err.get("error").getAsString();
								}
							}
							catch (Exception ignored) {}
						}
						onError.accept(errorMsg);
						return;
					}
					JsonObject json = gson.fromJson(responseBody.string(), JsonObject.class);
					String url = json.get("url").getAsString();
					onSuccess.accept(url);
				}
			}
		});
	}

	// ─── Webhook API ───

	public void fetchWebhook(Consumer<Map<String, Object>> onSuccess, Consumer<String> onError)
	{
		Request request = authedRequest("/webhook")
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to fetch webhook: {}", e.getMessage());
				onError.accept(e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (response.code() == 404)
					{
						onSuccess.accept(null);
						return;
					}
					if (!response.isSuccessful() || responseBody == null)
					{
						onError.accept("HTTP " + response.code());
						return;
					}
					Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
					Map<String, Object> webhook = gson.fromJson(responseBody.string(), mapType);
					onSuccess.accept(webhook);
				}
			}
		});
	}

	public void saveWebhook(String url, List<String> enabledTypes, Consumer<Map<String, Object>> onSuccess, Consumer<String> onError)
	{
		JsonObject body = new JsonObject();
		body.addProperty("webhookUrl", url);
		body.add("enabledTypes", gson.toJsonTree(enabledTypes));

		Request request = authedRequest("/webhook")
			.put(RequestBody.create(JSON, gson.toJson(body)))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to save webhook: {}", e.getMessage());
				onError.accept(e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						String errorMsg = "HTTP " + response.code();
						if (responseBody != null)
						{
							try
							{
								JsonObject err = gson.fromJson(responseBody.string(), JsonObject.class);
								if (err.has("error"))
								{
									errorMsg = err.get("error").getAsString();
								}
							}
							catch (Exception ignored) {}
						}
						onError.accept(errorMsg);
						return;
					}
					Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
					Map<String, Object> webhook = gson.fromJson(responseBody.string(), mapType);
					onSuccess.accept(webhook);
				}
			}
		});
	}

	public void deleteWebhook(Runnable onSuccess, Consumer<String> onError)
	{
		Request request = authedRequest("/webhook")
			.delete()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to delete webhook: {}", e.getMessage());
				onError.accept(e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
				if (response.isSuccessful())
				{
					onSuccess.run();
				}
				else
				{
					onError.accept("HTTP " + response.code());
				}
			}
		});
	}

	public void fetchClanSummary(Consumer<Map<String, Object>> onSuccess)
	{
		Request request = authedRequest("/clan/summary")
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Failed to fetch clan summary: {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody responseBody = response.body())
				{
					if (!response.isSuccessful() || responseBody == null)
					{
						return;
					}
					Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
					Map<String, Object> summary = gson.fromJson(responseBody.string(), mapType);
					onSuccess.accept(summary);
				}
			}
		});
	}
}
