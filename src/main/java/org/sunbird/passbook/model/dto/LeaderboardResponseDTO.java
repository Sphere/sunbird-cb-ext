package org.sunbird.passbook.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sunbird.passbook.entity.LeaderboardEntity;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponseDTO {
    private List<LeaderboardEntity> leaderboardList;

    private LeaderboardEntity activeUserDetails;
}
