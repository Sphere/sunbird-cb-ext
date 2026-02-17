package org.sunbird.passbook.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRequestDTO {

	private Map<String, Object> filters;

	private String userId;

	private Integer limit;

	private Integer offset;
}