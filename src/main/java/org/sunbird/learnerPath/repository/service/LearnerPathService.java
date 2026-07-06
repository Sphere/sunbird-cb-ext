package org.sunbird.learnerPath.repository.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.learnerPath.repository.LearnerPathRepository;
import org.sunbird.learnerPath.repository.model.LearnerPath;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearnerPathService {

    private final LearnerPathRepository repository;

    @Autowired
    public LearnerPathService(LearnerPathRepository repository) {
        this.repository = repository;
    }

    public LearnerPath insertOrUpdate(LearnerPath learnerPath) {
        // If it's a new enrollment, set the enrollment datetime
        if (learnerPath.getDatetime() == null) {
            learnerPath.setDatetime(LocalDateTime.now());
        }

        // Update last access time every time the user accesses the course
        learnerPath.setLastAccessTime(LocalDateTime.now());

        // Increment attempt count for each update
        learnerPath.incrementAttemptCount();

        // Save the learnerPath object to the repository
        return repository.save(learnerPath);
    }

    //    public Optional<LearnerPath> findById(String userId) {
//        return repository.findById(userId);
//    }
    public List<LearnerPath> findByUserId(String userId) {
        return repository.findByUserid(userId);
    }

    public List<LearnerPath> findByUserIdAndCourseId(String userId, String courseId) {
        return repository.findByUseridAndCourseid(userId, courseId);
    }

    public void deleteById(String userId) {
        repository.deleteById(userId);
    }
}

