package net.runelite.client.plugins.osrsconquest;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.osrsconquest.model.ClanEvent;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class ClanEventParser
{
	// Member joined the clan
	private static final Pattern JOIN_PATTERN = Pattern.compile(
		"(.+) has been invited into the clan by (.+)\\."
	);

	// Member left the clan
	private static final Pattern LEAVE_PATTERN = Pattern.compile(
		"(.+) has left the clan\\."
	);

	// Member was kicked/expelled
	private static final Pattern KICK_PATTERN = Pattern.compile(
		"(.+) has been kicked from the clan by (.+)\\."
	);

	// Clan application: "<name> has applied to join the clan." or "<name> wishes to join the clan."
	private static final Pattern APPLICATION_PATTERN = Pattern.compile(
		"(.+) (?:has applied to join|wishes to join) (?:the|your) clan\\."
	);

	// Level up: "<name> has reached <skill> level <level>."
	// Also handles total level: "<name> has reached a total level of <level>."
	private static final Pattern LEVEL_UP_PATTERN = Pattern.compile(
		"(.+) has reached (.+) level (\\d+)\\."
	);
	private static final Pattern TOTAL_LEVEL_PATTERN = Pattern.compile(
		"(.+) has reached a total level of (\\d+)\\."
	);

	// Quest completion: "<name> has completed a quest: <quest name>"
	private static final Pattern QUEST_PATTERN = Pattern.compile(
		"(.+) has completed a quest: (.+)"
	);

	// Valuable drop: "<name> received a drop: <item> (<value>)"
	// or "<name> received a drop: <item>"
	private static final Pattern DROP_PATTERN = Pattern.compile(
		"(.+) received a drop: (.+?)(?:\\s*\\(([\\d,]+)\\s*coins\\))?$"
	);

	// Collection log: "<name> received a new collection log item: <item> (<count>)"
	private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
		"(.+) received a new collection log item: (.+?)(?:\\s*\\((\\d+)/(\\d+)\\))?$"
	);

	// Pet drop: "<name> has a funny feeling like .*"
	// or "<name> has a funny feeling like they would have been followed..."
	private static final Pattern PET_PATTERN = Pattern.compile(
		"(.+) (?:has a funny feeling like .+|feels something weird sneaking into .+)"
	);

	// Personal best: "<name> has achieved a new personal best: <boss/raid>"
	private static final Pattern PB_PATTERN = Pattern.compile(
		"(.+) has achieved a new (?:.*) personal best: (.+)"
	);

	// Rank change: "<name> has been promoted to <rank>." or "<name> has been demoted to <rank>."
	private static final Pattern RANK_CHANGE_PATTERN = Pattern.compile(
		"(.+) has been (promoted|demoted) (?:from .+ )?to (.+)\\."
	);

	// Diary completion
	private static final Pattern DIARY_PATTERN = Pattern.compile(
		"(.+) has completed the (.+)\\."
	);

	// Combat achievement
	private static final Pattern COMBAT_ACHIEVEMENT_PATTERN = Pattern.compile(
		"(.+) has completed an? (.+) combat task: (.+)\\."
	);

	public ClanEvent parse(String message)
	{
		// Strip any color tags and html from the message
		String clean = message
			.replaceAll("<[^>]+>", "")
			.replaceAll("\\u00a0", " ")
			.trim();

		Matcher m;

		// Join (invite)
		m = JOIN_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("JOIN")
				.target(m.group(1).trim())
				.actor(m.group(2).trim())
				.detail("Invited by " + m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Leave
		m = LEAVE_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("LEAVE")
				.actor(m.group(1).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Kick
		m = KICK_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("KICK")
				.target(m.group(1).trim())
				.actor(m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Application
		m = APPLICATION_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("APPLICATION")
				.actor(m.group(1).trim())
				.detail("Applied to join the clan")
				.timestamp(Instant.now().toString())
				.build();
		}

		// Total level
		m = TOTAL_LEVEL_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("LEVEL_UP")
				.actor(m.group(1).trim())
				.detail("Total level " + m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Level up
		m = LEVEL_UP_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("LEVEL_UP")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim() + " level " + m.group(3).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Quest
		m = QUEST_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("QUEST")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Collection log
		m = COLLECTION_LOG_PATTERN.matcher(clean);
		if (m.matches())
		{
			String detail = m.group(2).trim();
			if (m.group(3) != null)
			{
				detail += " (" + m.group(3) + "/" + m.group(4) + ")";
			}
			return ClanEvent.builder()
				.type("COLLECTION_LOG")
				.actor(m.group(1).trim())
				.detail(detail)
				.timestamp(Instant.now().toString())
				.build();
		}

		// Drop
		m = DROP_PATTERN.matcher(clean);
		if (m.matches())
		{
			String detail = m.group(2).trim();
			if (m.group(3) != null)
			{
				detail += " (" + m.group(3) + " coins)";
			}
			return ClanEvent.builder()
				.type("DROP")
				.actor(m.group(1).trim())
				.detail(detail)
				.timestamp(Instant.now().toString())
				.build();
		}

		// Pet
		m = PET_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("PET")
				.actor(m.group(1).trim())
				.detail("Pet drop!")
				.timestamp(Instant.now().toString())
				.build();
		}

		// Personal best
		m = PB_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("PERSONAL_BEST")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Rank change
		m = RANK_CHANGE_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("RANK_CHANGE")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim() + " to " + m.group(3).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Combat achievement
		m = COMBAT_ACHIEVEMENT_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("COMBAT_ACHIEVEMENT")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim() + ": " + m.group(3).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Diary
		m = DIARY_PATTERN.matcher(clean);
		if (m.matches())
		{
			return ClanEvent.builder()
				.type("DIARY")
				.actor(m.group(1).trim())
				.detail(m.group(2).trim())
				.timestamp(Instant.now().toString())
				.build();
		}

		// Unrecognized — log it for future pattern updates
		log.debug("Unrecognized clan message: {}", clean);
		return null;
	}
}
