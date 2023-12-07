package com.contentgrid.surveyor.application.exporter.postgres.connections;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DatabaseConnection {

    private final String url;
    private final ObservationRegistry observationRegistry;
    private final DataSource dataSource;

    public <T> T lease(ConnectionQuerier<T> querier) {
        var leaseObservation = Observation.createNotStarted("database.lease", observationRegistry);
        leaseObservation.highCardinalityKeyValue("db.url", url);
        return leaseObservation.scoped(() -> {
            try (
                    var dbConnection = Observation.createNotStarted("database.connect", observationRegistry)
                            .observeChecked(() -> dataSource.getConnection())
            ) {
                log.trace("Checked out connection {}: {}", this, dbConnection);
                return Observation.createNotStarted("database.query", observationRegistry)
                        .observeChecked(() -> querier.query(dbConnection));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                log.trace("Released connection {}", this);
            }

        });
    }

    public interface ConnectionQuerier<T> {
        T query(Connection connection) throws SQLException;
    }
}
