package app.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public enum Entity {
  CUSTOMER(
      "Customer",
      "mCustomer",
      "CUS",
      new Field("id_customer", "ID Customer", "id"),
      new Field("nama_customer", "Nama lengkap", "text"),
      new Field("telp_customer", "Telepon", "phone"),
      new Field("status_customer", "Status", "status")),
  SUPPLIER(
      "Supplier",
      "mSupplier",
      "SUP",
      new Field("id_supplier", "ID Supplier", "id"),
      new Field("nama_supplier", "Nama supplier", "text"),
      new Field("telp_supplier", "Telepon", "phone"),
      new Field("alamat", "Alamat", "text"),
      new Field("status_supplier", "Status", "status")),
  KATEGORI(
      "Kategori",
      "mKategori",
      "KTG",
      new Field("id_kategori", "ID Kategori", "id"),
      new Field("nama_kategori", "Nama kategori", "text"),
      new Field("keterangan", "Keterangan", "text")),
  OBAT(
      "Obat",
      "mObat",
      "OBT",
      new Field("id_obat", "ID Obat", "id"),
      new Field("nama_obat", "Nama obat", "text"),
      new Field("id_kategori", "Kategori", "category"),
      new Field("kandungan", "Kandungan", "text"),
      new Field("jenis", "Jenis", "text"),
      new Field("dosis", "Dosis", "text"),
      new Field("exp_date", "Kedaluwarsa (YYYY-MM-DD)", "date"),
      new Field("stok", "Stok", "integer"),
      new Field("harga", "Harga (rupiah)", "money")),
  RESEP(
      "Resep",
      "mResep",
      "RSP",
      new Field("id_resep", "ID Resep", "id"),
      new Field("id_customer", "Customer", "customer"),
      new Field("nama_dokter", "Nama dokter", "text"),
      new Field("praktek", "Tempat praktek", "text"),
      new Field("kandungan", "Kandungan / instruksi", "text"),
      new Field("tgl_input", "Tanggal (YYYY-MM-DD)", "date")),
  USER(
      "User",
      "mUser",
      "USR",
      new Field("id_user", "ID User", "id"),
      new Field("nama_user", "Nama lengkap", "text"),
      new Field("username", "Username", "username"),
      new Field("password", "Password baru (12+ karakter)", "password"),
      new Field("jabatan", "Jabatan", "role"),
      new Field("telp_user", "Telepon", "phone"),
      new Field("status_user", "Status", "status"));

  public record Field(String column, String label, String type) {}

  public final String label, table, prefix;
  public final List<Field> fields;

  Entity(String label, String table, String prefix, Field... fields) {
    this.label = label;
    this.table = table;
    this.prefix = prefix;
    this.fields = List.of(fields);
  }

  public String idColumn() {
    return fields.get(0).column();
  }

  public String nameColumn() {
    return fields.get(1).column();
  }

  public List<Field> visibleFields() {
    return fields.stream().filter(f -> !f.type.equals("password")).toList();
  }

  public String newId() {
    return prefix
        + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
  }

  public void requireAccess(Session session, boolean write) {
    if (session == null
        || session.role() == Role.KEPALA
        || session.role() == Role.KASIR && !(this == CUSTOMER || this == OBAT || this == RESEP)) {
      throw new SecurityException("Sesi ini tidak berhak membuka " + label + ".");
    }
  }

  public Map<String, Object> validate(Map<String, String> input, boolean update) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Field f : fields) {
      String raw = input.getOrDefault(f.column, "");
      String value = f.type.equals("password") ? raw : raw.trim();
      if (f.type.equals("password") && update && value.isEmpty()) continue;
      if (value.isBlank() || value.length() > 250)
        throw new IllegalArgumentException(f.label + " wajib diisi, maksimal 250 karakter.");
      Object parsed = value;
      try {
        switch (f.type) {
          case "id", "category", "customer" -> {
            if (!value.matches("[A-Za-z0-9_-]{1,32}")) throw new IllegalArgumentException();
          }
          case "username" -> {
            if (!value.matches("[A-Za-z0-9_.-]{3,40}")) throw new IllegalArgumentException();
          }
          case "phone" -> {
            if (!value.matches("[+0-9 ()-]{5,30}")) throw new IllegalArgumentException();
          }
          case "integer" -> {
            int n = Integer.parseInt(value);
            if (n < 0 || n > 1_000_000) throw new IllegalArgumentException();
            parsed = n;
          }
          case "money" -> {
            BigDecimal n = new BigDecimal(value);
            if (n.signum() < 0 || n.scale() > 2 || n.compareTo(new BigDecimal("1000000000")) > 0)
              throw new IllegalArgumentException();
            parsed = n;
          }
          case "date" -> parsed = LocalDate.parse(value);
          case "status" -> {
            if (!Set.of("Aktif", "Tidak Aktif").contains(value))
              throw new IllegalArgumentException();
          }
          case "role" -> Role.parse(value);
          case "password" -> parsed = Passwords.hash(value.toCharArray());
          default -> {}
        }
      } catch (RuntimeException ex) {
        throw new IllegalArgumentException("Periksa " + f.label + ".");
      }
      result.put(f.column, parsed);
    }
    return result;
  }
}
