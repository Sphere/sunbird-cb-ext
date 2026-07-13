package org.sunbird.passbook.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.common.model.SBApiResponse;
import org.sunbird.common.util.Constants;
import org.sunbird.passbook.model.dto.CertificateRequest;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/certificate")
public class CertificateController {

    private static final String SQL = " SELECT is_completed, is_certificate_generated, certificate_url " +
            "FROM user_program_completion WHERE root_org_id = ? AND program_id = ? AND user_id = ?";

    private static final String API_ID = "api.program.certification";

    private final JdbcTemplate jdbc;

    @Autowired
    public CertificateController(@Qualifier("certificateJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(@Valid @RequestBody CertificateRequest req) {
        List<Map<String, Object>> rows = jdbc.queryForList(SQL, req.getRootOrgId(), req.getProgramId(), req.getUserId());
        if (rows.isEmpty()) return error(404, "RESOURCE_NOT_FOUND");
        Map<String, Object> r = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootOrgId", req.getRootOrgId());
        result.put("userId", req.getUserId());
        result.put("programId", req.getProgramId());
        result.put("isCompleted", r.get("is_completed"));
        result.put("isCertificateGenerated", r.get("is_certificate_generated"));
        return ok(result);
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(@Valid @RequestBody CertificateRequest req) {
        List<Map<String, Object>> rows = jdbc.queryForList(SQL, req.getRootOrgId(), req.getProgramId(), req.getUserId());
        if (rows.isEmpty()) return error(404, "RESOURCE_NOT_FOUND");
        Map<String, Object> r = rows.get(0);
        if (!Boolean.TRUE.equals(r.get("is_completed"))) return error(400, "PROGRAM_NOT_COMPLETED");
        String url = (String) r.get("certificate_url");
        if (url == null || url.trim().isEmpty()) return error(404, "CERTIFICATE_NOT_GENERATED");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootOrgId", req.getRootOrgId());
        result.put("userId", req.getUserId());
        result.put("programId", req.getProgramId());
        result.put("certificateUrl", url);
        return ok(result);
    }
    // ok()/error() build the standard SBApiResponse envelope
    // (id="api.program.certification", ver="v1", ts, params, responseCode, result).
    private ResponseEntity<SBApiResponse> ok(Map<String, Object> result) {
        SBApiResponse response = new SBApiResponse(API_ID);
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setResult(result);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    private ResponseEntity<SBApiResponse> error(int statusCode, String message) {
        HttpStatus status = HttpStatus.valueOf(statusCode);
        SBApiResponse response = new SBApiResponse(API_ID);
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErr(message);
        response.getParams().setErrmsg(message);
        response.setResponseCode(status);
        return new ResponseEntity<>(response, status);
    }
}
