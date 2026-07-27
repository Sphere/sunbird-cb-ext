package org.sunbird.assessment.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;

public class UserQuizMasterRepositoryImpl implements UserQuizMasterRepositoryCustom {

	@Autowired
	CassandraOperations cassandraOperations;
	
	@Override
	public UserQuizMasterModel updateQuiz(UserQuizMasterModel quiz, UserQuizSummaryModel quizSummary) {
		// Individual INSERTs instead of a CQL batch (see UserAssessmentMasterRepositoryImpl):
		// YugabyteDB (YCQL) rejects the BEGIN BATCH ... APPLY BATCH text that Spring Data
		// 2.0.x's batchOps() emits; the two writes are in different partitions so the batch
		// gave no atomicity anyway. Kept unconditional to match the original behaviour.
		cassandraOperations.insert(quiz);
		cassandraOperations.insert(quizSummary);
		return quiz;
	}
}
