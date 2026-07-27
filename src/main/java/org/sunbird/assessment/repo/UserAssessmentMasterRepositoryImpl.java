package org.sunbird.assessment.repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;

public class UserAssessmentMasterRepositoryImpl implements UserAssessmentMasterRepositoryCustom {

	@Autowired
	CassandraOperations cassandraOperations;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sunbird.assessment.repo.
	 * UserAssessmentMasterRepositoryCustom#updateAssessment(org.sunbird.assessment.
	 * repo.UserAssessmentMasterModel,
	 * org.sunbird.assessment.repo.UserAssessmentSummaryModel)
	 */
	@Override
	public UserAssessmentMasterModel updateAssessment(UserAssessmentMasterModel assessment,
			UserAssessmentSummaryModel assessmentSummary) {
		// Writes are issued as individual INSERTs rather than a CQL batch: the two
		// tables live in different partitions, so a cross-partition batch gave no
		// atomicity anyway, and YugabyteDB (YCQL) rejects the text form that Spring
		// Data 2.0.x's batchOps() emits (BEGIN BATCH ... APPLY BATCH). A plain insert
		// renders "INSERT INTO ...", which works on both YCQL and stock Cassandra.
		cassandraOperations.insert(assessment);
		if (assessmentSummary.getPrimaryKey() != null) {
			cassandraOperations.insert(assessmentSummary);
		}
		return assessment;
	}
}
