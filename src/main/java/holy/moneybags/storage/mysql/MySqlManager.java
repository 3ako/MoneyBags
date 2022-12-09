package holy.moneybags.storage.mysql;

import holy.moneybags.storage.files.Configuration;
import holy.moneybags.storage.files.ConfigurationManager;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class MySqlManager {
    private final Configuration config;
    private Stack<Connection> freePool = new Stack<>();
    private Set<Connection> occupiedPool = new HashSet<>();
    @Getter
    private final String baseName;
    public MySqlManager(ConfigurationManager configurationManager){
        this.config = configurationManager.getConfig("config.yml");
        baseName = config.c("storage.mysql.baseName");
        setup();
    }
    private Connection makeAvailable(Connection conn) throws SQLException {
        if (isConnectionAvailable(conn)) {
            return conn;
        }

        occupiedPool.remove(conn);
        conn.close();

        conn = createNewConnection();
        occupiedPool.add(conn);
        return conn;
    }
    private boolean isConnectionAvailable(Connection conn) {
        try (Statement st = conn.createStatement()) {
            st.executeQuery("select 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    private Connection createNewConnectionForPool() throws SQLException {
        Connection conn = createNewConnection();
        occupiedPool.add(conn);
        return conn;
    }
    private Connection createNewConnection() throws SQLException {
        Connection conn;
        String databaseUrl = "jdbc:mysql://" + config.c("storage.mysql.host") + ":" + config.c("storage.mysql.port");
        conn = DriverManager.getConnection(databaseUrl, config.c("storage.mysql.user"), config.c("storage.mysql.password"));
        return conn;
    }
    private Connection getConnectionFromPool() {
        Connection conn = null;

        if (freePool.size() > 0) {
            conn = freePool.pop();
            occupiedPool.add(conn);
        }

        return conn;
    }
    public synchronized Connection getConnection(){
        try {
            Connection conn;

            if (isFull()) {
                throw new SQLException("Exceeded the maximum number of connections");
            }

            conn = getConnectionFromPool();

            if (conn == null) {
                conn = createNewConnectionForPool();
            }

            conn = makeAvailable(conn);
            return conn;
        } catch (SQLException e){
            e.printStackTrace();
        }

        return null;
    }
    public synchronized void returnConnection(Connection conn) {
        try {
            if (conn == null) {
                throw new NullPointerException();
            }
            occupiedPool.remove(conn);
            freePool.push(conn);
        } catch (NullPointerException e){
            e.printStackTrace();
        }

    }
    private synchronized boolean isFull() {
        return ((freePool.size() == 0) && (freePool.size()+occupiedPool.size() >= config.getFile().getInt("storage.mysql.maxConnections")));
    }
    private void setup(){
        Connection conn = getConnection();
        try {
            Statement statement = conn.createStatement();

            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `"+baseName+"`;");

            statement.execute("USE "+baseName+";");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `dynamic_items` (" +
                    "  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL," +
                    "  `progress` int DEFAULT '0'," +
                    "  `material` varchar(32) DEFAULT NULL," +
                    "  `lvl` int DEFAULT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;");
//
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `static_items_statistic` (" +
                    "  `uuid` varchar(36) NOT NULL," +
                    "  `material` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL," +
                    "  `progress` int NOT NULL DEFAULT '0'" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `users` (" +
                    "  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL," +
                    "  `lvl` int DEFAULT '1'," +
                    "  PRIMARY KEY (`uuid`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;");
//
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            returnConnection(conn);
        }
    }
}
