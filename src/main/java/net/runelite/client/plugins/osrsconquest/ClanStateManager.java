package net.runelite.client.plugins.osrsconquest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.osrsconquest.model.MemberInfo;
import net.runelite.api.Client;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanMember;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class ClanStateManager
{
	private final ConquestApiClient apiClient;
	private final OSRSConquestConfig config;
	private volatile String clanName;

	@Inject
	public ClanStateManager(ConquestApiClient apiClient, OSRSConquestConfig config)
	{
		this.apiClient = apiClient;
		this.config = config;
	}

	public String getClanName()
	{
		return clanName;
	}

	public void setClanName(String name)
	{
		this.clanName = name;
	}

	public void syncMembers(Client client)
	{
		if (config.apiKey().isEmpty())
		{
			return;
		}

		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings == null)
		{
			return;
		}

		List<ClanMember> clanMembers = clanSettings.getMembers();
		if (clanMembers == null || clanMembers.isEmpty())
		{
			return;
		}

		// Build set of normalized online RSNs from the clan channel
		Set<String> onlineRsns = new HashSet<>();
		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null)
		{
			List<ClanChannelMember> channelMembers = clanChannel.getMembers();
			if (channelMembers != null)
			{
				for (ClanChannelMember cm : channelMembers)
				{
					onlineRsns.add(Text.toJagexName(cm.getName()));
				}
			}
		}

		List<MemberInfo> members = new ArrayList<>();
		for (ClanMember member : clanMembers)
		{
			String normalized = Text.toJagexName(member.getName());
			members.add(MemberInfo.builder()
				.rsn(member.getName())
				.rank(member.getRank().toString())
				.online(onlineRsns.contains(normalized))
				.build());
		}

		apiClient.syncMembers(members);
		log.debug("Synced {} clan members ({} online)", members.size(), onlineRsns.size());
	}

	public String detectClanName(Client client)
	{
		ClanSettings clanSettings = client.getClanSettings();
		if (clanSettings != null)
		{
			String name = clanSettings.getName();
			if (name != null && !name.isEmpty())
			{
				this.clanName = name;
				return name;
			}
		}

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel != null)
		{
			String name = clanChannel.getName();
			if (name != null && !name.isEmpty())
			{
				this.clanName = name;
				return name;
			}
		}

		return null;
	}
}
