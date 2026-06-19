package com.frwss.system.audit.repository;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByOrderByIdDesc();

    List<AuditLog> findAllByOrderByIdAsc();

    List<AuditLog> findAllByOrderByTimestampDesc();

//    @Query("""
//       SELECT a FROM AuditLog a
//       WHERE (:actionType IS NULL OR a.actionType = :actionType)
//       AND (:entityType IS NULL OR LOWER(a.entityType) LIKE LOWER(CONCAT('%', :entityType, '%')))
//       AND (:performedBy IS NULL OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%')))
//       ORDER BY a.timestamp DESC
//       """)
//    List<AuditLog> searchAuditLogs(
//            @Param("actionType") AuditActionType actionType,
//            @Param("entityType") String entityType,
//            @Param("performedBy") String performedBy
//    );


}
