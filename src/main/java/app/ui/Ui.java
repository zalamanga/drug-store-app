package app.ui;

import app.core.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Ui {
  public static final Color GREEN = new Color(0x176c3b), BACKGROUND = new Color(0xfbffff);

  private Ui() {}

  public static void install() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception ignored) {
    }
    UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    UIManager.put("TextField.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    UIManager.put("Table.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
  }

  public static JButton button(String label, Runnable action) {
    JButton button = new JButton(label);
    button.setName(label);
    button.setMargin(new Insets(8, 14, 8, 14));
    button.addActionListener(e -> action.run());
    return button;
  }

  public static void primary(JButton button) {
    button.setUI(
        new javax.swing.plaf.basic.BasicButtonUI() {
          @Override
          protected void paintFocus(
              Graphics g, AbstractButton b, Rectangle view, Rectangle text, Rectangle icon) {
            g.setColor(Color.WHITE);
            g.drawRect(4, 4, b.getWidth() - 9, b.getHeight() - 9);
          }
        });
    button.setBackground(GREEN);
    button.setForeground(Color.WHITE);
    button.setOpaque(true);
  }

  public static JPanel header(String title, String description) {
    JPanel header = new JPanel(new BorderLayout(18, 6));
    header.setBackground(BACKGROUND);
    header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
    java.net.URL logo = Ui.class.getResource("/Bahan/LogoKecil_HaloTek.png");
    if (logo != null) {
      JLabel image = new JLabel(new ImageIcon(logo));
      image.getAccessibleContext().setAccessibleName("Logo HaloTek");
      header.add(image, BorderLayout.WEST);
    }
    JPanel words = new JPanel(new GridLayout(0, 1, 0, 5));
    words.setOpaque(false);
    JLabel heading = new JLabel(title);
    heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
    heading.setForeground(GREEN);
    words.add(heading);
    words.add(new JLabel(description));
    header.add(words, BorderLayout.CENTER);
    return header;
  }

  public static void label(JPanel panel, String text, JComponent field, int row) {
    JLabel label = new JLabel(text);
    label.setLabelFor(field);
    field.getAccessibleContext().setAccessibleName(text);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = row;
    c.anchor = GridBagConstraints.LINE_START;
    c.insets = new Insets(6, 8, 6, 12);
    panel.add(label, c);
    c.gridx = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(field, c);
  }

  public static void home(AppService service, JFrame previous) {
    if (service.session() == null) {
      new HaloTek.LoginForm(service).setVisible(true);
    } else
      switch (service.session().role()) {
        case ADMIN -> new Admin.DashAdmin(service).setVisible(true);
        case KASIR -> new Kasir.DashKasir(service).setVisible(true);
        case KEPALA -> new Kepala.DashKepala(service).setVisible(true);
      }
    previous.dispose();
  }

  public static void error(Component owner, Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null
        && !(cause instanceof SQLException)
        && !(cause instanceof IllegalArgumentException)
        && !(cause instanceof SecurityException)) cause = cause.getCause();
    String message;
    if (cause instanceof IllegalArgumentException || cause instanceof SecurityException)
      message = cause.getMessage();
    else {
      if (cause instanceof SQLException sql)
        Logger.getLogger("halotek")
            .warning(
                "Database operation failed; state="
                    + sql.getSQLState()
                    + "; code="
                    + sql.getErrorCode());
      else
        Logger.getLogger("halotek")
            .warning("Operation failed: " + cause.getClass().getSimpleName());
      message =
          "Data belum tersimpan atau dimuat. Periksa koneksi DB, isian unik dan data yang masih dipakai transaksi, lalu coba lagi.";
    }
    JOptionPane.showMessageDialog(owner, message, "Periksa kembali", JOptionPane.WARNING_MESSAGE);
  }
}
