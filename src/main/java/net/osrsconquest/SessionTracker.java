package net.osrsconquest;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class SessionTracker
{
	private final ConquestApiClient apiClient;
	private final OSRSConquestConfig config;
	private volatile boolean sessionActive = false;

	@Inject
	public SessionTracker(ConquestApiClient apiClient, OSRSConquestConfig config)
	{
		this.apiClient = apiClient;
		this.config = config;
	}

	public void onLogin(int world)
	{
		if (!config.trackSessions() || config.apiKey().isEmpty())
		{
			return;
		}

		sessionActive = true;
		apiClient.submitLogin(world);
		log.debug("Session started on world {}", world);
	}

	public void onLogout()
	{
		if (!config.trackSessions() || config.apiKey().isEmpty() || !sessionActive)
		{
			return;
		}

		sessionActive = false;
		apiClient.submitLogout();
		log.debug("Session ended");
	}

	public boolean isSessionActive()
	{
		return sessionActive;
	}
}
