package app.ui;

import app.core.*;
import javax.swing.*;
import java.awt.*;

public class Dashboard extends BaseFrame {
  public Dashboard(AppService service, Role role) {
    super(service, "Menu " + role.label(), "HaloTek / " + role.label());
    service.requireRole(role);
    JPanel menu = new JPanel(new GridLayout(0, 2, 14, 14));
    menu.setBorder(BorderFactory.createEmptyBorder(30, 45, 30, 45));
    if (role == Role.ADMIN) {
      add(menu, "User", () -> new Master.User(service));
      add(menu, "Customer", () -> new Master.Customer(service));
      add(menu, "Supplier", () -> new Master.Supplier(service));
      add(menu, "Kategori", () -> new Master.Kategori(service));
      add(menu, "Obat", () -> new Master.Obat(service));
      add(menu, "Resep", () -> new Master.Resep(service));
      add(menu, "Pembelian", () -> new Transaksi.TransaksiPembelian(service));
    } else if (role == Role.KASIR) {
      add(menu, "Obat", () -> new Master.Obat1(service));
      add(menu, "Resep", () -> new Master.Resep1(service));
      add(menu, "Customer", () -> new Master.Customer1(service));
      add(menu, "Penjualan", () -> new Transaksi.TransaksiPenjualan(service));
    } else {
      add(menu, "Laporan pembelian", () -> new Laporan.LaporanPembelian(service));
      add(menu, "Laporan penjualan", () -> new Laporan.LaporanPenjualan(service));
    }
    body.add(menu);
    JButton logout =
        Ui.button(
            "Keluar akun",
            () -> {
              service.logout();
              new HaloTek.LoginForm(service).setVisible(true);
              dispose();
            });
    body.add(logout, BorderLayout.SOUTH);
    status.setText("Masuk sebagai " + service.session().name() + " / " + role.label());
  }

  private void add(JPanel menu, String label, java.util.function.Supplier<JFrame> screen) {
    JButton button =
        Ui.button(
            label,
            () -> {
              try {
                JFrame next = screen.get();
                next.setVisible(true);
                dispose();
              } catch (RuntimeException ex) {
                Ui.error(this, ex);
              }
            });
    button.setFont(button.getFont().deriveFont(Font.BOLD, 18));
    menu.add(button);
  }
}
