package com.example.requestmanagement.service;

import com.example.requestmanagement.exception.DatabaseRetryException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PublicationNumberGenerator {

    private static final String SQL = "SELECT PUBLICATION_SEQ.NEXTVAL FROM DUAL";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MILLIS = 1_000L;

    private final DataSource dataSource;

    public PublicationNumberGenerator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long next() {

        SQLException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {

            try (Connection connection = dataSource.getConnection();

                 PreparedStatement statement = connection.prepareStatement(SQL);

                 ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {

                    throw new DatabaseRetryException("Publication sequence did not return a value", null);
                }

                long value = resultSet.getLong(1);

                if (resultSet.wasNull()) {

                    throw new DatabaseRetryException("Publication sequence returned NULL", null);
                }

                return value;

            } catch (SQLException ex) {

                lastException = ex;
                if (attempt > MAX_RETRIES) {
                    break;
                }
                sleepBeforeRetry();
            }
        }
        throw new DatabaseRetryException("Unable to generate publication number after " + MAX_RETRIES + " retries", lastException);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DatabaseRetryException("Database retry interrupted", ex);
        }
    }
}