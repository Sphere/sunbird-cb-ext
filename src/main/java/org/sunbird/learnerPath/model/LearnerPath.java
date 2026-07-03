package org.sunbird.learnerPath.model;

import java.time.LocalDateTime;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = "asha_learnerpath")
public class LearnerPath {
    @PrimaryKey("userid")
    private String userid;                 // Unique user identifier
    private String courseid;               // Identifier for the course
    private String batchid;                // Identifier for the batch
    private String contentid;              // Identifier for the content
    private String competencyid;
    private int competencylevel;            // competency level
    private double completionpercentage;    // Percentage of completion
    private LocalDateTime datetime;        // Date and time of enrollment
    private LocalDateTime last_access_time;  // Last access time of the learner
    private LocalDateTime last_completed_time; // Last completion time of the learner
    private String content_type;                // Progress status (e.g., "In Progress", "Completed")
    private String pass_fail_status = "Fail"; // Default value set to "Fail"
    private int attemptcount = 0;          // Default value set to 0


    // Getters and Setters

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getCourseid() {
        return courseid;
    }

    public void setCourseid(String courseid) {
        this.courseid = courseid;
    }

    public String getBatchid() {
        return batchid;
    }

    public void setBatchid(String batchid) {
        this.batchid = batchid;
    }

    public String getContentid() {
        return contentid;
    }

    public void setContentid(String contentid) {
        this.contentid = contentid;
    }

    public String getCompetencyid() {
        return competencyid;
    }

    public void setCompetencyid(String competencyid) {
        this.competencyid = competencyid;
    }

    public int getcompetencylevel() {
        return competencylevel;
    }

    public void setcompetencylevel(int competencylevel) {
        this.competencylevel = competencylevel;
    }

    public double getCompletionpercentage() {
        return completionpercentage;
    }

    public void setCompletionpercentage(double completionpercentage) {
        this.completionpercentage = completionpercentage;
        // Update passFailStatus if completionpercentage is 100
        if (completionpercentage == 100) {
            this.pass_fail_status = "Pass";
            updateLastCompletedTime();
        }
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    // Only set this once when the user enrolls
    public void setDatetime(LocalDateTime datetime) {
        if (this.datetime == null) {
            this.datetime = datetime;
        }
    }

    public LocalDateTime getLastAccessTime() {
        return last_access_time;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.last_access_time = lastAccessTime;
    }

    public LocalDateTime getLastCompletedTime() {
        return last_completed_time;
    }

    private void updateLastCompletedTime() {
        this.last_completed_time = LocalDateTime.now(); // Update to current time
    }

    public String getContentType() {
        return content_type;
    }

    public void setContentType(String content_type) {
        this.content_type = content_type;
    }


    public String getPassFailStatus() {
        return pass_fail_status;
    }

    public void setPassFailStatus(String passFailStatus) {
        this.pass_fail_status = passFailStatus;
    }

    public int getAttemptcount() {
        return attemptcount;
    }

    // Method to increment attempt count
    public void incrementAttemptCount() {
        this.attemptcount++;
    }

    public void setAttemptcount(int attemptcount) {
        this.attemptcount = attemptcount;
    }
}
