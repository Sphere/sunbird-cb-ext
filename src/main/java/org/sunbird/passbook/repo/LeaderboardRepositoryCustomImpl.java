package org.sunbird.passbook.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.sunbird.passbook.entity.LeaderboardEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LeaderboardRepositoryCustomImpl implements LeaderboardRepositoryCustom {

	private static final Logger logger = LoggerFactory.getLogger(LeaderboardRepositoryCustomImpl.class);

	@PersistenceContext
	private EntityManager entityManager;

    /**
     * @param filters
     * @param limit
     * @param offset
     * @return
     */
	@Override
	public List<LeaderboardEntity> findAllUsersByDynamicFilters(
			Map<String, Object> filters,
			Integer limit,
			Integer offset
	) {
		StringBuilder queryBuilder = new StringBuilder("SELECT * FROM public.leaderboard_table WHERE 1=1");

//		 Add dynamic filters
		if (filters != null && !filters.isEmpty()) {
			for (String key : filters.keySet()) {
				queryBuilder.append(" AND ").append(key).append(" = :").append(key);
			}
		}

		// Add ordering - default by date ASC
		queryBuilder.append(" ORDER BY points DESC, date ASC");

		String finalQuery = queryBuilder.toString();
		logger.info("Final query for getting leaderboard user list: {}", finalQuery);

		Query query = entityManager.createNativeQuery(finalQuery, LeaderboardEntity.class);

		// Set filter parameters
		if (filters != null && !filters.isEmpty()) {
			for (Map.Entry<String, Object> entry : filters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}

		// Set pagination - offset (skip records)
		if (offset != null && offset >= 0) {
			query.setFirstResult(offset);
			logger.info("Setting offset: {}", offset);
		}

		// Set pagination - limit (max results)
		if (limit != null && limit > 0) {
			query.setMaxResults(limit);
			logger.info("Setting limit: {}", limit);
		}

		logger.info("Executing query with {} filter(s), Filters: {}", filters != null ? filters.size() : 0, filters);

		List<LeaderboardEntity> results = query.getResultList();
		logger.info("Query returned {} results", results.size());
		return results;
	}

    /** This method call return users which can exist in pre pulled user list.
	 * When this method is running query for individual user it matched with existing user list in Hibernate first-level
	 * cache while calling findAllUsersByDynamicFilters method.
	 *
	 * So this method is flushing and clearing the cache using entity manager, method needs to be transactional.
	 *
     * @param userId
     * @param filters
     * @return
     */
	@Transactional
	@Override
	public Optional<LeaderboardEntity> findUserByDynamicFilter(String userId, Map<String, Object> filters) {
		StringBuilder queryBuilder = new StringBuilder("SELECT * FROM public.leaderboard_table WHERE 1=1");

//		 Add dynamic filters
		if (filters != null && !filters.isEmpty()) {
			for (String key : filters.keySet()) {
				queryBuilder.append(" AND ").append(key).append(" = :").append(key);
			}
		}
		queryBuilder.append(" AND userid = :userId");

		String finalQuery = queryBuilder.toString();
		logger.info("Final query for getting individual user: {}", finalQuery);

		entityManager.flush();
		entityManager.clear();
		Query query = entityManager.createNativeQuery(finalQuery, LeaderboardEntity.class);

		// Set filter parameters
		if (filters != null && !filters.isEmpty()) {
			for (Map.Entry<String, Object> entry : filters.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}
		query.setParameter("userId", userId);

		List<LeaderboardEntity> leaderboardEntityList = query.getResultList();

		if (leaderboardEntityList != null && leaderboardEntityList.size() == 1) {
			return Optional.of(leaderboardEntityList.get(0));
		}

		return Optional.empty();
	}

    /** Count users ranked higher (more points OR same points with earlier date)
	 *
     * @param leaderboardEntity
     * @return
     */
	@Override
	public Integer findUserRank(LeaderboardEntity leaderboardEntity, Map<String, Object> filters) {

		StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(*) FROM public.leaderboard_table WHERE 1=1");

		if (filters != null && !filters.isEmpty()) {
			for (String key : filters.keySet()) {
				queryBuilder.append(" AND ").append(key).append(" = :").append(key);
			}
		}
		queryBuilder.append(" AND ((points > :userPoints) OR (points = :userPoints AND date < :userDate))");

		String finalQuery = queryBuilder.toString();
		logger.info("Final query for getting individual user rank: {}", finalQuery);
		Query rankQuery = entityManager.createNativeQuery(finalQuery);

		if (filters != null && !filters.isEmpty()) {
			for (Map.Entry<String, Object> entry : filters.entrySet()) {
				rankQuery.setParameter(entry.getKey(), entry.getValue());
			}
		}
		rankQuery.setParameter("userPoints", leaderboardEntity.getPoints());
		rankQuery.setParameter("userDate", leaderboardEntity.getDate());

		Object rankResult = rankQuery.getSingleResult();
		Integer rank = ((Number) rankResult).intValue();
		logger.info("User {} rank: {}", leaderboardEntity.getUserId(), rank);
		return rank;
	}
}
