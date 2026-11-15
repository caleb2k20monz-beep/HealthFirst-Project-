import java.sql.*;

class DB {
    private static final String URL = System.getenv().getOrDefault(
            "PIMS_DB_URL", "jdbc:mysql://localhost:3306/healthfirst_pims?useSSL=false&serverTimezone=UTC");
    private static final String USER = System.getenv().getOrDefault("PIMS_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("PIMS_DB_PASSWORD", "");

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
