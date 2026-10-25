package app.core;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public final class AppService {
  @FunctionalInterface
  public interface Connections {
    Connection open() throws SQLException;
  }

  public record Option(String id, String name) {
    @Override
    public String toString() {
      return id + " - " + name;
    }
  }

  private final Connections connections;
  private volatile Session session;

  public AppService(Connections connections) {
    this.connections = Objects.requireNonNull(connections);
  }

  public Session session() {
    return session;
  }

  public void logout() {
    session = null;
  }

  public Session login(String username, char[] password) throws SQLException {
    logout();
    try {
      if (username == null || !username.matches("[A-Za-z0-9_.-]{3,40}") || password.length > 128)
        return null;
      try (Connection c = connections.open();
          PreparedStatement p =
              c.prepareStatement(
                  "SELECT id_user,nama_user,password,jabatan FROM mUser WHERE username=? AND status_user=?")) {
        p.setString(1, username);
        p.setString(2, "Aktif");
        try (ResultSet r = p.executeQuery()) {
          if (!r.next() || !Passwords.verify(password, r.getString("password"))) return null;
          session =
              new Session(
                  r.getString("id_user"),
                  r.getString("nama_user"),
                  Role.parse(r.getString("jabatan")));
          return session;
        }
      }
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  public void requireRole(Role... allowed) {
    Session current = session;
    if (current == null || Arrays.stream(allowed).noneMatch(r -> r == current.role()))
      throw new SecurityException("Sesi tidak memiliki akses. Silakan masuk kembali.");
  }

  public List<Map<String, Object>> list(Entity entity, String term) throws SQLException {
    entity.requireAccess(session, false);
    String fields =
        entity.visibleFields().stream().map(Entity.Field::column).collect(Collectors.joining(","));
    String query =
        "SELECT "
            + fields
            + " FROM "
            + entity.table
            + " WHERE "
            + entity.idColumn()
            + " LIKE ? OR "
            + entity.nameColumn()
            + " LIKE ? ORDER BY "
            + entity.idColumn();
    try (Connection c = connections.open();
        PreparedStatement p = c.prepareStatement(query)) {
      p.setMaxRows(500);
      String search = "%" + Objects.toString(term, "").trim() + "%";
      p.setString(1, search);
      p.setString(2, search);
      try (ResultSet r = p.executeQuery()) {
        return rows(r);
      }
    }
  }

  public List<Option> options(Entity entity) throws SQLException {
    requireRole(Role.ADMIN, Role.KASIR);
    if (session.role() == Role.KASIR
        && !(entity == Entity.CUSTOMER
            || entity == Entity.KATEGORI
            || entity == Entity.OBAT
            || entity == Entity.RESEP)) throw new SecurityException("Pilihan tidak tersedia.");
    String active =
        (entity == Entity.CUSTOMER || entity == Entity.SUPPLIER)
            ? " WHERE status_" + entity.name().toLowerCase(Locale.ROOT) + "=?"
            : "";
    try (Connection c = connections.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT "
                    + entity.idColumn()
                    + ","
                    + entity.nameColumn()
                    + " FROM "
                    + entity.table
                    + active
                    + " ORDER BY "
                    + entity.nameColumn())) {
      if (!active.isEmpty()) p.setString(1, "Aktif");
      try (ResultSet r = p.executeQuery()) {
        List<Option> values = new ArrayList<>();
        while (r.next()) values.add(new Option(r.getString(1), r.getString(2)));
        return values;
      }
    }
  }

  public void save(Entity entity, Map<String, String> input, boolean update) throws SQLException {
    entity.requireAccess(session, true);
    Map<String, Object> values = entity.validate(input, update);
    String id = values.get(entity.idColumn()).toString();
    if (entity == Entity.USER
        && id.equals(session.id())
        && (!values.get("jabatan").equals("Admin") || !values.get("status_user").equals("Aktif"))) {
      throw new IllegalArgumentException("Akun aktif Anda harus tetap Admin dan Aktif.");
    }
    List<String> columns = new ArrayList<>(values.keySet());
    String sql;
    if (update) {
      columns.remove(entity.idColumn());
      sql =
          "UPDATE "
              + entity.table
              + " SET "
              + columns.stream().map(x -> x + "=?").collect(Collectors.joining(","))
              + " WHERE "
              + entity.idColumn()
              + "=?";
    } else {
      sql =
          "INSERT INTO "
              + entity.table
              + " ("
              + String.join(",", columns)
              + ") VALUES ("
              + columns.stream().map(x -> "?").collect(Collectors.joining(","))
              + ")";
    }
    try (Connection c = connections.open();
        PreparedStatement p = c.prepareStatement(sql)) {
      int n = 1;
      for (String column : columns) p.setObject(n++, values.get(column));
      if (update) p.setString(n, id);
      if (p.executeUpdate() != 1) throw new SQLException("Record changed or missing.");
    }
  }

  public void delete(Entity entity, String id) throws SQLException {
    entity.requireAccess(session, true);
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,32}"))
      throw new IllegalArgumentException("Pilih data yang akan dihapus.");
    if (entity == Entity.USER && id.equals(session.id()))
      throw new IllegalArgumentException("Akun yang sedang dipakai tidak dapat dihapus.");
    boolean soft = entity == Entity.CUSTOMER || entity == Entity.SUPPLIER;
    String sql =
        soft
            ? "UPDATE "
                + entity.table
                + " SET status_"
                + entity.name().toLowerCase(Locale.ROOT)
                + "=? WHERE "
                + entity.idColumn()
                + "=?"
            : "DELETE FROM " + entity.table + " WHERE " + entity.idColumn() + "=?";
    try (Connection c = connections.open();
        PreparedStatement p = c.prepareStatement(sql)) {
      int n = 1;
      if (soft) p.setString(n++, "Tidak Aktif");
      p.setString(n, id);
      if (p.executeUpdate() != 1)
        throw new IllegalArgumentException("Data tidak ditemukan. Muat ulang daftar.");
    }
  }

  public List<Map<String, Object>> medicines() throws SQLException {
    return list(Entity.OBAT, "");
  }

  public String transact(boolean purchase, String partnerId, LocalDate date, Cart cart)
      throws SQLException {
    requireRole(purchase ? Role.ADMIN : Role.KASIR);
    if (date == null || cart == null || cart.lines().isEmpty())
      throw new IllegalArgumentException("Tanggal dan minimal satu obat wajib diisi.");
    if (date.getYear() < 2000 || date.isAfter(LocalDate.now()))
      throw new IllegalArgumentException(
          "Tanggal transaksi tidak boleh di masa depan atau sebelum tahun 2000.");
    if (partnerId != null && !partnerId.matches("[A-Za-z0-9_-]{1,32}"))
      throw new IllegalArgumentException("Pilihan supplier/resep tidak valid.");
    if (purchase && partnerId == null) throw new IllegalArgumentException("Pilih supplier aktif.");
    String id =
        (purchase ? "TRB" : "TRJ")
            + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    String suffix = purchase ? "Pembelian" : "Penjualan";
    String idColumn = purchase ? "id_pembelian" : "id_penjualan";
    try (Connection c = connections.open()) {
      c.setAutoCommit(false);
      try {
        if (purchase && !activeSupplier(c, partnerId))
          throw new IllegalArgumentException("Supplier tidak aktif atau tidak ditemukan.");
        BigDecimal total = BigDecimal.ZERO;
        List<Cart.Line> actual = new ArrayList<>();
        // Lock/update medicine rows in the same order for concurrent transactions.
        for (Cart.Line line :
            cart.lines().stream().sorted(Comparator.comparing(Cart.Line::medicineId)).toList()) {
          BigDecimal price;
          try (PreparedStatement p =
              c.prepareStatement("SELECT harga,exp_date FROM mObat WHERE id_obat=?")) {
            p.setString(1, line.medicineId());
            try (ResultSet r = p.executeQuery()) {
              if (!r.next())
                throw new IllegalArgumentException("Obat tidak ditemukan: " + line.medicineId());
              price = purchase ? line.price() : r.getBigDecimal("harga");
              if (!purchase && r.getDate("exp_date").toLocalDate().isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Obat kedaluwarsa tidak dapat dijual.");
              if (!purchase && price.compareTo(line.price()) != 0)
                throw new IllegalArgumentException(
                    "Harga berubah. Kosongkan rincian dan muat ulang obat.");
            }
          }
          String sql =
              purchase
                  ? "UPDATE mObat SET stok=stok+? WHERE id_obat=? AND stok<=?"
                  : "UPDATE mObat SET stok=stok-? WHERE id_obat=? AND stok>=?";
          try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, line.quantity());
            p.setString(2, line.medicineId());
            p.setInt(3, purchase ? 1_000_000 - line.quantity() : line.quantity());
            if (p.executeUpdate() != 1)
              throw new IllegalArgumentException(
                  purchase
                      ? "Batas stok terlampaui."
                      : "Stok tidak mencukupi. Muat ulang data obat.");
          }
          Cart.Line accepted =
              new Cart.Line(line.medicineId(), line.name(), price, line.quantity());
          actual.add(accepted);
          total = total.add(accepted.total());
        }
        String partnerColumn = purchase ? "id_supplier" : "id_resep";
        String totalColumn = purchase ? "total_harga_beli" : "total_harga_jual";
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO tbTr"
                    + suffix
                    + " ("
                    + idColumn
                    + ","
                    + partnerColumn
                    + ",tgl_"
                    + suffix.toLowerCase(Locale.ROOT)
                    + ","
                    + totalColumn
                    + ") VALUES (?,?,?,?)")) {
          p.setString(1, id);
          p.setString(2, partnerId);
          p.setDate(3, java.sql.Date.valueOf(date));
          p.setBigDecimal(4, total);
          p.executeUpdate();
        }
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO tbDetail"
                    + suffix
                    + " ("
                    + idColumn
                    + ",id_obat,jumlah_obat,jumlah_harga) VALUES (?,?,?,?)")) {
          for (Cart.Line line : actual) {
            p.setString(1, id);
            p.setString(2, line.medicineId());
            p.setInt(3, line.quantity());
            p.setBigDecimal(4, line.total());
            p.addBatch();
          }
          p.executeBatch();
        }
        c.commit();
        return id;
      } catch (SQLException | RuntimeException ex) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          ex.addSuppressed(rollback);
        }
        throw ex;
      }
    }
  }

  private boolean activeSupplier(Connection c, String id) throws SQLException {
    try (PreparedStatement p =
        c.prepareStatement(
            "SELECT id_supplier FROM mSupplier WHERE id_supplier=? AND status_supplier=?")) {
      p.setString(1, id);
      p.setString(2, "Aktif");
      try (ResultSet r = p.executeQuery()) {
        return r.next();
      }
    }
  }

  public List<Map<String, Object>> report(boolean purchase, int year, int month)
      throws SQLException {
    requireRole(Role.KEPALA);
    if (year < 2000 || year > 2200 || month < 1 || month > 12)
      throw new IllegalArgumentException("Pilih tahun 2000-2200 dan bulan 1-12.");
    LocalDate start = LocalDate.of(year, month, 1);
    String sql =
        purchase
            ? "SELECT t.id_pembelian,s.nama_supplier,t.tgl_pembelian,t.total_harga_beli FROM tbTrPembelian t JOIN mSupplier s ON s.id_supplier=t.id_supplier WHERE t.tgl_pembelian>=? AND t.tgl_pembelian<? ORDER BY t.tgl_pembelian,t.id_pembelian"
            : "SELECT t.id_penjualan,o.nama_obat,d.jumlah_obat,t.tgl_penjualan,d.jumlah_harga FROM tbTrPenjualan t JOIN tbDetailPenjualan d ON d.id_penjualan=t.id_penjualan JOIN mObat o ON o.id_obat=d.id_obat WHERE t.tgl_penjualan>=? AND t.tgl_penjualan<? ORDER BY t.tgl_penjualan,t.id_penjualan,o.id_obat";
    try (Connection c = connections.open();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setDate(1, java.sql.Date.valueOf(start));
      p.setDate(2, java.sql.Date.valueOf(start.plusMonths(1)));
      try (ResultSet r = p.executeQuery()) {
        return rows(r);
      }
    }
  }

  private static List<Map<String, Object>> rows(ResultSet r) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    ResultSetMetaData metadata = r.getMetaData();
    while (r.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int n = 1; n <= metadata.getColumnCount(); n++)
        row.put(metadata.getColumnLabel(n).toLowerCase(Locale.ROOT), r.getObject(n));
      rows.add(row);
    }
    return rows;
  }
}
