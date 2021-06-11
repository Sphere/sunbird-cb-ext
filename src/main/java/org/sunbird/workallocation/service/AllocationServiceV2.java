package org.sunbird.workallocation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.common.model.Response;
import org.sunbird.common.util.Constants;
import org.sunbird.core.exception.BadRequestException;
import org.sunbird.workallocation.model.*;
import org.sunbird.workallocation.util.Validator;
import org.sunbird.workallocation.util.WorkAllocationConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Service
public class AllocationServiceV2 {

    @Autowired
    private IndexerService indexerService;

    @Autowired
    private Validator validator;

    @Autowired
    private AllocationService allocationService;

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${workorder.index.name}")
    public String workOrderIndex;

    @Value("${workorder.index.type}")
    public String workOrderIndexType;

    @Value("${workallocationv2.index.name}")
    public String workAllocationIndex;

    @Value("${workallocation.index.type}")
    public String workAllocationIndexType;

    ObjectMapper mapper = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(AllocationServiceV2.class);

    /**
     *
     * @param userId user Id of the user
     * @param workOrder work order object
     * @return response message as success of failed
     */
    public Response addWorkOrder(String userId, WorkOrderDTO workOrder) {
        validator.validateWorkOrder(workOrder, WorkAllocationConstants.ADD);
        enrichmentService.enrichWorkOrder(workOrder, userId, WorkAllocationConstants.ADD);
        RestStatus restStatus = indexerService.addEntity(workOrderIndex, workOrderIndexType, workOrder.getId(),
                mapper.convertValue(workOrder, Map.class));
        Response response = new Response();
        if (!ObjectUtils.isEmpty(restStatus)) {
            response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        } else {
            response.put(Constants.MESSAGE, Constants.FAILED);
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("id", workOrder.getId());
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     *
     * @param userId user Id of the user
     * @param workOrder work order object
     * @return response message as success of failed
     */
    public Response updateWorkOrder(String userId, WorkOrderDTO workOrder) {
        validator.validateWorkOrder(workOrder, WorkAllocationConstants.UPDATE);
        enrichmentService.enrichWorkOrder(workOrder, userId, WorkAllocationConstants.UPDATE);
        RestStatus restStatus = indexerService.updateEntity(workOrderIndex, workOrderIndexType, workOrder.getId(),
                mapper.convertValue(workOrder, Map.class));
        Response response = new Response();
        if (!ObjectUtils.isEmpty(restStatus)) {
            response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        } else {
            response.put(Constants.MESSAGE, Constants.FAILED);
        }
        response.put(Constants.DATA, restStatus);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     *
     * @param authUserToken auth token
     * @param userId user Id
     * @param workAllocationDTO work allocation object
     * @return
     */
    public Response addWorkAllocation(String authUserToken, String userId, WorkAllocationDTOV2 workAllocationDTO) {
        try {
            logger.info("Adding work allocation, {}", mapper.writeValueAsString(workAllocationDTO));
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
        validator.addWorkAllocation(workAllocationDTO);
        enrichmentService.enrichWorkAllocation(workAllocationDTO, userId);
        if (StringUtils.isEmpty(workAllocationDTO.getId()))
            workAllocationDTO.setId(UUID.randomUUID().toString());
        if (!CollectionUtils.isEmpty(workAllocationDTO.getRoleCompetencyList())) {
            verifyRoleActivity(authUserToken, workAllocationDTO);
            verifyCompetencyDetails(authUserToken, workAllocationDTO);
        }
        if (StringUtils.isEmpty(workAllocationDTO.getPositionId())
                && !StringUtils.isEmpty(workAllocationDTO.getUserPosition())) {
            workAllocationDTO.setPositionId(allocationService.createUserPosition(authUserToken, workAllocationDTO.getUserPosition()));
        }
        RestStatus restStatus = indexerService.addEntity(workAllocationIndex, workAllocationIndexType, workAllocationDTO.getId(),
                mapper.convertValue(workAllocationDTO, Map.class));
        Map<String, Object> workOrderObject = indexerService.readEntity(workOrderIndex, workOrderIndexType, workAllocationDTO.getWorkOrderId());
        WorkOrderDTO workOrder = mapper.convertValue(workOrderObject, WorkOrderDTO.class);
        if(CollectionUtils.isEmpty(workOrder.getUserIds())){workOrder.setUserIds(new ArrayList<>());}
        workOrder.addUserId(workAllocationDTO.getId());
        updateWorkOderCount(workOrder);
        indexerService.updateEntity(workOrderIndex, workOrderIndexType, workOrder.getId(),mapper.convertValue(workOrder, Map.class));
        Response response = new Response();
        if (!ObjectUtils.isEmpty(restStatus)) {
            response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        } else {
            response.put(Constants.MESSAGE, Constants.FAILED);
        }
        response.put(Constants.DATA, restStatus);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    private void verifyRoleActivity(String authUserToken, WorkAllocationDTOV2 workAllocation) {
        for (RoleCompetency roleCompetency : workAllocation.getRoleCompetencyList()) {
            Role oldRole = roleCompetency.getRoleDetails();
            Role newRole = null;
            try {
                if (StringUtils.isEmpty(oldRole.getId())) {
                    newRole = allocationService.fetchAddedRole(authUserToken, oldRole, null);
                    maintainExtraRoleInfo(newRole, roleCompetency);
                    roleCompetency.setRoleDetails(newRole);
                } else {
                    // Role is from FRAC - No need to create new.
                    // However, we need to check Activity is from FRAC or not.
                    if (!CollectionUtils.isEmpty(oldRole.getChildNodes())) {
                        List<ChildNode> newChildNodes = new ArrayList<>();
                        boolean isNewChildAdded = oldRole.getChildNodes().stream()
                                .anyMatch(childNode -> StringUtils.isEmpty(childNode.getId()));
                        if (isNewChildAdded) {
                            newRole = allocationService.fetchAddedRole(authUserToken, oldRole, null);
                            newChildNodes.addAll(newRole.getChildNodes());
                        } else {
                            newChildNodes.addAll(oldRole.getChildNodes());
                        }
                        oldRole.setChildNodes(newChildNodes);
                        maintainExtraRoleInfo(oldRole, roleCompetency);
                        roleCompetency.setRoleDetails(oldRole);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to Add Role / Activity. Excption: ", e);
            }
        }
    }

    private void maintainExtraRoleInfo(Role role, RoleCompetency roleCompetency) {
        if (!CollectionUtils.isEmpty(role.getChildNodes())) {
            for (ChildNode childNode : role.getChildNodes()) {
                if (!StringUtils.isEmpty(childNode.getDescription())) {
                    for (ChildNode childNode1 : roleCompetency.getRoleDetails().getChildNodes()) {
                        if (childNode.getDescription().equals(childNode1.getDescription())) {
                            childNode.setSubmittedFromId(childNode1.getSubmittedFromId());
                            childNode.setSubmittedFromName(childNode1.getSubmittedFromName());
                            childNode.setSubmittedFromEmail(childNode1.getSubmittedFromEmail());
                            childNode.setSubmittedToId(childNode1.getSubmittedToId());
                            childNode.setSubmittedToName(childNode1.getSubmittedToName());
                            childNode.setSubmittedToEmail(childNode1.getSubmittedToEmail());
                        }
                    }
                }
            }
        }
    }

    private void verifyCompetencyDetails(String authUserToken, WorkAllocationDTOV2 workAllocation) {
        for (RoleCompetency roleCompetency : workAllocation.getRoleCompetencyList()) {
            List<CompetencyDetails> oldCompetencyDetails = roleCompetency.getCompetencyDetails();
            List<CompetencyDetails> newCompetencyDetails = new ArrayList<>();
            allocationService.addOrUpdateCompetencyToFrac(authUserToken, oldCompetencyDetails, newCompetencyDetails);
            if (oldCompetencyDetails.size() == newCompetencyDetails.size()) {
                roleCompetency.setCompetencyDetails(newCompetencyDetails);
            } else {
                logger.error("Failed to create FRAC Competency / CompetencyLevel. Old List Size: {} , New List Size: {}", oldCompetencyDetails.size(), newCompetencyDetails.size());
            }
        }
    }

    public Response getWorkOrders(SearchCriteria criteria) {
        logger.info("Searching work order ....");
        validator.validateSearchCriteria(criteria);
        final BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (!StringUtils.isEmpty(criteria.getStatus())) {
            query.must(QueryBuilders.matchQuery("status", criteria.getStatus()));
        }
        if (!StringUtils.isEmpty(criteria.getDepartmentName())) {
            query.must(QueryBuilders.matchQuery("deptName", criteria.getDepartmentName()));
        }if(!StringUtils.isEmpty(criteria.getQuery())){
            query.must(QueryBuilders.matchPhrasePrefixQuery("name", criteria.getQuery()));
        }
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(query);
        sourceBuilder.from(criteria.getPageNo());
        sourceBuilder.size(criteria.getPageSize());
        sourceBuilder.sort(SortBuilders.fieldSort("name.keyword").order(SortOrder.ASC));

        List<WorkOrderDTO> workOrderDTOList = new ArrayList<>();
        long totalCount = 0;
        try {
            SearchResponse searchResponse = indexerService.getEsResult(workOrderIndex, workOrderIndexType, sourceBuilder);
            totalCount = searchResponse.getHits().getTotalHits();
            for (SearchHit hit : searchResponse.getHits()) {
                workOrderDTOList.add(mapper.convertValue(hit.getSourceAsMap(), WorkOrderDTO.class));
            }
        } catch (IOException e) {
            logger.error("Elastic Search Exception", e);
        }
        logger.info("Searching work order completed!");
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, workOrderDTOList);
        response.put("totalHit", totalCount);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    public Response getWorkOrderById(String workOrderId) throws Exception {
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, getWorkOrderObject(workOrderId));
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }
    
    public Map<String, Object> getWorkOrderObject(String workOrderId) throws Exception
    {
    	Map<String, Object> workOrderObject = indexerService.readEntity(workOrderIndex, workOrderIndexType, workOrderId);
        List<Object> userList = null;
        if (!CollectionUtils.isEmpty((Collection<?>) workOrderObject.get("userIds"))) {
            userList = new ArrayList<>();
            SearchResponse searchResponse = getSearchResponseForWorkOrder(workOrderId);
            for (SearchHit hit : searchResponse.getHits()) {
                userList.add(hit.getSourceAsMap());
            }
            workOrderObject.put("users", userList);
        } else {
            workOrderObject.put("users", new ArrayList<>());
        }
        return workOrderObject;
    }

    public Response getWorkAllocationById(String workAllocationId){
        Map<String, Object> workAllocationObject = indexerService.readEntity(workAllocationIndex, workAllocationIndexType, workAllocationId);
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, workAllocationObject);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    private SearchResponse getSearchResponseForWorkOrder(String workOrderId) throws IOException {
        final BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(QueryBuilders.termQuery("workOrderId.keyword", workOrderId));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(query);
        logger.info(sourceBuilder.query().toString());
        SearchResponse searchResponse = indexerService.getEsResult(workAllocationIndex, workAllocationIndexType, sourceBuilder);
        return searchResponse;
    }

    /**
     *
     * @param userId user Id of the user
     * @param workOrderDTO work order object
     * @return response message as success of failed
     */
    public Response copyWorkOrder(String userId, WorkOrderDTO workOrderDTO) {
        if(StringUtils.isEmpty(workOrderDTO.getId())){
            throw new BadRequestException("Work Order Id should not be empty!");
        }
        Map<String, Object> workOrderObject = indexerService.readEntity(workOrderIndex, workOrderIndexType, workOrderDTO.getId());
        if(ObjectUtils.isEmpty(workOrderObject)){
            throw new BadRequestException("No work order found on given Id!");
        }
        WorkOrderDTO workOrder = mapper.convertValue(workOrderObject, WorkOrderDTO.class);
        if(!WorkAllocationConstants.PUBLISHED_STATUS.equals(workOrder.getStatus())){
            throw new BadRequestException("Can not copy the work order, work order is not in published status!");
        }
        validator.validateWorkOrder(workOrder, WorkAllocationConstants.ADD);
        workOrder.setStatus(null);
        workOrder.setPublishedPdfLink(null);
        workOrder.setSignedPdfLink(null);
        if(!StringUtils.isEmpty(workOrderDTO.getName())){
            workOrder.setName(workOrderDTO.getName());
        }
        enrichmentService.enrichWorkOrder(workOrder, userId, WorkAllocationConstants.ADD);
        RestStatus restStatus = indexerService.addEntity(workOrderIndex, workOrderIndexType, workOrder.getId(),
                mapper.convertValue(workOrder, Map.class));
        if(!CollectionUtils.isEmpty(workOrder.getUserIds())){
            for(String id : workOrder.getUserIds()){
                WorkAllocationDTOV2 workAllocationDTO = mapper.convertValue(indexerService.readEntity(workAllocationIndex, workOrderIndexType, id), WorkAllocationDTOV2.class);
                workAllocationDTO.setCreatedBy(null);
                enrichmentService.enrichWorkAllocation(workAllocationDTO, userId);
                workAllocationDTO.setId(UUID.randomUUID().toString());
                workAllocationDTO.setWorkOrderId(workOrder.getId());
                indexerService.addEntity(workAllocationIndex, workAllocationIndexType, workAllocationDTO.getId(), mapper.convertValue(workAllocationDTO, Map.class));
            }
        }
        Response response = new Response();
        if (!ObjectUtils.isEmpty(restStatus)) {
            response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        } else {
            response.put(Constants.MESSAGE, Constants.FAILED);
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("id", workOrder.getId());
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }
    public Response getUserBasicDetails(String userId) throws IOException {
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        Set<String> userIds = new HashSet<>();
        userIds.add(userId);
        response.put(Constants.DATA, allocationService.getUserDetails(userIds).get(userId));
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    private void updateWorkOderCount(WorkOrderDTO workOrderDTO) {
        int rolesCount = 0;
        int activitiesCount = 0;
        int competenciesCount = 0;
        int errorCount = 0;
        int progress = 0;
        try {
            logger.info("Work order Object ::: , {}", mapper.writeValueAsString(workOrderDTO));
            logger.info("Work allocation Object ::: , {}", mapper.writeValueAsString(indexerService.readEntity(workAllocationIndex, workAllocationIndexType, workOrderDTO.getUserIds().get(0))));
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
        List<WorkAllocationDTOV2> workAllocationList = new ArrayList<>();
        try {
            SearchResponse searchResponse = getSearchResponseForWorkOrder(workOrderDTO.getId());
            for (SearchHit hit : searchResponse.getHits()) {
                workAllocationList.add(mapper.convertValue(hit.getSourceAsMap(), WorkAllocationDTOV2.class));
            }
        } catch (IOException e) {
            logger.error("Exception occurred while searching the users for work order!");
        }
        logger.info("Search Response for work order to update the count");
        try {
            logger.info(mapper.writeValueAsString(workAllocationList));
        } catch (JsonProcessingException e) {
            logger.error(e.toString());
        }
        for (WorkAllocationDTOV2 workAllocationDTOV2 : workAllocationList) {
            if (!CollectionUtils.isEmpty(workAllocationDTOV2.getRoleCompetencyList())) {
                rolesCount = rolesCount + workAllocationDTOV2.getRoleCompetencyList().size();
                for (RoleCompetency roleCompetency : workAllocationDTOV2.getRoleCompetencyList()) {
                    if (!ObjectUtils.isEmpty(roleCompetency.getRoleDetails()) && !CollectionUtils.isEmpty(roleCompetency.getRoleDetails().getChildNodes()))
                        activitiesCount = activitiesCount + roleCompetency.getRoleDetails().getChildNodes().size();
                    if (!ObjectUtils.isEmpty(roleCompetency.getRoleDetails()) && !CollectionUtils.isEmpty(roleCompetency.getCompetencyDetails()))
                        competenciesCount = competenciesCount + roleCompetency.getCompetencyDetails().size();
                }
            }
            if (!CollectionUtils.isEmpty(workAllocationDTOV2.getUnmappedActivities())) {
                activitiesCount = activitiesCount + workAllocationDTOV2.getUnmappedActivities().size();
            }
            if (!CollectionUtils.isEmpty(workAllocationDTOV2.getUnmappedCompetencies())) {
                competenciesCount = competenciesCount + workAllocationDTOV2.getUnmappedCompetencies().size();
            }
            errorCount = errorCount + workAllocationDTOV2.getErrorCount();
            progress = progress + workAllocationDTOV2.getProgress();
        }
        if (!CollectionUtils.isEmpty(workAllocationList)) {
            progress = progress / workAllocationList.size();
        }
        workOrderDTO.setRolesCount(rolesCount);
        workOrderDTO.setActivitiesCount(activitiesCount);
        workOrderDTO.setCompetenciesCount(competenciesCount);
        workOrderDTO.setErrorCount(errorCount);
        workOrderDTO.setProgress(progress);
    }

}
