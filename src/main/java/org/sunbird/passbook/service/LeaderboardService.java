package org.sunbird.passbook.service;

import org.sunbird.common.model.SBApiResponse;
import org.sunbird.passbook.model.dto.LeaderboardRequestDTO;

public interface LeaderboardService {

    SBApiResponse getAllLeaderBoard(LeaderboardRequestDTO leaderboardRequestDTO);
}
