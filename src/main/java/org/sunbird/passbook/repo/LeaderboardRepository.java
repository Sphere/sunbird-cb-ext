package org.sunbird.passbook.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sunbird.passbook.entity.LeaderboardEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<LeaderboardEntity, String>, LeaderboardRepositoryCustom {

	List<LeaderboardEntity> findByRootOrgId(String rootOrgId);

	List<LeaderboardEntity> findByProfession(String profession);

	List<LeaderboardEntity> findByState(String state);

	Optional<LeaderboardEntity> findOneByUserId(String userId);


	@Query(value = "SELECT * FROM public.leaderboard_table " +
			"WHERE (:rootOrgId IS NULL OR rootorgid = :rootOrgId) " +
			"AND (:profession IS NULL OR profession = :profession) " +
			"AND (:state IS NULL OR state = :state) " +
			"AND (:district IS NULL OR district = :district) " +
			"AND (:minPoints IS NULL OR points >= :minPoints) " +
			"ORDER BY points DESC " +
			"LIMIT COALESCE(:limit, 100)",
			nativeQuery = true)
	List<LeaderboardEntity> findByFilters(
			@Param("rootOrgId") String rootOrgId,
			@Param("profession") String profession,
			@Param("state") String state,
			@Param("district") String district,
			@Param("minPoints") Long minPoints,
			@Param("limit") Integer limit
	);

	/**
	 * Native query to get top N users by points with optional filters
	 *
	 * @param rootOrgId root organization ID (optional)
	 * @param limit number of top users to retrieve
	 * @return list of top leaderboard entities
	 */
	@Query(value = "SELECT * FROM public.leaderboard_table " +
			"WHERE (:rootOrgId IS NULL OR rootorgid = :rootOrgId) " +
			"ORDER BY points DESC " +
			"LIMIT :limit",
			nativeQuery = true)
	List<LeaderboardEntity> findTopByPoints(
			@Param("rootOrgId") String rootOrgId,
			@Param("limit") Integer limit
	);
}