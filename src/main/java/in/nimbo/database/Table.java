package in.nimbo.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class Table {
    private static final Logger LOGGER = LoggerFactory.getLogger(Table.class);
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties databaseProperties = new Properties();
        try {
            databaseProperties.load(new FileInputStream(Objects.requireNonNull(Thread.currentThread().
                    getContextClassLoader().getResource("database.properties")).getPath()));
        } catch (IOException e) {
            LOGGER.error("", e);
            System.exit(0);
        }
        URL = databaseProperties.getProperty("url");
        USER = databaseProperties.getProperty("user");
        PASSWORD = databaseProperties.getProperty("password");
    }

    private final PreparedStatement searchTitle;
    private final PreparedStatement searchTitleInDate;
    private final PreparedStatement searchDescriptionInDate;
    private final String name;

    public Table(final String name) throws SQLException {
        Connection searchTitleConnection = DriverManager.getConnection(URL, USER, PASSWORD);
        Connection searchTitleInDateConnection = DriverManager.getConnection(URL, USER, PASSWORD);
        Connection searchDescriptionInDateConnection = DriverManager.getConnection(URL, USER, PASSWORD);
        try (final Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             final Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = '%s') THEN CREATE TABLE %s(agency text, " +
                    "title text, published_date timestamp without time zone, description text, author text); " +
                    "END IF; END $$;", name, name));
            searchTitle = searchTitleConnection.prepareStatement(String.format("SELECT * FROM %s WHERE title ~ ?;", name
            ));
            searchTitleInDate = searchTitleInDateConnection.prepareStatement(String.format("SELECT * FROM %s WHERE " +
                    "title ~ ? AND published_date >= ? AND published_date <= ?;", name));
            searchDescriptionInDate = searchDescriptionInDateConnection.prepareStatement(String.format("SELECT * " +
                    "FROM %s WHERE description ~ ? AND published_date >= ? AND published_date <= ?;", name));
        }
        this.name = name;
    }

    public void insert(final String agencyName, final String title, final Date publishedDate, final String description,
                       final String author) throws SQLException {
        try (final Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             final Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM %s WHERE agency = '%s' " +
                    "AND title = '%s') THEN INSERT INTO %s(agency, title, published_date, description, author) " +
                    "VALUES ('%s', '%s', TIMESTAMP (6) WITHOUT TIME ZONE '%s', '%s', '%s'); END IF; END $$;", name,
                    agencyName, title, name, agencyName, title, new Timestamp(publishedDate.getTime()), description,
                    author));
        }
    }

    public ResultSet searchTitle(final String title) throws SQLException {
        searchTitle.setString(1, title);
        return searchTitle.executeQuery();
    }

    public ResultSet searchTitleInDate(final String title, final Date from, final Date to) throws SQLException {
        searchTitleInDate.setString(1, title);
        searchTitleInDate.setTimestamp(2, new Timestamp(from.getTime()));
        searchTitleInDate.setTimestamp(3, new Timestamp(to.getTime()));
        return searchTitleInDate.executeQuery();
    }

    public ResultSet searchDescriptionInDate(String description, Date from, Date to) throws SQLException {
        searchDescriptionInDate.setString(1, description);
        searchDescriptionInDate.setTimestamp(2, new Timestamp(from.getTime()));
        searchDescriptionInDate.setTimestamp(3, new Timestamp(to.getTime()));
        return searchDescriptionInDate.executeQuery();
    }
}
