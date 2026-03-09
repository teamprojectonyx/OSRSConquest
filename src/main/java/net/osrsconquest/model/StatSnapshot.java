package net.osrsconquest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatSnapshot
{
	private String skill;
	private long xp;
	private int level;
}
