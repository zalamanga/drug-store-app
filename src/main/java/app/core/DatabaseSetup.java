package app.core;

import Config.DBConnect;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;

/** Explicit setup command for an empty, disposable demonstration database. */
public final class DatabaseSetup {
  private DatabaseSetup() {}

  public static void install(Connection c, String username, char[] password)
      throws SQLException, IOException {
    if (username == null || !username.matches("[A-Za-z0-9_.-]{3,40}"))
      throw new IllegalArgumentException("Set HALOTEK_ADMIN_USERNAME (3-40 karakter).");
    String hash = Passwords.hash(password);
    try (InputStream in = DatabaseSetup.class.getResourceAsStream("/db/schema.sql")) {
      if (in == null) throw new IOException("Schema resource missing.");
      String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      try (Statement s = c.createStatement()) {
        // DDL contains only repository-controlled schema, never user input.
        for (String statement : schema.split(";"))
          if (!statement.isBlank()) s.execute(statement.trim());
      }
    }
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO mUser (id_user,nama_user,username,password,jabatan,telp_user,status_user) VALUES (?,?,?,?,?,?,?)")) {
      p.setString(1, "USR_ADMIN_DEMO");
      p.setString(2, "Administrator Demo");
      p.setString(3, username);
      p.setString(4, hash);
      p.setString(5, "Admin");
      p.setString(6, "0000000000");
      p.setString(7, "Aktif");
      p.executeUpdate();
    }
  }

  public static void main(String[] args) {
    if (args.length != 1 || !args[0].equals("--empty-demo-database")) {
      System.err.println(
          "Usage: DatabaseSetup --empty-demo-database. Creates schema in an EMPTY demo database; never use an original database.");
      return;
    }
    String raw = System.getenv("HALOTEK_ADMIN_PASSWORD");
    char[] password = raw == null ? new char[0] : raw.toCharArray();
    try (Connection c = DBConnect.open()) {
      install(c, System.getenv("HALOTEK_ADMIN_USERNAME"), password);
      System.out.println(
          "Schema demo dibuat. Masuk dengan username dan password yang Anda tetapkan.");
    } catch (SQLException | IOException | IllegalArgumentException ex) {
      System.err.println(
          "Setup gagal. Gunakan database demo kosong, konfigurasi DB yang benar, dan password admin 12+ karakter. Schema yang sudah ada tidak ditimpa.");
      System.exit(1);
    } finally {
      Arrays.fill(password, '\0');
    }
  }
}
