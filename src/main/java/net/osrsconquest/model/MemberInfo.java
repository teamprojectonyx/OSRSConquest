package net.osrsconquest.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberInfo
{
	private String rsn;
	private String rank;
	@Builder.Default
	private boolean online = false;
}
