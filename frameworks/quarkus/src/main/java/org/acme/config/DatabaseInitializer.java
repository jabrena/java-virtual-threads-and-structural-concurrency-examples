package org.acme.config;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

@Startup
@ApplicationScoped
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    void initialize() {
        try (Connection connection = dataSource.getConnection()) {
            executeScript(connection, "schema.sql");
            executeScript(connection, "import.sql");
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Unable to initialize database", ex);
        }
    }

    private static void executeScript(Connection connection, String resourceName) throws IOException, SQLException {
        String script = new String(
                DatabaseInitializer.class.getClassLoader().getResourceAsStream(resourceName).readAllBytes(),
                StandardCharsets.UTF_8);
        for (String statementText : script.split(";")) {
            String sql = statementText.trim();
            if (sql.isEmpty()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }
}
