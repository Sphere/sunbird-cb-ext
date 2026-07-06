package org.sunbird.learnerPath.repository.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.sunbird.learnerPath.repository.model.LearnerPath;
import org.sunbird.learnerPath.repository.service.LearnerPathService;
import org.sunbird.learnerPath.repository.service.UserPassbookService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LearnerPathController {

    @Autowired
    private LearnerPathService learnerpathservice;
    @Autowired
    private UserPassbookService userPassbookService;

    @PostMapping(value = "/learnerpath")
    public ResponseEntity<LearnerPath> createOrUpdateLearnerPath(@RequestBody LearnerPath learnerPath) {
        return ResponseEntity.ok(learnerpathservice.insertOrUpdate(learnerPath));
    }


    @GetMapping("/learnerpath")
    public ResponseEntity<List<LearnerPath>> getLearnerPath(@RequestParam(value = "userId", required = true) String userId,
                                                            @RequestParam(value = "courseId", required = false) String courseId) {

        List<LearnerPath> learnerPaths;
        List<LearnerPath> userPassbooks;
        if (courseId != null) {

            userPassbooks= userPassbookService.getUserPassbookByUseridAndCourseId(userId, courseId);
            learnerPaths = learnerpathservice.findByUserIdAndCourseId(userId, courseId);

        } else {
            userPassbooks = userPassbookService.getUserPassbookByUserid(userId);
            learnerPaths = learnerpathservice.findByUserId(userId);
        }
//		 Set<LearnerPath> learnerPathSet = new HashSet<>(learnerPaths); // Convert to set to eliminate duplicates
//		    learnerPathSet.removeAll(userPassbooks); // Remove userPassbooks from learnerPaths set
        Set<LearnerPath> mergedSet = new HashSet<>(learnerPaths);
        mergedSet.addAll(userPassbooks);

        List<LearnerPath> mergedList = new ArrayList<>(mergedSet);

        if (mergedList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mergedList);
    }
}
