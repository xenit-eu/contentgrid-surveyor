package com.contentgrid.surveyor.application.exporter.postgres.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DatabaseConnection {
    private final DataSource dataSource;


    public <T> T lease(ConnectionQuerier<T> querier) {
        try (var dbConnection = dataSource.getConnection()){
            log.info("Checked out connection {}: {}", this, dbConnection);
            return querier.query(dbConnection);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            log.info("Released connection {}", this);
        }
    }

    public interface ConnectionQuerier<T> {
        T query(Connection connection) throws SQLException;
    }
}
