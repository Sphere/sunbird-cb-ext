package org.sunbird.passbook.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {
    @JsonProperty("rootOrgId")
    @NotBlank
    private String rootOrgId;

    @JsonProperty("programId")
    @NotBlank
    String programId;

    @JsonProperty("userId")
    @NotBlank
    String userId;
}
