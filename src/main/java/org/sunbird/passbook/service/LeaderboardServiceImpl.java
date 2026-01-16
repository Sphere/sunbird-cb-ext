package org.sunbird.passbook.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sunbird.common.model.SBApiResponse;
import org.sunbird.common.util.Constants;
import org.sunbird.common.util.ProjectUtil;
import org.sunbird.passbook.entity.LeaderboardEntity;
import org.sunbird.passbook.model.dto.LeaderboardRequestDTO;
import org.sunbird.passbook.model.dto.LeaderboardResponseDTO;
import org.sunbird.passbook.repo.LeaderboardRepository;

import javax.annotation.PostConstruct;
import javax.persistence.Column;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaderboardServiceImpl implements LeaderboardService{
    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    private List<String> allowedFilterColumns;

    @PostConstruct
    public void init() {
        // Get all field names from entity using reflection
        allowedFilterColumns = Arrays.stream(LeaderboardEntity.class.getDeclaredFields())
                .map(field -> {
                    Column column = field.getAnnotation(Column.class);
                    return column != null ? column.name() : field.getName();
                })
//                .filter(name -> !name.equals("userid")) // Exclude primary key from filters
                .collect(Collectors.toList());
    }

    /**
     * @param leaderboardRequestDTO
     * @return
     */
    @Override
    public SBApiResponse getAllLeaderBoard(LeaderboardRequestDTO leaderboardRequestDTO) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.USER_LEADERBOARD_READ_API);
        try {
            Optional<String> errorMessage = validateLeaderBoardContent(leaderboardRequestDTO);

            if (errorMessage.isPresent()) {
                response.getParams().setStatus(Constants.FAILED);
                response.getParams().setErrmsg(errorMessage.get());
                response.setResponseCode(HttpStatus.BAD_REQUEST);

                return response;
            }

            Optional<LeaderboardResponseDTO> leaderboardResponseDTOOptional =
                    getWrappedResponseData(leaderboardRequestDTO);

            if (!leaderboardResponseDTOOptional.isPresent()) {
                logger.error("Some data is missing in leaderboard list or active user details");
                response.getParams().setStatus(Constants.FAILED);
                response.getParams().setErrmsg("Some data is missing in leaderboard list or active user details");
                response.setResponseCode(HttpStatus.BAD_REQUEST);
            }

            response.getResult().put(Constants.COUNT, leaderboardResponseDTOOptional.get().getLeaderboardList().size());
            response.getResult().put(Constants.CONTENT, leaderboardResponseDTOOptional.get());
        } catch (Exception e) {
            logger.error("Error while collecting leaderboard list", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(e.getMessage());
            response.setResponseCode(HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    /** No need of verification of Leaderboard request DTO - it should be done by callee method.
     *
     * @param leaderboardRequestDTO - No validation required.
     * @return
     */
    private Optional<LeaderboardResponseDTO> getWrappedResponseData(LeaderboardRequestDTO leaderboardRequestDTO) {
        LeaderboardResponseDTO leaderboardResponseDTO = new LeaderboardResponseDTO();

        List<LeaderboardEntity> leaderboardEntityList = leaderboardRepository
                .findByDynamicFilters(
                        leaderboardRequestDTO.getFilterAttribute(),
                        leaderboardRequestDTO.getLimit(),
                        leaderboardRequestDTO.getOffset());

        if (leaderboardEntityList == null || leaderboardEntityList.isEmpty()) {
            return Optional.empty();
        }

        int startRank = leaderboardRequestDTO.getOffset() + 1;

        for (int i = 0; i < leaderboardEntityList.size(); i++) {  // TODO: need to rectify
            leaderboardEntityList.get(i).setRank(startRank + i);
        }
        leaderboardResponseDTO.setLeaderboardList(leaderboardEntityList);

        Optional<LeaderboardEntity> leaderboardEntityOptional = leaderboardRepository
                .findOneByUserId(leaderboardRequestDTO.getActiveUserId());

        if (!leaderboardEntityOptional.isPresent()) {
            return Optional.empty();
        }

        Integer activeUserRank = leaderboardRepository.findUserRank(leaderboardEntityOptional.get() );
        leaderboardEntityOptional.get().setRank(activeUserRank);

//        leaderboardResponseDTO.getActiveUserDetails().setRank(activeUserRank);

        leaderboardResponseDTO.setActiveUserDetails(leaderboardEntityOptional.get());

        return Optional.of(leaderboardResponseDTO);
    }

    private Optional<String> validateLeaderBoardContent(LeaderboardRequestDTO leaderboardRequestDTO) {
        if (leaderboardRequestDTO == null) {
            logger.error("LeaderboardRequestDTO is null");
            return Optional.of("Invalid request. Leaderboard data is required");
        }

        // Validate activeUserId
        if (StringUtils.isEmpty(leaderboardRequestDTO.getActiveUserId())) {
            logger.error("ActiveUserId is missing in leaderboard request");
            return Optional.of("User ID is required");
        }

        // Validate limit if provided
        if (leaderboardRequestDTO.getLimit() == null) {
            logger.error("Invalid limit value");
            return Optional.of("Limit is required field");
        }else {
            if (leaderboardRequestDTO.getLimit() <= 0) {
                logger.error("Invalid limit value: {}. Must be greater than 0",
                        leaderboardRequestDTO.getLimit());
                return Optional.of("Limit must be a positive number");
            }
            if (leaderboardRequestDTO.getLimit() > 1000) {
                logger.error("Limit exceeds maximum: {}. Maximum allowed is 1000",
                        leaderboardRequestDTO.getLimit());
                return Optional.of("Maximum limit is 1000 records");
            }
        }

        // Validate offset if provided
        if (leaderboardRequestDTO.getOffset() == null) {
            logger.error("Invalid offset value");
            return Optional.of("Offset is required filed");
        }else {
            if (leaderboardRequestDTO.getOffset() < 0) {
                logger.error("Invalid offset value: {}. Must be non-negative",
                        leaderboardRequestDTO.getOffset());
                return Optional.of("Offset must be a non-negative number");
            }
        }

        if (leaderboardRequestDTO.getFilterAttribute() == null
                || leaderboardRequestDTO.getFilterAttribute().isEmpty()) {
            logger.error("Invalid filter map - no value exist");
            return Optional.of("Invalid filter map");
        }

        for (String key : leaderboardRequestDTO.getFilterAttribute().keySet()) {
            // Check if filter key is not there
            if (!allowedFilterColumns.contains(key.toLowerCase())) {
                logger.error("Invalid filter attribute key: {}. Allowed columns: {}",
                        key, allowedFilterColumns);
                return Optional.of("Invalid filter field: " + key);
            }

            // Check if filter value is not empty
            Object value = leaderboardRequestDTO.getFilterAttribute().get(key);
            if (value == null) {
                logger.error("Filter attribute '{}' has empty or null value", key);
                return Optional.of("Filter value cannot be empty for field: " + key);
            }
        }

        return Optional.empty();
    }
}
