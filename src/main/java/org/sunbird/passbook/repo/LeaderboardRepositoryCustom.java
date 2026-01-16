package org.sunbird.passbook.repo;

import org.sunbird.passbook.entity.LeaderboardEntity;

import java.util.List;
import java.util.Map;

public interface LeaderboardRepositoryCustom {

	List<LeaderboardEntity> findByDynamicFilters(
			Map<String, Object> filters,
			Integer limit,
			Integer offset
	);

	Integer findUserRank(LeaderboardEntity leaderboardEntity);
}