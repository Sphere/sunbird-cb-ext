package org.sunbird.learnerPath.repository;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sunbird.learnerPath.repository.model.LearnerPath;


@Repository
public interface LearnerPathRepository extends CassandraRepository<LearnerPath, String> {
    List<LearnerPath> findByUserid(String userid);
    List<LearnerPath> findByUseridAndCourseid(String userId, String courseId);
}
