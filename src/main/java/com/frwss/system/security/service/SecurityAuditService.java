package com.frwss.system.security.service;

import com.frwss.system.security.model.SecurityAuditLog;
import com.frwss.system.security.repository.SecurityAuditLogRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository repository;

    public SecurityAuditService(SecurityAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(Integer userId, String username, String eventType,
                    String outcome, String ipAddress, String message) {

        String previousHash = repository.findTopByOrderByLogIdDesc()
                .map(SecurityAuditLog::getCurrentHash)
                .orElse("GENESIS");

        SecurityAuditLog log = new SecurityAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setEventType(eventType);
        log.setOutcome(outcome);
        log.setIpAddress(ipAddress);
        log.setMessage(message);
        log.setPreviousHash(previousHash);
        // Truncate to microseconds: matches PostgreSQL's timestamp precision,
        // so the value used to compute the hash below is identical to what
        // comes back after a save/reload (see the same fix in AuditService).
        log.setCreatedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        log.setCurrentHash(generateHash(log));

        repository.save(log);
    }

    private String generateHash(SecurityAuditLog log) {
        String data = log.getUserId()
                + log.getUsername()
                + log.getEventType()
                + log.getOutcome()
                + log.getIpAddress()
                + log.getMessage()
                + log.getPreviousHash()
                + log.getCreatedAt();

        return DigestUtils.sha256Hex(data);
    }
}