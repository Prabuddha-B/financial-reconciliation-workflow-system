package com.frwss.system.security.repository;

import com.frwss.system.security.model.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {
    Optional<SecurityAuditLog> findTopByOrderByLogIdDesc();
}
