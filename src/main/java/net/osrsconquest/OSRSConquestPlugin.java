package net.osrsconquest;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.osrsconquest.model.ClanEvent;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "OSRS Conquest",
	description = "Tracks clan activity, XP gains, and playtime to a shared backend",
	tags = {"clan", "tracker", "stats", "conquest"}
)
public class OSRSConquestPlugin extends Plugin
{
	private static final int PANEL_REFRESH_TICKS = 100;  // ~60 seconds
	private static final int STAT_FLUSH_TICKS = 500;    // ~5 minutes
	private static final int MEMBER_SYNC_TICKS = 500;   // ~5 minutes (keeps online status fresh)
	private static final int INITIAL_SYNC_TICKS = 10;   // ~6 seconds after login

	@Inject
	private Client client;

	@Inject
	private OSRSConquestConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ConquestApiClient apiClient;

	@Inject
	private ClanEventParser eventParser;

	@Inject
	private SessionTracker sessionTracker;

	@Inject
	private StatTracker statTracker;

	@Inject
	private ClanStateManager clanStateManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ApplicationNotificationOverlay applicationOverlay;

	private OSRSConquestPanel panel;
	private NavigationButton navButton;

	private final List<ClanEvent> eventQueue = Collections.synchronizedList(new ArrayList<>());
	private int tickCounter = 0;
	private volatile boolean registrationPending = false;
	private boolean initialSyncDone = false;

	@Override
	protected void startUp()
	{
		panel = injector.getInstance(OSRSConquestPanel.class);
		panel.setSyncMembersCallback(() -> clanStateManager.syncMembers(client));

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/conquest_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("OSRS Conquest")
			.icon(icon != null ? icon : new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB))
			.priority(10)
			.panel(panel)
			.build();

		if (config.showPanel())
		{
			clientToolbar.addNavigation(navButton);
		}

		overlayManager.add(applicationOverlay);

		log.info("OSRS Conquest started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(applicationOverlay);
		clientToolbar.removeNavigation(navButton);
		flushEventQueue();
		log.info("OSRS Conquest stopped");
	}

	@Provides
	OSRSConquestConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OSRSConquestConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			tickCounter = 0;
			initialSyncDone = false;

			if (config.dataConsent())
			{
				int world = client.getWorld();
				sessionTracker.onLogin(world);
				statTracker.captureAndSubmit(client);
			}

			panel.refreshCurrentTab();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			if (config.dataConsent())
			{
				sessionTracker.onLogout();
				flushEventQueue();
			}
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (!config.dataConsent())
		{
			return;
		}

		String detectedClan = clanStateManager.detectClanName(client);

		if (detectedClan != null && !detectedClan.isEmpty())
		{
			// Auto-register if no API key configured
			if (config.apiKey().isEmpty() && !registrationPending)
			{
				registrationPending = true;
				String rsn = client.getLocalPlayer() != null
					? client.getLocalPlayer().getName()
					: null;

				if (rsn != null)
				{
					apiClient.register(rsn, detectedClan,
						(response) -> {
							// Save the API key to config
							configManager.setConfiguration("osrsconquest", "apiKey", response.getApiKey());
							registrationPending = false;
							log.info("Auto-registered with OSRS Conquest as {} in clan {}", rsn, detectedClan);

							// Now that we have a key, refresh panel
							if (config.autoSyncMembers())
							{
								clanStateManager.syncMembers(client);
							}
							panel.refreshCurrentTab();
						},
						(error) -> {
							registrationPending = false;
							log.warn("Auto-registration failed: {}", error);
						}
					);
				}
			}
			else
			{
				// Already registered, sync if opted in and refresh panel
				if (config.autoSyncMembers())
				{
					clanStateManager.syncMembers(client);
				}
				panel.refreshCurrentTab();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.CLAN_MESSAGE
			&& event.getType() != ChatMessageType.CLAN_GIM_MESSAGE)
		{
			return;
		}

		if (!config.dataConsent() || !config.trackEvents() || config.apiKey().isEmpty())
		{
			return;
		}

		ClanEvent clanEvent = eventParser.parse(event.getMessage());
		if (clanEvent != null)
		{
			eventQueue.add(clanEvent);

			// Show viewport notification for applications
			if ("APPLICATION".equals(clanEvent.getType()))
			{
				applicationOverlay.showNotification(clanEvent.getActor());
			}

			log.debug("Queued clan event: {} by {}", clanEvent.getType(), clanEvent.getActor());

			// Flush immediately for important events
			if ("JOIN".equals(clanEvent.getType())
				|| "LEAVE".equals(clanEvent.getType())
				|| "KICK".equals(clanEvent.getType())
				|| "APPLICATION".equals(clanEvent.getType()))
			{
				flushEventQueue();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tickCounter++;

		if (!config.dataConsent())
		{
			return;
		}

		// Flush events every 10 ticks (~6 seconds) if any are queued
		if (tickCounter % 10 == 0 && !eventQueue.isEmpty())
		{
			flushEventQueue();
		}

		// Initial sync ~6 seconds after login (gives clan channel time to populate)
		if (!initialSyncDone && tickCounter == INITIAL_SYNC_TICKS)
		{
			initialSyncDone = true;
			if (config.autoSyncMembers())
			{
				clanStateManager.syncMembers(client);
			}
			panel.refreshCurrentTab();
		}

		// Refresh sidebar panel every ~60 seconds
		if (tickCounter % PANEL_REFRESH_TICKS == 0)
		{
			panel.refreshCurrentTab();
		}

		// Submit stats every 5 minutes
		if (tickCounter % STAT_FLUSH_TICKS == 0)
		{
			statTracker.captureAndSubmit(client);
		}

		// Sync members every 5 minutes if user opted in
		if (config.autoSyncMembers() && tickCounter % MEMBER_SYNC_TICKS == 0)
		{
			clanStateManager.syncMembers(client);
		}
	}

	private void flushEventQueue()
	{
		if (eventQueue.isEmpty())
		{
			return;
		}

		List<ClanEvent> toSubmit;
		synchronized (eventQueue)
		{
			toSubmit = new ArrayList<>(eventQueue);
			eventQueue.clear();
		}

		apiClient.submitEvents(toSubmit);
	}
}
