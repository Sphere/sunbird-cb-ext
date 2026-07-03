package org.sunbird.learnerPath.model;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;
import java.util.Map;

@Table(value="user_passbook_v2") // table name in Cassandra
public class UserPassbook {

    @PrimaryKey("userid")
    @Column("userid")
    private String userid;

    @Column("typename")
    private String typename;

    @Column("acquiredchannel")
    private String acquiredChannel;

    @Column("typeid")
    private String typeId;

    @Column("contextid")
    private String contextId;

    @Column("effectivedate")
    private Instant effectiveDate;

    @Column("acquireddetails")
    private Map<String, String> acquiredDetails;

    @Column("additionalparams")
    private Map<String, String> additionalParams;

    // Top-level fields for final response
    private String resourceId;
    private String courseId;
    private String competencyName;

    // Getters and setters for all fields

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getTypename() {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    public String getAcquiredChannel() {
        return acquiredChannel;
    }

    public void setAcquiredChannel(String acquiredChannel) {
        this.acquiredChannel = acquiredChannel;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public Instant getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Instant effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Map<String, String> getAcquiredDetails() {
        return acquiredDetails;
    }

    public void setAcquiredDetails(Map<String, String> acquiredDetails) {
        this.acquiredDetails = acquiredDetails;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    // Getters and setters for new top-level fields
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCompetencyName() {
        return competencyName;
    }

    public void setCompetencyName(String competencyName) {
        this.competencyName = competencyName;
    }
}

