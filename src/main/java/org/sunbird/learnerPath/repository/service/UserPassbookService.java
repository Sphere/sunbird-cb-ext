package org.sunbird.learnerPath.repository.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.learnerPath.repository.UserPassbookRepository;
import org.sunbird.learnerPath.repository.model.LearnerPath;
import org.sunbird.learnerPath.repository.model.UserPassbook;

@Service
public class UserPassbookService {

    @Autowired
    private UserPassbookRepository userPassbookRepository;
    @Autowired
    public UserPassbookService(UserPassbookRepository repository) {
        this.userPassbookRepository = repository;
    }


    public List<LearnerPath> getUserPassbookByUserid(String userid) {
        // Fetch all UserPassbook records for the given userId
        List<UserPassbook> userPassbooks = userPassbookRepository.findByUserid(userid);

        // Process each UserPassbook in the list
        return userPassbooks.stream().map(passbook -> {
            // Get the acquiredDetails map
            Map<String, String> acquiredDetails = passbook.getAcquiredDetails();
            LearnerPath learnerPath = new LearnerPath();
            learnerPath.setUserid(passbook.getUserid());
            learnerPath.setContentType(passbook.getAcquiredChannel());
            learnerPath.setCompetencyid(passbook.getTypeId());
            int contextId = Integer.parseInt(passbook.getContextId());
            learnerPath.setcompetencylevel(contextId);
            learnerPath.setCompletionpercentage(100);
            learnerPath.setPassFailStatus("Pass");

            if (acquiredDetails != null) {
                // Extract the ResourceId, courseId, and competencyName
                String resourceId = acquiredDetails.getOrDefault("ResourseId","");
                String courseId = acquiredDetails.get("courseId");
                String competencyName = acquiredDetails.get("competencyName");

                // Set these values as top-level fields in the UserPassbook object
//                passbook.setResourceId(resourceId);
//                passbook.setCourseId(courseId);
//                passbook.setCompetencyName(competencyName);
                learnerPath.setCourseid(courseId);
                learnerPath.setContentid(resourceId);

                // Optionally, remove acquiredDetails if you don't want to include it in the final response
                passbook.setAcquiredDetails(null); // Nullify or reset the map
            }

            return learnerPath;
        }).collect(Collectors.toList()); // Collect the modified list of UserPassbook objects
    }

    public List<LearnerPath> getUserPassbookByUseridAndCourseId(String userid, String courseId) {
        // Fetch all UserPassbook records for the given userId
        List<UserPassbook> userPassbooks = userPassbookRepository.findByUserid(userid);

        // Process each UserPassbook in the list
        return userPassbooks.stream()
                // Apply filter for the desired courseId
                .filter(passbook -> {
                    // Check if acquiredDetails is not null and contains the courseId key
                    Map<String, String> acquiredDetails = passbook.getAcquiredDetails();
                    return acquiredDetails != null && acquiredDetails.containsKey("courseId") &&
                            acquiredDetails.get("courseId").equals(courseId); // courseId is the value you are filtering by
                })
                .map(passbook -> {
                    // Get the acquiredDetails map
                    Map<String, String> acquiredDetails = passbook.getAcquiredDetails();
                    LearnerPath learnerPath = new LearnerPath();
                    learnerPath.setUserid(passbook.getUserid());
                    learnerPath.setContentType(passbook.getAcquiredChannel());
                    learnerPath.setCompetencyid(passbook.getTypeId());
                    int contextId = Integer.parseInt(passbook.getContextId());
                    learnerPath.setcompetencylevel(contextId);
                    learnerPath.setCompletionpercentage(100);
                    learnerPath.setPassFailStatus("Pass");
                    // learnerPath.setDatetime(passbook.getEffectiveDate());

                    if (acquiredDetails != null) {
                        // Extract the ResourceId, courseId, and competencyName
                        String resourceId = acquiredDetails.getOrDefault("ResourseId", "");
                        String courseIdFromDetails = acquiredDetails.get("courseId");
                        String competencyName = acquiredDetails.get("competencyName");

                        learnerPath.setCourseid(courseIdFromDetails);
                        learnerPath.setContentid(resourceId);

                        // Set these values as top-level fields in the UserPassbook object
//                        passbook.setResourceId(resourceId);
//                        passbook.setCourseId(courseIdFromDetails); // you may need to use the actual `courseIdFromDetails`
//                        passbook.setCompetencyName(competencyName);

                        // Optionally, remove acquiredDetails if you don't want to include it in the final response
                        passbook.setAcquiredDetails(null); // Nullify or reset the map
                    }

                    return learnerPath;
                })
                .collect(Collectors.toList());

    }
}

