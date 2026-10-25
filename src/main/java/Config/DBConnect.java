package Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens a short-lived connection. Credentials never belong in a JDBC URL. */
public final class DBConnect {
  private DBConnect() {}

  public static Connection open() throws SQLException {
    String url = setting("halotek.db.url", "DB_URL");
    String user = setting("halotek.db.user", "DB_USER");
    String password = setting("halotek.db.password", "DB_PASSWORD");
    if (url == null
        || !url.startsWith("jdbc:sqlserver://")
        || url.toLowerCase().matches(".*(?:;password=|;user=|;username=).*")) {
      throw new SQLException("Set DB_URL to a SQL Server URL without credentials.");
    }
    if (user == null || user.isBlank() || password == null || password.isBlank()) {
      throw new SQLException("DB_USER and DB_PASSWORD are required.");
    }
    Properties properties = new Properties();
    properties.setProperty("user", user);
    properties.setProperty("password", password);
    properties.setProperty("loginTimeout", "5");
    properties.setProperty("socketTimeout", "10000");
    return DriverManager.getConnection(url, properties);
  }

  private static String setting(String property, String environment) {
    String value = System.getProperty(property);
    return value != null ? value : System.getenv(environment);
  }
}
