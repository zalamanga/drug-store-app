package app.ui;

import app.core.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class TradeFrame extends BaseFrame {
  private final boolean purchase;
  private final Cart cart = new Cart();
  private final JComboBox<AppService.Option> medicine = new JComboBox<>(),
      partner = new JComboBox<>();
  private final JTextField quantity = new JTextField("1", 6),
      price = new JTextField(10),
      date = new JTextField(LocalDate.now().toString(), 12);
  private final JLabel total = new JLabel("Total: Rp 0");
  private final DefaultTableModel cartModel =
      model("ID Obat", "Nama obat", "Harga satuan", "Jumlah", "Total");
  private final JTable cartTable = new JTable(cartModel),
      medicines = new JTable(),
      partners = new JTable();
  private List<Map<String, Object>> medicineRows = List.of();

  private record Loaded(
      List<Map<String, Object>> medicines,
      List<Map<String, Object>> partners,
      List<AppService.Option> choices) {}

  public TradeFrame(AppService service, boolean purchase) {
    super(
        service,
        purchase ? "Pembelian obat" : "Penjualan obat",
        "Tambah obat ke rincian, periksa jumlah, lalu simpan transaksi.");
    this.purchase = purchase;
    service.requireRole(purchase ? Role.ADMIN : Role.KASIR);
    JPanel fields = new JPanel(new GridLayout(0, 3, 12, 8));
    medicine.setName("medicine");
    partner.setName("partner");
    quantity.setName("quantity");
    price.setName("price");
    date.setName("date");
    field(fields, purchase ? "Supplier" : "Resep (opsional)", partner);
    field(fields, "Tanggal (YYYY-MM-DD)", date);
    field(fields, "Obat", medicine);
    field(fields, "Harga satuan (rupiah)", price);
    field(fields, "Jumlah", quantity);
    price.setEditable(purchase);
    medicine.addActionListener(e -> setPrice());
    JPanel actions = new JPanel(new GridLayout(0, 4, 8, 8));
    actions.add(Ui.button("Tambah obat", this::add));
    actions.add(Ui.button("Hapus baris", this::remove));
    actions.add(
        Ui.button(
            "Kosongkan rincian",
            () -> {
              cart.clear();
              renderCart();
            }));
    actions.add(Ui.button("Muat ulang", this::refresh));
    if (!purchase) {
      actions.add(
          Ui.button("Tambah customer", () -> new Master.InputCustomer(service).setVisible(true)));
      actions.add(Ui.button("Tambah resep", () -> new Master.InputResep(service).setVisible(true)));
    }
    JPanel top = new JPanel(new BorderLayout());
    top.add(fields);
    top.add(actions, BorderLayout.SOUTH);
    cartTable.setName("cart");
    cartTable.setRowHeight(28);
    cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JPanel cartPanel = new JPanel(new BorderLayout());
    cartPanel.add(new JScrollPane(cartTable));
    cartPanel.add(total, BorderLayout.SOUTH);
    JTabbedPane context = new JTabbedPane();
    context.addTab("Data obat", searchable(medicines));
    context.addTab(purchase ? "Data supplier" : "Data resep", searchable(partners));
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cartPanel, context);
    split.setResizeWeight(.48);
    cartPanel.setMinimumSize(new Dimension(200, 100));
    context.setMinimumSize(new Dimension(200, 130));
    SwingUtilities.invokeLater(() -> split.setDividerLocation(.45));
    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottom.add(Ui.button("Menu utama", () -> Ui.home(service, this)));
    JButton save = Ui.button("Simpan transaksi", this::commit);
    Ui.primary(save);
    bottom.add(save);
    body.add(top, BorderLayout.NORTH);
    body.add(split);
    body.add(bottom, BorderLayout.SOUTH);
    refresh();
  }

  private static void field(JPanel panel, String text, JComponent input) {
    JPanel cell = new JPanel(new BorderLayout(0, 4));
    JLabel label = new JLabel(text);
    label.setLabelFor(input);
    input.getAccessibleContext().setAccessibleName(text);
    cell.add(label, BorderLayout.NORTH);
    cell.add(input);
    panel.add(cell);
  }

  private JPanel searchable(JTable table) {
    table.setRowHeight(26);
    table.setAutoCreateRowSorter(true);
    table.setFillsViewportHeight(true);
    JPanel panel = new JPanel(new BorderLayout());
    JTextField term = new JTextField(20);
    JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel label = new JLabel("Cari pada tabel");
    label.setLabelFor(term);
    tools.add(label);
    tools.add(term);
    JButton filter =
        Ui.button(
            "Filter",
            () -> {
              if (table.getRowSorter() instanceof TableRowSorter<?> sorter)
                sorter.setRowFilter(
                    term.getText().isBlank()
                        ? null
                        : RowFilter.regexFilter(
                            "(?i)" + java.util.regex.Pattern.quote(term.getText())));
            });
    term.addActionListener(e -> filter.doClick());
    tools.add(filter);
    if (table == medicines)
      tools.add(
          Ui.button(
              "Urut stok",
              () ->
                  table
                      .getRowSorter()
                      .setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.ASCENDING)))));
    panel.add(tools, BorderLayout.NORTH);
    panel.add(new JScrollPane(table));
    return panel;
  }

  private static DefaultTableModel model(String... labels) {
    return new DefaultTableModel(labels, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };
  }

  public void refresh() {
    task(
        "Memuat obat dan " + (purchase ? "supplier" : "resep") + "...",
        () ->
            new Loaded(
                service.medicines(),
                service.list(purchase ? Entity.SUPPLIER : Entity.RESEP, ""),
                service.options(purchase ? Entity.SUPPLIER : Entity.RESEP)),
        loaded -> {
          medicineRows = loaded.medicines();
          medicine.removeAllItems();
          for (Map<String, Object> row : medicineRows)
            medicine.addItem(
                new AppService.Option(
                    row.get("id_obat").toString(), row.get("nama_obat").toString()));
          partner.removeAllItems();
          if (!purchase) partner.addItem(new AppService.Option("", "Tanpa resep"));
          loaded.choices().forEach(partner::addItem);
          DefaultTableModel m = model("ID", "Nama", "Kandungan", "Kedaluwarsa", "Stok", "Harga");
          for (Map<String, Object> row : medicineRows)
            m.addRow(
                new Object[] {
                  row.get("id_obat"),
                  row.get("nama_obat"),
                  row.get("kandungan"),
                  row.get("exp_date"),
                  row.get("stok"),
                  row.get("harga")
                });
          medicines.setModel(m);
          String[] columns =
              purchase
                  ? new String[] {
                    "id_supplier", "nama_supplier", "telp_supplier", "alamat", "status_supplier"
                  }
                  : new String[] {
                    "id_resep", "id_customer", "nama_dokter", "kandungan", "tgl_input"
                  };
          DefaultTableModel p = model(columns);
          for (Map<String, Object> row : loaded.partners())
            p.addRow(Arrays.stream(columns).map(row::get).toArray());
          partners.setModel(p);
          setPrice();
          status.setText(
              medicineRows.isEmpty()
                  ? "Belum ada obat. Admin perlu menambahkan kategori dan obat terlebih dahulu."
                  : "Data dimuat. Rincian yang belum disimpan tetap tersedia.");
        });
  }

  private Map<String, Object> selectedMedicine() {
    AppService.Option choice = (AppService.Option) medicine.getSelectedItem();
    return choice == null
        ? null
        : medicineRows.stream()
            .filter(x -> x.get("id_obat").equals(choice.id()))
            .findFirst()
            .orElse(null);
  }

  private void setPrice() {
    Map<String, Object> row = selectedMedicine();
    price.setText(row == null ? "" : row.get("harga").toString());
  }

  private void add() {
    try {
      Map<String, Object> row = selectedMedicine();
      if (row == null) throw new IllegalArgumentException("Pilih obat terlebih dahulu.");
      Cart.Line line =
          new Cart.Line(
              row.get("id_obat").toString(),
              row.get("nama_obat").toString(),
              new BigDecimal(price.getText().trim()),
              Integer.parseInt(quantity.getText().trim()));
      if (!purchase) {
        int existing =
            cart.lines().stream()
                .filter(x -> x.medicineId().equals(line.medicineId()))
                .mapToInt(Cart.Line::quantity)
                .sum();
        if (line.quantity() + existing > ((Number) row.get("stok")).intValue())
          throw new IllegalArgumentException("Jumlah melebihi stok yang ditampilkan.");
      }
      cart.add(line);
      renderCart();
      status.setText("Obat ditambahkan ke rincian. Stok belum berubah sebelum transaksi disimpan.");
    } catch (RuntimeException ex) {
      status.setText(
          ex instanceof NumberFormatException
              ? "Harga dan jumlah harus berupa angka yang valid."
              : ex.getMessage());
    }
  }

  private void remove() {
    int row = cartTable.getSelectedRow();
    if (row < 0) {
      status.setText("Pilih baris rincian yang akan dihapus.");
      return;
    }
    cart.remove(cart.lines().get(cartTable.convertRowIndexToModel(row)).medicineId());
    renderCart();
  }

  private void renderCart() {
    cartModel.setRowCount(0);
    for (Cart.Line line : cart.lines())
      cartModel.addRow(
          new Object[] {
            line.medicineId(), line.name(), line.price(), line.quantity(), line.total()
          });
    total.setText("Total: Rp " + cart.total().toPlainString());
  }

  private void commit() {
    LocalDate transactionDate;
    try {
      transactionDate = LocalDate.parse(date.getText().trim());
    } catch (RuntimeException ex) {
      status.setText("Tanggal harus memakai format YYYY-MM-DD.");
      return;
    }
    if (cart.lines().isEmpty()) {
      status.setText("Tambahkan minimal satu obat ke rincian.");
      return;
    }
    AppService.Option choice = (AppService.Option) partner.getSelectedItem();
    String partnerId = choice == null || choice.id().isEmpty() ? null : choice.id();
    Cart snapshot = new Cart();
    cart.lines().forEach(snapshot::add);
    task(
        "Menyimpan transaksi dan stok...",
        () -> service.transact(purchase, partnerId, transactionDate, snapshot),
        id -> {
          cart.clear();
          renderCart();
          status.setText("Transaksi " + id + " tersimpan.");
          refresh();
        });
  }
}
