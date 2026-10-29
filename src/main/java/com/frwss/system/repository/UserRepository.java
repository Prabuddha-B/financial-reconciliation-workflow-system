package com.frwss.system.repository;

import com.frwss.system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    @Query("SELECT COALESCE(MAX(u.userId),0) FROM User u")
    Integer findMaxUserId();

    // Retrieves enabled users with the same role excluding the current user
    @Query("SELECT u FROM User u JOIN u.roles r "
            + "WHERE r.name = :roleName "
            + "AND u.enabled = true "
            + "AND u.username <> :currentUsername")
    List<User> findEnabledUsersByRoleNameExcludingUsername(
            @Param("roleName") String roleName,
            @Param("currentUsername") String currentUsername
    );

    // ------------------------------------------------------------
    // User Management Filters
    // ------------------------------------------------------------
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN u.roles r
        WHERE
            (:username IS NULL OR :username = ''
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
        AND
            (:role IS NULL OR :role = '' OR r.name = :role)
        AND
            (:enabled IS NULL OR u.enabled = :enabled)
        ORDER BY u.username
        """)
    List<User> findUsersByFilters(
            @Param("username") String username,
            @Param("role") String role,
            @Param("enabled") Boolean enabled
    );

    // ------------------------------------------------------------
    // Dashboard Statistics
    // ------------------------------------------------------------

    long countByEnabledTrue();

    long countByEnabledFalse();

    @Query("""
            SELECT COUNT(u)
            FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
            """)
    long countUsersByRole(
            @Param("roleName") String roleName
    );

}