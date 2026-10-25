package app.ui;

import app.core.*;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

@Tag("desktop")
class DesktopTest {
  private Fixture fixture;

  @BeforeEach
  void setup() throws Exception {
    Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required.");
    fixture = new Fixture();
    edt(
        () -> {
          Ui.install();
          return null;
        });
  }

  @AfterEach
  void closeOwnTestWindows() throws Exception {
    edt(
        () -> {
          for (Window w : Window.getWindows()) w.dispose();
          return null;
        });
  }

  @Test
  void loginKeyboardDefaultButtonCustomerCrudAndRoleScreens() throws Exception {
    HaloTek.LoginForm login =
        edt(
            () -> {
              var f = new HaloTek.LoginForm(fixture.service);
              f.setVisible(true);
              return f;
            });
    screenshot(login, "01-login.png");
    edt(
        () -> {
          ((JTextField) find(login, "username")).setText("to-clear");
          ((JCheckBox) find(login, "showPassword")).doClick();
          assertEquals((char) 0, ((JPasswordField) find(login, "password")).getEchoChar());
          ((JCheckBox) find(login, "showPassword")).doClick();
          return null;
        });
    click(login, "Kosongkan");
    assertEquals("", edt(() -> ((JTextField) find(login, "username")).getText()));
    edt(
        () -> {
          ((JTextField) find(login, "username")).setText("test-admin");
          ((JPasswordField) find(login, "password")).setText(Fixture.PASSWORD);
          login.getRootPane().getDefaultButton().doClick();
          return null;
        });
    await(() -> fixture.service.session() != null);
    await(() -> windows(Admin.DashAdmin.class) > 0);
    Admin.DashAdmin dashboard =
        edt(
            () ->
                (Admin.DashAdmin)
                    java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof Admin.DashAdmin && w.isShowing())
                        .findFirst()
                        .orElseThrow());
    screenshot(dashboard, "02-admin.png");
    click(dashboard, "Customer");
    await(() -> windows(Master.Customer.class) > 0);
    Master.Customer customer =
        edt(
            () ->
                (Master.Customer)
                    java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof Master.Customer && w.isShowing())
                        .findFirst()
                        .orElseThrow());
    idle(customer);
    edt(
        () -> {
          ((JTextField) find(customer, "nama_customer")).setText("Customer UI sintetis");
          ((JTextField) find(customer, "telp_customer")).setText("0000000000");
          return null;
        });
    click(customer, "Simpan baru");
    idle(customer);
    assertEquals(2, fixture.service.list(Entity.CUSTOMER, "").size());
    edt(
        () -> {
          ((JTextField) find(customer, "search")).setText("UI sintetis");
          return null;
        });
    click(customer, "Cari");
    idle(customer);
    JTable records = edt(() -> (JTable) find(customer, "records"));
    assertEquals(1, edt(records::getRowCount));
    edt(
        () -> {
          records.setRowSelectionInterval(0, 0);
          ((JTextField) find(customer, "nama_customer")).setText("Customer UI diubah");
          return null;
        });
    click(customer, "Ubah");
    idle(customer);
    assertEquals(1, fixture.service.list(Entity.CUSTOMER, "diubah").size());
    edt(
        () -> {
          ((JTextField) find(customer, "search")).setText("diubah");
          return null;
        });
    click(customer, "Cari");
    idle(customer);
    edt(
        () -> {
          records.setRowSelectionInterval(0, 0);
          return null;
        });
    SwingUtilities.invokeLater(() -> ((JButton) find(customer, "Hapus")).doClick());
    await(
        () ->
            java.util.Arrays.stream(Window.getWindows())
                .anyMatch(w -> w instanceof JDialog && w.isShowing()));
    edt(
        () -> {
          for (Window w : Window.getWindows())
            if (w instanceof JDialog dialog && w.isShowing()) {
              JOptionPane pane = findPane(dialog);
              if (pane != null) pane.setValue(JOptionPane.OK_OPTION);
            }
          return null;
        });
    idle(customer);
    assertEquals(
        "Tidak Aktif",
        fixture.service.list(Entity.CUSTOMER, "diubah").get(0).get("status_customer"));
    click(customer, "Form baru");
    click(customer, "Muat ulang");
    idle(customer);
    edt(
        () -> {
          customer.setSize(800, 600);
          return null;
        });
    screenshot(customer, "03-customer-800.png");
    click(customer, "Menu utama");
    // Open retained master classes with a real service and populated synthetic DB.
    for (java.util.function.Supplier<JFrame> screen :
        java.util.List.<java.util.function.Supplier<JFrame>>of(
            () -> new Master.Supplier(fixture.service),
            () -> new Master.Kategori(fixture.service),
            () -> new Master.Obat(fixture.service),
            () -> new Master.Resep(fixture.service),
            () -> new Master.User(fixture.service))) {
      JFrame frame =
          edt(
              () -> {
                JFrame f = screen.get();
                f.setVisible(true);
                return f;
              });
      idle((BaseFrame) frame);
      assertNotNull(edt(() -> find(frame, "records")));
      edt(
          () -> {
            frame.dispose();
            return null;
          });
    }
    // Compatibility screens preserve their original class names and shared domain views.
    for (java.util.function.Supplier<JFrame> screen :
        java.util.List.<java.util.function.Supplier<JFrame>>of(
            () -> new Master.InputCustomer(fixture.service),
            () -> new Master.InputResep(fixture.service),
            () -> new Master.Obat1(fixture.service),
            () -> new Master.Resep1(fixture.service),
            () -> new Master.Customer1(fixture.service))) {
      JFrame frame =
          edt(
              () -> {
                JFrame f = screen.get();
                f.setVisible(true);
                return f;
              });
      idle((BaseFrame) frame);
      edt(
          () -> {
            frame.dispose();
            return null;
          });
    }
  }

  @Test
  void purchaseSalesQuickAddAndMonthlyReports() throws Exception {
    var buy =
        edt(
            () -> {
              var f = new Transaksi.TransaksiPembelian(fixture.service);
              f.setVisible(true);
              return f;
            });
    idle(buy);
    click(buy, "Tambah obat");
    edt(
        () -> {
          ((JTable) find(buy, "cart")).setRowSelectionInterval(0, 0);
          return null;
        });
    click(buy, "Hapus baris");
    assertEquals(0, edt(() -> ((JTable) find(buy, "cart")).getRowCount()));
    click(buy, "Tambah obat");
    click(buy, "Kosongkan rincian");
    click(buy, "Tambah obat");
    click(buy, "Simpan transaksi");
    idle(buy);
    assertEquals(21, fixture.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    screenshot(buy, "04-purchase.png");
    edt(
        () -> {
          buy.dispose();
          return null;
        });
    fixture.login(Role.KASIR);
    var cashier =
        edt(
            () -> {
              var f = new Kasir.DashKasir(fixture.service);
              f.setVisible(true);
              return f;
            });
    assertNotNull(edt(() -> find(cashier, "Penjualan")));
    edt(
        () -> {
          cashier.dispose();
          return null;
        });
    var sell =
        edt(
            () -> {
              var f = new Transaksi.TransaksiPenjualan(fixture.service);
              f.setVisible(true);
              return f;
            });
    idle(sell);
    click(sell, "Urut stok");
    click(sell, "Filter");
    click(sell, "Tambah obat");
    screenshot(sell, "05-sales.png");
    click(sell, "Simpan transaksi");
    idle(sell);
    assertEquals(20, fixture.integer("SELECT stok FROM mObat WHERE id_obat='OBT_TEST'"));
    click(sell, "Tambah customer");
    await(() -> windows(Master.InputCustomer.class) > 0);
    click(sell, "Tambah resep");
    await(() -> windows(Master.InputResep.class) > 0);
    edt(
        () -> {
          for (Window w : Window.getWindows()) w.dispose();
          return null;
        });
    fixture.login(Role.KEPALA);
    var report =
        edt(
            () -> {
              var f = new Laporan.LaporanPenjualan(fixture.service);
              f.setVisible(true);
              return f;
            });
    idle(report);
    assertEquals(1, edt(() -> ((JTable) find(report, "report")).getRowCount()));
    click(report, "Tampilkan");
    idle(report);
    screenshot(report, "06-sales-report.png");
    click(report, "Preview cetak / PDF");
    idle(report);
    await(
        () ->
            java.util.Arrays.stream(Window.getWindows())
                .anyMatch(
                    w -> w instanceof net.sf.jasperreports.view.JasperViewer && w.isShowing()));
    edt(
        () -> {
          report.dispose();
          return null;
        });
    var purchaseReport =
        edt(
            () -> {
              var f = new Laporan.LaporanPembelian(fixture.service);
              f.setVisible(true);
              return f;
            });
    idle(purchaseReport);
    assertEquals(1, edt(() -> ((JTable) find(purchaseReport, "report")).getRowCount()));
    click(purchaseReport, "Menu utama");
    await(() -> windows(Kepala.DashKepala.class) > 0);
    var chief =
        edt(
            () ->
                (JFrame)
                    java.util.Arrays.stream(Window.getWindows())
                        .filter(w -> w instanceof Kepala.DashKepala && w.isShowing())
                        .findFirst()
                        .orElseThrow());
    click(chief, "Keluar akun");
    assertNull(fixture.service.session());
  }

  private static int windows(Class<?> type) {
    return (int)
        java.util.Arrays.stream(Window.getWindows())
            .filter(w -> type.isInstance(w) && w.isShowing())
            .count();
  }

  private static Component find(Container root, String name) {
    for (Component c : root.getComponents()) {
      if (name.equals(c.getName())) return c;
      if (c instanceof Container nested) {
        Component found = find(nested, name);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static JOptionPane findPane(Container root) {
    for (Component c : root.getComponents()) {
      if (c instanceof JOptionPane pane) return pane;
      if (c instanceof Container nested) {
        JOptionPane pane = findPane(nested);
        if (pane != null) return pane;
      }
    }
    return null;
  }

  private static void click(Container frame, String name) throws Exception {
    edt(
        () -> {
          Component c = find(frame, name);
          assertInstanceOf(JButton.class, c);
          ((JButton) c).doClick();
          return null;
        });
  }

  private static void idle(BaseFrame frame) throws Exception {
    await(() -> !frame.isWorking());
    edt(() -> null);
  }

  private static void await(BooleanSupplier condition) throws Exception {
    long end = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
    while (System.nanoTime() < end) {
      if (edt(condition::getAsBoolean)) return;
      Thread.sleep(30);
    }
    fail("Timed out waiting for this application's EDT operation.");
  }

  private static <T> T edt(Callable<T> action) throws Exception {
    if (SwingUtilities.isEventDispatchThread()) return action.call();
    java.util.concurrent.FutureTask<T> task = new java.util.concurrent.FutureTask<>(action);
    SwingUtilities.invokeAndWait(task);
    return task.get();
  }

  private static void screenshot(JFrame frame, String name) throws Exception {
    String destination = System.getProperty("halotek.screenshot.dir", "target/desktop-qa");
    Path dir = Path.of(destination);
    Files.createDirectories(dir);
    BufferedImage picture =
        edt(
            () -> {
              frame.validate();
              BufferedImage image =
                  new BufferedImage(
                      frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
              Graphics2D g = image.createGraphics();
              frame.paintAll(g);
              g.dispose();
              return image;
            });
    ImageIO.write(picture, "png", dir.resolve(name).toFile());
  }
}
