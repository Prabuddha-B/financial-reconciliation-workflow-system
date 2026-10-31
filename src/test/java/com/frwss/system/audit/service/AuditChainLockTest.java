package com.frwss.system.audit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Chain Lock Unit Tests")
class AuditChainLockTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private PreparedStatement statement;

    @Test
    @DisplayName("Should take the PostgreSQL advisory lock using the fixed chain key")
    void acquire_postgres_takesAdvisoryLock() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")).thenReturn(statement);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        new AuditChainLock(dataSource).acquire();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(statement).setLong(1, AuditChainLock.CHAIN_LOCK_KEY);
        verify(statement).execute();
        verify(connection).close();
    }

    @Test
    @DisplayName("Should skip the lock on databases other than PostgreSQL")
    void acquire_otherDatabase_skipsLock() throws Exception {

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        new AuditChainLock(dataSource).acquire();

        verify(connection, never()).prepareStatement(anyString());
        verify(connection).close();
    }

    @Test
    @DisplayName("Should fail loudly instead of writing an unprotected entry when the lock cannot be taken")
    void acquire_sqlFailure_throwsIllegalStateException() throws Exception {

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        assertThrows(IllegalStateException.class, () -> new AuditChainLock(dataSource).acquire());

        verify(connection).close();
    }
}