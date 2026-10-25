package app.core;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class SqlServerIntegrationTest {
  @Test
  void isolatedSqlServerSchemaCrudStockRollbackAndReport() throws Exception {
    Assumptions.assumeTrue(
        "1".equals(System.getenv("HALOTEK_SQLSERVER_QA")),
        "Explicit local SQL Server QA opt-in required.");
    String supplied = System.getenv("DB_URL");
    assertNotNull(supplied);
    assertTrue(supplied.startsWith("jdbc:sqlserver://localhost:14343;"));
    assertTrue(supplied.contains("databaseName=halotek_demo;"));
    Properties properties = new Properties();
    properties.setProperty("user", System.getenv("DB_USER"));
    properties.setProperty("password", System.getenv("DB_PASSWORD"));
    properties.setProperty("loginTimeout", "5");
    properties.setProperty("socketTimeout", "10000");
    String name =
        "halotek_qa_20261025_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String master = supplied.replace("databaseName=halotek_demo;", "databaseName=master;");
    String url = supplied.replace("databaseName=halotek_demo;", "databaseName=" + name + ";");
    try (Connection c = DriverManager.getConnection(master, properties);
        Statement s = c.createStatement()) {
      s.execute("CREATE DATABASE " + name);
    }
    // Only the test-created database is touched; no original or pre-existing data is loaded.
    try {
      try (Connection c = DriverManager.getConnection(url, properties)) {
        DatabaseSetup.install(c, "qa-admin", Fixture.PASSWORD.toCharArray());
      }
      AppService service = new AppService(() -> DriverManager.getConnection(url, properties));
      assertNotNull(service.login("qa-admin", Fixture.PASSWORD.toCharArray()));
      service.save(
          Entity.KATEGORI,
          Map.of(
              "id_kategori",
              "KTG_QA",
              "nama_kategori",
              "Kategori sintetis",
              "keterangan",
              "Untuk QA"),
          false);
      service.save(
          Entity.SUPPLIER,
          Map.of(
              "id_supplier",
              "SUP_QA",
              "nama_supplier",
              "Supplier sintetis",
              "telp_supplier",
              "0000000000",
              "alamat",
              "Alamat QA",
              "status_supplier",
              "Aktif"),
          false);
      service.save(
          Entity.CUSTOMER,
          Map.of(
              "id_customer",
              "CUS_QA",
              "nama_customer",
              "Customer O'Neil sintetis",
              "telp_customer",
              "0000000000",
              "status_customer",
              "Aktif"),
          false);
      Map<String, String> medicine = new LinkedHashMap<>();
      medicine.put("id_obat", "OBT_QA");
      medicine.put("nama_obat", "Obat sintetis");
      medicine.put("id_kategori", "KTG_QA");
      medicine.put("kandungan", "Bahan sintetis");
      medicine.put("jenis", "Tablet");
      medicine.put("dosis", "Demo");
      medicine.put("exp_date", "2099-01-01");
      medicine.put("stok", "5");
      medicine.put("harga", "1250.50");
      service.save(Entity.OBAT, medicine, false);
      for (Role role : List.of(Role.KASIR, Role.KEPALA))
        service.save(
            Entity.USER,
            Map.of(
                "id_user",
                "USR_" + role,
                "nama_user",
                "Akun QA",
                "username",
                "qa-" + role.label().toLowerCase(Locale.ROOT),
                "password",
                Fixture.PASSWORD,
                "jabatan",
                role.label(),
                "telp_user",
                "0000000000",
                "status_user",
                "Aktif"),
            false);
      assertEquals(1, service.list(Entity.CUSTOMER, "O'Neil").size());
      assertTrue(service.list(Entity.CUSTOMER, "' OR 1=1 --").isEmpty());
      Cart cart = new Cart();
      cart.add(new Cart.Line("OBT_QA", "Obat sintetis", new BigDecimal("1250.50"), 2));
      service.transact(true, "SUP_QA", LocalDate.now(), cart);
      assertEquals(7, ((Number) service.list(Entity.OBAT, "OBT_QA").get(0).get("stok")).intValue());
      assertNotNull(service.login("qa-kasir", Fixture.PASSWORD.toCharArray()));
      service.transact(false, null, LocalDate.now(), cart);
      assertThrows(
          SQLException.class, () -> service.transact(false, "RSP_MISSING", LocalDate.now(), cart));
      assertEquals(5, ((Number) service.list(Entity.OBAT, "OBT_QA").get(0).get("stok")).intValue());
      service.login("qa-kepala", Fixture.PASSWORD.toCharArray());
      assertEquals(
          1,
          service.report(true, LocalDate.now().getYear(), LocalDate.now().getMonthValue()).size());
      assertEquals(
          1,
          service.report(false, LocalDate.now().getYear(), LocalDate.now().getMonthValue()).size());
      System.out.println(
          "SQL Server synthetic schema/auth/CRUD/purchase/sale/rollback/report verified in temporary QA database.");
    } finally {
      try (Connection c = DriverManager.getConnection(master, properties);
          Statement s = c.createStatement()) {
        if (!name.matches("halotek_qa_20261025_[a-f0-9]{8}"))
          throw new IllegalStateException("Unexpected QA database name.");
        s.execute("ALTER DATABASE " + name + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
        s.execute("DROP DATABASE " + name);
      }
    }
  }
}
