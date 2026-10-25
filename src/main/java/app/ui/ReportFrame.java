package app.ui;

import app.core.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportFrame extends BaseFrame {
  private final boolean purchase;
  private final JSpinner month =
      new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
  private final JSpinner year =
      new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2200, 1));
  private final JLabel total = new JLabel("Total: Rp 0");
  private final DefaultTableModel model =
      new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int r, int c) {
          return false;
        }
      };
  private List<Map<String, Object>> rows = List.of();
  private final String[] fields;
  private int loadedYear, loadedMonth;
  private final JButton print;

  public ReportFrame(AppService service, boolean purchase) {
    super(
        service,
        purchase ? "Laporan pembelian" : "Laporan penjualan",
        "Filter per bulan. Preview cetak memakai data yang sama dengan tabel.");
    this.purchase = purchase;
    service.requireRole(Role.KEPALA);
    fields =
        purchase
            ? new String[] {"id_pembelian", "nama_supplier", "tgl_pembelian", "total_harga_beli"}
            : new String[] {
              "id_penjualan", "nama_obat", "jumlah_obat", "tgl_penjualan", "jumlah_harga"
            };
    for (String field : fields) model.addColumn(field);
    JTable table = new JTable(model);
    table.setName("report");
    table.setRowHeight(28);
    table.setAutoCreateRowSorter(true);
    body.add(new JScrollPane(table));
    month.setName("month");
    year.setName("year");
    year.setEditor(new JSpinner.NumberEditor(year, "#"));
    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel m = new JLabel("Bulan");
    m.setLabelFor(month);
    JLabel y = new JLabel("Tahun");
    y.setLabelFor(year);
    controls.add(m);
    controls.add(month);
    controls.add(y);
    controls.add(year);
    controls.add(Ui.button("Tampilkan", this::refresh));
    print = Ui.button("Preview cetak / PDF", this::preview);
    print.setEnabled(false);
    controls.add(print);
    controls.add(Ui.button("Menu utama", () -> Ui.home(service, this)));
    body.add(controls, BorderLayout.NORTH);
    body.add(total, BorderLayout.SOUTH);
    refresh();
  }

  public void refresh() {
    int selectedYear = ((Number) year.getValue()).intValue(),
        selectedMonth = ((Number) month.getValue()).intValue();
    task(
        "Memuat laporan...",
        () -> service.report(purchase, selectedYear, selectedMonth),
        data -> {
          rows = data;
          loadedYear = selectedYear;
          loadedMonth = selectedMonth;
          model.setRowCount(0);
          BigDecimal sum = BigDecimal.ZERO;
          for (Map<String, Object> row : rows) {
            model.addRow(java.util.Arrays.stream(fields).map(row::get).toArray());
            sum = sum.add((BigDecimal) row.get(fields[fields.length - 1]));
          }
          total.setText("Total: Rp " + sum.toPlainString());
          print.setEnabled(!rows.isEmpty());
          status.setText(
              rows.isEmpty()
                  ? "Tidak ada transaksi pada bulan tersebut."
                  : rows.size() + " baris untuk " + loadedMonth + "/" + loadedYear + ".");
        });
  }

  private void preview() {
    service.requireRole(Role.KEPALA);
    List<Map<String, Object>> snapshot = List.copyOf(rows);
    task(
        "Menyiapkan preview laporan...",
        () -> Reports.create(purchase, loadedYear, loadedMonth, snapshot),
        report -> {
          net.sf.jasperreports.view.JasperViewer viewer =
              new net.sf.jasperreports.view.JasperViewer(report, false);
          viewer.setTitle(getTitle());
          viewer.setVisible(true);
          status.setText("Preview siap. Pilih cetak atau ekspor dari toolbar JasperReports.");
        });
  }
}
