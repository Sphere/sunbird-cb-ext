package org.sunbird.passbook.repo;

import org.sunbird.passbook.entity.LeaderboardEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LeaderboardRepositoryCustom {

	List<LeaderboardEntity> findAllUsersByDynamicFilters(
			Map<String, Object> filters,
			Integer limit,
			Integer offset
	);

	Optional<LeaderboardEntity> findUserByDynamicFilter(String userId, Map<String, Object> filters);

	Integer findUserRank(LeaderboardEntity leaderboardEntity, Map<String, Object> filters);
}