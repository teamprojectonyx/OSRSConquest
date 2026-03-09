package net.osrsconquest;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OSRSConquestPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OSRSConquestPlugin.class);
		RuneLite.main(args);
	}
}
