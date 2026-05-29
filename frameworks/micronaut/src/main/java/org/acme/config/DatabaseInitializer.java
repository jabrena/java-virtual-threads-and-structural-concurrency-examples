package org.acme.config;

import io.micronaut.context.annotation.Context;
import io.micronaut.transaction.TransactionOperations;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
@Singleton
public class DatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final TransactionOperations<?> transactionOperations;

    public DatabaseInitializer(DataSource dataSource, TransactionOperations<?> transactionOperations) {
        this.dataSource = dataSource;
        this.transactionOperations = transactionOperations;
    }

    @PostConstruct
    public void initialize() {
        LOGGER.info("Initializing database schema and reference data");
        transactionOperations.executeWrite(status -> {
            try (Connection connection = dataSource.getConnection()) {
                executeScript(connection, "schema.sql");
                executeScript(connection, "import.sql");
                LOGGER.info("Database initialization completed");
            } catch (SQLException | IOException ex) {
                LOGGER.error("Database initialization failed", ex);
                throw new IllegalStateException("Unable to initialize database", ex);
            }
            return null;
        });
    }

    private static void executeScript(Connection connection, String resourceName) throws IOException, SQLException {
        String script = new String(
                DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName).readAllBytes(),
                StandardCharsets.UTF_8);
        int executedStatements = 0;
        for (String statementText : script.split(";")) {
            String sql = statementText.trim();
            if (sql.isEmpty()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                executedStatements++;
            }
        }
        LOGGER.debug("Executed {} statements from {}", executedStatements, resourceName);
    }
}
