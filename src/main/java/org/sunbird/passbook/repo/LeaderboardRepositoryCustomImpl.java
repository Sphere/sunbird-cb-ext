package org.sunbird.passbook.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.sunbird.passbook.entity.LeaderboardEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

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
	public List<LeaderboardEntity> findByDynamicFilters(
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
		logger.info("Generated SQL query: {}", finalQuery);

		Query query = entityManager.createNativeQuery(finalQuery, LeaderboardEntity.class);
//
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

    /**
     * @param leaderboardEntity
     * @return
     */
	@Override
	public Integer findUserRank(LeaderboardEntity leaderboardEntity) {

		// Step 2: Count users ranked higher (more points OR same points with earlier date)
		StringBuilder rankQueryBuilder = new StringBuilder(
				"SELECT COUNT(*) FROM public.leaderboard_table WHERE " +
				"((points > :userPoints) OR (points = :userPoints AND date < :userDate))");


		Query rankQuery = entityManager.createNativeQuery(rankQueryBuilder.toString());
		rankQuery.setParameter("userPoints", leaderboardEntity.getPoints());
		rankQuery.setParameter("userDate", leaderboardEntity.getDate());

		Object rankResult = rankQuery.getSingleResult();
		Integer rank = ((Number) rankResult).intValue();
		logger.info("User {} rank: {}", leaderboardEntity.getUserId(), rank);
		return rank;
	}
}
