package net.runelite.client.plugins.osrsconquest;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("osrsconquest")
public interface OSRSConquestConfig extends Config
{
	@ConfigItem(
		keyName = "apiUrl",
		name = "API URL",
		description = "The OSRS Conquest backend API URL",
		position = 1
	)
	default String apiUrl()
	{
		return "https://conquest.projectonyx.net/api";
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "Your API key (auto-generated on first login)",
		secret = true,
		position = 2
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "dataConsent",
		name = "Data Sharing Consent",
		description = "I acknowledge that this plugin sends clan member names, ranks, activity, and XP data to the OSRS Conquest backend. All clan members should be aware of this plugin's use.",
		position = 3
	)
	default boolean dataConsent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackEvents",
		name = "Track Clan Events",
		description = "Send clan broadcasts (joins, drops, level-ups) to the backend",
		position = 4
	)
	default boolean trackEvents()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackStats",
		name = "Track XP/Stats",
		description = "Send periodic stat snapshots to the backend",
		position = 5
	)
	default boolean trackStats()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackSessions",
		name = "Track Sessions",
		description = "Report login/logout to the backend",
		position = 6
	)
	default boolean trackSessions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoSyncMembers",
		name = "Auto-Sync Clan Members",
		description = "Automatically sync clan member list to the dashboard periodically. When off, use the Sync button in the panel.",
		position = 7
	)
	default boolean autoSyncMembers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showPanel",
		name = "Show Sidebar Panel",
		description = "Show the OSRS Conquest sidebar panel",
		position = 8
	)
	default boolean showPanel()
	{
		return true;
	}
}
