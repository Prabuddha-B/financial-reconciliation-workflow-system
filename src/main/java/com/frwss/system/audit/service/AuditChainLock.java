package com.frwss.system.audit.service;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Makes audit writes happen one at a time across the whole application.
 *
 * Each audit entry is chained to the previous one, so two entries written at
 * the same moment would both chain to the same "latest" entry and verification
 * would later report a false break. This takes a PostgreSQL advisory lock that
 * is held until the current transaction ends, so the next writer waits and
 * then sees the entry that was just written.
 *
 * On any other database (for example the in-memory H2 used by the tests) the
 * lock is skipped, because advisory locks are PostgreSQL specific.
 */
@Component
public class AuditChainLock {

    // Arbitrary application-wide key identifying the "audit chain" lock
    static final long CHAIN_LOCK_KEY = 7265001001L;

    private final DataSource dataSource;

    public AuditChainLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Blocks until the audit chain lock is free, then holds it until the
     * surrounding transaction commits or rolls back. Calling it again within
     * the same transaction is harmless.
     */
    public void acquire() {
        // Returns the connection of the current transaction, so the lock is
        // released automatically when that transaction ends.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            String product = connection.getMetaData().getDatabaseProductName();
            if (!"PostgreSQL".equalsIgnoreCase(product)) {
                return;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, CHAIN_LOCK_KEY);
                statement.execute();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not acquire the audit chain lock", e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
