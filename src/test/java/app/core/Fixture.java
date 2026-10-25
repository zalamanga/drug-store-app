package app.core;

import java.sql.*;
import java.util.UUID;

public final class Fixture {
  public static final String PASSWORD = "Synthetic-Test-Only!42";
  private final String url =
      "jdbc:h2:mem:"
          + UUID.randomUUID()
          + ";MODE=MSSQLServer;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
  public final AppService service = new AppService(this::open);

  public Fixture() throws Exception {
    try (Connection c = open()) {
      DatabaseSetup.install(c, "test-admin", PASSWORD.toCharArray());
    }
    user("KASIR_TEST", "test-kasir", Role.KASIR);
    user("KEPALA_TEST", "test-kepala", Role.KEPALA);
    execute("INSERT INTO mKategori VALUES ('KTG_TEST','Kategori sintetis','Untuk pengujian')");
    execute(
        "INSERT INTO mSupplier VALUES ('SUP_TEST','Supplier sintetis','0000000000','Alamat demo','Aktif')");
    execute("INSERT INTO mCustomer VALUES ('CUS_TEST','Customer sintetis','0000000000','Aktif')");
    execute(
        "INSERT INTO mResep VALUES ('RSP_TEST','CUS_TEST','Dokter demo','Praktek demo','Instruksi sintetis','2026-01-01')");
    execute(
        "INSERT INTO mObat VALUES ('OBT_TEST','Obat sintetis','KTG_TEST','Bahan demo','Tablet','Demo','2099-01-01',20,1250.50)");
    login(Role.ADMIN);
  }

  public Connection open() throws SQLException {
    return DriverManager.getConnection(url);
  }

  public void login(Role role) throws SQLException {
    String username =
        role == Role.ADMIN ? "test-admin" : role == Role.KASIR ? "test-kasir" : "test-kepala";
    if (service.login(username, PASSWORD.toCharArray()) == null)
      throw new AssertionError("Fixture login failed.");
  }

  public void user(String id, String username, Role role) throws SQLException {
    try (Connection c = open();
        PreparedStatement p = c.prepareStatement("INSERT INTO mUser VALUES (?,?,?,?,?,?,?)")) {
      p.setString(1, id);
      p.setString(2, "Akun sintetis");
      p.setString(3, username);
      p.setString(4, Passwords.hash(PASSWORD.toCharArray()));
      p.setString(5, role.label());
      p.setString(6, "0000000000");
      p.setString(7, "Aktif");
      p.executeUpdate();
    }
  }

  public void execute(String sql) throws SQLException {
    try (Connection c = open();
        Statement s = c.createStatement()) {
      s.execute(sql);
    }
  }

  public int integer(String sql) throws SQLException {
    try (Connection c = open();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(sql)) {
      r.next();
      return r.getInt(1);
    }
  }
}
