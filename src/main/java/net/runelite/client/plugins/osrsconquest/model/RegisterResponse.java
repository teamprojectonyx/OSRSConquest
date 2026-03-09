package net.runelite.client.plugins.osrsconquest.model;

import lombok.Data;

@Data
public class RegisterResponse
{
	private String apiKey;
	private int memberId;
	private int clanId;
	private String clanName;
}
