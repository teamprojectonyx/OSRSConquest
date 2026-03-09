package net.runelite.client.plugins.osrsconquest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.osrsconquest.model.StatSnapshot;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class StatTracker
{
	private final ConquestApiClient apiClient;
	private final OSRSConquestConfig config;

	@Inject
	public StatTracker(ConquestApiClient apiClient, OSRSConquestConfig config)
	{
		this.apiClient = apiClient;
		this.config = config;
	}

	public void captureAndSubmit(Client client)
	{
		if (!config.trackStats() || config.apiKey().isEmpty())
		{
			return;
		}

		List<StatSnapshot> stats = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}

			int xp = client.getSkillExperience(skill);
			int level = client.getRealSkillLevel(skill);

			stats.add(StatSnapshot.builder()
				.skill(skill.getName())
				.xp(xp)
				.level(level)
				.build());
		}

		// Add overall / total
		long totalXp = 0;
		int totalLevel = 0;
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}
			totalXp += client.getSkillExperience(skill);
			totalLevel += client.getRealSkillLevel(skill);
		}
		stats.add(StatSnapshot.builder()
			.skill("Overall")
			.xp(totalXp)
			.level(totalLevel)
			.build());

		apiClient.submitStats(stats);
		log.debug("Submitted {} stat snapshots", stats.size());
	}
}
