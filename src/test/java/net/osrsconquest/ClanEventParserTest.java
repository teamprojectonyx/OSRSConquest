package net.osrsconquest;

import net.osrsconquest.model.ClanEvent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClanEventParserTest
{
	private ClanEventParser parser;

	@Before
	public void setUp()
	{
		parser = new ClanEventParser();
	}

	@Test
	public void testJoinParsing()
	{
		ClanEvent e = parser.parse("Player123 has been invited into the clan by Leader1.");
		assertNotNull(e);
		assertEquals("JOIN", e.getType());
		assertEquals("Player123", e.getTarget());
		assertEquals("Leader1", e.getActor());
	}

	@Test
	public void testLeaveParsing()
	{
		ClanEvent e = parser.parse("SomeGuy has left the clan.");
		assertNotNull(e);
		assertEquals("LEAVE", e.getType());
		assertEquals("SomeGuy", e.getActor());
	}

	@Test
	public void testKickParsing()
	{
		ClanEvent e = parser.parse("BadPlayer has been kicked from the clan by Admin1.");
		assertNotNull(e);
		assertEquals("KICK", e.getType());
		assertEquals("BadPlayer", e.getTarget());
		assertEquals("Admin1", e.getActor());
	}

	@Test
	public void testLevelUpParsing()
	{
		ClanEvent e = parser.parse("Zezima has reached Attack level 99.");
		assertNotNull(e);
		assertEquals("LEVEL_UP", e.getType());
		assertEquals("Zezima", e.getActor());
		assertEquals("Attack level 99", e.getDetail());
	}

	@Test
	public void testTotalLevelParsing()
	{
		ClanEvent e = parser.parse("Zezima has reached a total level of 2277.");
		assertNotNull(e);
		assertEquals("LEVEL_UP", e.getType());
		assertEquals("Zezima", e.getActor());
		assertEquals("Total level 2277", e.getDetail());
	}

	@Test
	public void testQuestParsing()
	{
		ClanEvent e = parser.parse("Player1 has completed a quest: Dragon Slayer II");
		assertNotNull(e);
		assertEquals("QUEST", e.getType());
		assertEquals("Player1", e.getActor());
		assertEquals("Dragon Slayer II", e.getDetail());
	}

	@Test
	public void testDropParsing()
	{
		ClanEvent e = parser.parse("Ironman received a drop: Twisted bow (1,200,000,000 coins)");
		assertNotNull(e);
		assertEquals("DROP", e.getType());
		assertEquals("Ironman", e.getActor());
		assertTrue(e.getDetail().contains("Twisted bow"));
	}

	@Test
	public void testDropWithoutValue()
	{
		ClanEvent e = parser.parse("Player1 received a drop: Dragon bones");
		assertNotNull(e);
		assertEquals("DROP", e.getType());
		assertEquals("Dragon bones", e.getDetail());
	}

	@Test
	public void testCollectionLogParsing()
	{
		ClanEvent e = parser.parse("Player1 received a new collection log item: Abyssal whip (42/1443)");
		assertNotNull(e);
		assertEquals("COLLECTION_LOG", e.getType());
		assertEquals("Player1", e.getActor());
		assertTrue(e.getDetail().contains("Abyssal whip"));
	}

	@Test
	public void testPetParsing()
	{
		ClanEvent e = parser.parse("LuckyGuy has a funny feeling like he's being followed.");
		assertNotNull(e);
		assertEquals("PET", e.getType());
		assertEquals("LuckyGuy", e.getActor());
	}

	@Test
	public void testRankChangeParsing()
	{
		ClanEvent e = parser.parse("Player1 has been promoted to Captain.");
		assertNotNull(e);
		assertEquals("RANK_CHANGE", e.getType());
		assertEquals("Player1", e.getActor());
		assertTrue(e.getDetail().contains("promoted"));
	}

	@Test
	public void testUnrecognizedReturnsNull()
	{
		ClanEvent e = parser.parse("Some random clan message that doesn't match anything.");
		assertNull(e);
	}

	@Test
	public void testHtmlStripping()
	{
		ClanEvent e = parser.parse("<col=ff0000>Player1</col> has left the clan.");
		assertNotNull(e);
		assertEquals("LEAVE", e.getType());
		assertEquals("Player1", e.getActor());
	}
}
