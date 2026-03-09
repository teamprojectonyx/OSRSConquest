package net.runelite.client.plugins.osrsconquest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClanEvent
{
	private String type;
	private String actor;
	private String target;
	private String detail;
	private String source;
	private String timestamp;
}
