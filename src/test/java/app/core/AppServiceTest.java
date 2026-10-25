package app.core;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AppServiceTest {
  private Fixture db;

  @BeforeEach
  void setup() throws Exception {
    db = new Fixture();
  }

  private Cart cart(int quantity) {
    Cart c = new Cart();
    c.add(new Cart.Line("OBT_TEST", "Obat sintetis", new BigDecimal("1250.50"), quantity));
    return c;
  }

  @Test
  void loginRejectsInjectionWrongPasswordInactiveAndClearsPasswordArray() throws Exception {
    char[] password = Fixture.PASSWORD.toCharArray();
    assertNotNull(db.service.login("test-admin", password));
    for (char c : password) assertEquals('\0', c);
    assertNull(db.service.login("' OR 1=1 --", Fixture.PASSWORD.toCharArray()));
    assertNull(db.service.session());
    assertNull(db.service.login("test-admin", "wrong".toCharArray()));
    db.execute("UPDATE mUser SET status_user='Tidak Aktif' WHERE username='test-admin'");
    assertNull(db.service.login("test-admin", Fixture.PASSWORD.toCharArray()));
  }

  @Test
  void customerCrudSearchAndSoftDeleteUseLiteralParameters() throws Exception {
    Map<String, String> values =
        new LinkedHashMap<>(
            Map.of(
                "id_customer",
                "CUS_NEW",
                "nama_customer",
                "O'Neil demo",
                "telp_customer",
                "0000000000",
                "status_customer",
                "Aktif"));
    db.service.save(Entity.CUSTOMER, values, false);
    assertEquals(1, db.service.list(Entity.CUSTOMER, "O'Neil").size());
    assertTrue(db.service.list(Entity.CUSTOMER, "' OR 1=1 --").isEmpty());
    values.put("nama_customer", "Nama berubah");
    db.service.save(Entity.CUSTOMER, values, true);
    assertEquals(1, db.service.list(Entity.CUSTOMER, "berubah").size());
    db.service.delete(Entity.CUSTOMER, "CUS_NEW");
    assertEquals(
        "Tidak Aktif", db.service.list(Entity.CUSTOMER, "CUS_NEW").get(0).get("status_customer"));
  }

  @Test
  void userPasswordIsNeverReturnedAndBlankUpdateKeepsHash() throws Exception {
    Map<String, String> user =
        new LinkedHashMap<>(
            Map.of(
                "id_user",
                "USR_NEW",
                "nama_user",
                "Demo user",
                "username",
                "new-user",
                "password",
                Fixture.PASSWORD,
                "jabatan",
                "Kasir",
                "telp_user",
                "0000000000",
                "status_user",
                "Aktif"));
    db.service.save(Entity.USER, user, false);
    assertFalse(db.service.list(Entity.USER, "USR_NEW").get(0).containsKey("password"));
    user.put("password", "");
    db.service.save(Entity.USER, user, true);
    assertNotNull(db.service.login("new-user", Fixture.PASSWORD.toCharArray()));
  }

  @Test
  void permissionsAreEnforcedInServiceAndLogoutRevokesAccess() throws Exception {
    db.login(Role.KASIR);
    assertThrows(SecurityException.class, () -> db.service.list(Entity.USER, ""));
    assertThrows(
        SecurityException.class,
        () -> db.service.transact(true, "SUP_TEST", LocalDate.now(), cart(1)));
    db.login(Role.KEPALA);
    assertThrows(SecurityException.class, () -> db.service.list(Entity.OBAT, ""));
    assertThrows(
        SecurityException.class, () -> db.service.transact(false, null, LocalDate.now(), cart(1)));
    db.service.logout();
    assertThrows(SecurityException.class, () -> db.service.report(false, 2026, 1));
  }

  @Test
  void adminCannotDeleteOrDemoteOwnActiveAccount() {
    assertThrows(
        IllegalArgumentException.class, () -> db.service.delete(Entity.USER, "USR_ADMIN_DEMO"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            db.service.save(
                Entity.USER,
                Map.of(
                    "id_user",
                    "USR_ADMIN_DEMO",
                    "nama_user",
                    "Demo",
                    "username",
                    "test-admin",
                    "password",
                    "",
                    "jabatan",
                    "Kasir",
                    "telp_user",
                    "0000000000",
                    "status_user",
                    "Aktif"),
                true));
  }

  @Test
  void purchaseUpdatesStockAndDetailsAndMonthlyReport() throws Exception {
    String id = db.service.transact(true, "SUP_TEST", LocalDate.of(2026, 10, 1), cart(2));
    assertTrue(id.startsWith("TRB"));
    assertEquals(22, db.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    assertEquals(1, db.integer("SELECT COUNT(*) FROM tbDetailPembelian"));
    db.login(Role.KEPALA);
    var rows = db.service.report(true, 2026, 10);
    assertEquals(1, rows.size());
    assertEquals(new BigDecimal("2501.00"), rows.get(0).get("total_harga_beli"));
    assertTrue(db.service.report(true, 2026, 9).isEmpty());
  }

  @Test
  void saleWithOrWithoutPrescriptionDecrementsStock() throws Exception {
    db.login(Role.KASIR);
    db.service.transact(false, "RSP_TEST", LocalDate.of(2026, 10, 2), cart(3));
    db.service.transact(false, null, LocalDate.of(2026, 10, 3), cart(1));
    assertEquals(16, db.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    db.login(Role.KEPALA);
    assertEquals(2, db.service.report(false, 2026, 10).size());
  }

  @Test
  void insufficientStockAndForeignKeyFailureRollbackEntireTransaction() throws Exception {
    db.login(Role.KASIR);
    assertThrows(
        IllegalArgumentException.class,
        () -> db.service.transact(false, null, LocalDate.now(), cart(21)));
    assertEquals(20, db.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    assertThrows(
        java.sql.SQLException.class,
        () -> db.service.transact(false, "MISSING_RECIPE", LocalDate.now(), cart(2)));
    assertEquals(20, db.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    assertEquals(0, db.integer("SELECT COUNT(*) FROM tbTrPenjualan"));
  }

  @Test
  void expiredMedicineAndInactiveSupplierAreRejected() throws Exception {
    db.execute("UPDATE mSupplier SET status_supplier='Tidak Aktif'");
    assertThrows(
        IllegalArgumentException.class,
        () -> db.service.transact(true, "SUP_TEST", LocalDate.now(), cart(2)));
    db.execute("UPDATE mObat SET exp_date='2000-01-01'");
    db.login(Role.KASIR);
    assertThrows(
        IllegalArgumentException.class,
        () -> db.service.transact(false, null, LocalDate.now(), cart(1)));
    assertEquals(20, db.integer("SELECT stok FROM mObat"));
  }

  @Test
  void referencedMedicineCannotBeDeletedAndTransactionRemains() throws Exception {
    db.service.transact(true, "SUP_TEST", LocalDate.now(), cart(1));
    assertThrows(java.sql.SQLException.class, () -> db.service.delete(Entity.OBAT, "OBT_TEST"));
    assertEquals(1, db.integer("SELECT COUNT(*) FROM tbTrPembelian"));
  }
}
