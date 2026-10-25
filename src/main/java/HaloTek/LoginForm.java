package HaloTek;

import app.core.*;
import app.ui.*;
import javax.swing.*;
import java.awt.*;

/** HaloTek login. Original application form credits: Ariza and Youmal. */
public class LoginForm extends BaseFrame {
  private final JTextField username = new JTextField(24);
  private final JPasswordField password = new JPasswordField(24);

  public LoginForm() {
    this(AppRuntime.service());
  }

  public LoginForm(AppService service) {
    super(service, "HaloTek", "Sistem Informasi Toko Apotek");
    service.logout();
    setSize(830, 620);
    setMinimumSize(new Dimension(660, 520));
    JPanel fields = new JPanel(new GridBagLayout());
    username.setName("username");
    password.setName("password");
    Ui.label(fields, "Username", username, 0);
    Ui.label(fields, "Password", password, 1);
    JCheckBox show = new JCheckBox("Lihat password");
    show.setName("showPassword");
    char echo = password.getEchoChar();
    show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echo));
    Ui.label(fields, "", show, 2);
    JPanel actions = new JPanel();
    JButton login = Ui.button("Masuk", this::login);
    Ui.primary(login);
    actions.add(login);
    actions.add(
        Ui.button(
            "Kosongkan",
            () -> {
              username.setText("");
              password.setText("");
              username.requestFocusInWindow();
            }));
    JPanel center = new JPanel(new BorderLayout(12, 16));
    center.setBorder(BorderFactory.createEmptyBorder(35, 70, 35, 70));
    center.add(fields);
    center.add(actions, BorderLayout.SOUTH);
    body.add(center);
    status.setText("Aplikasi asli: Ariza dan Youmal. Masuk dengan akun pada database demo Anda.");
    getRootPane().setDefaultButton(login);
    getRootPane().unregisterKeyboardAction(KeyStroke.getKeyStroke("ESCAPE"));
  }

  private void login() {
    String user = username.getText().trim();
    char[] secret = password.getPassword();
    if (user.isEmpty() || secret.length == 0) {
      java.util.Arrays.fill(secret, '\0');
      status.setText("Isi username dan password.");
      username.requestFocusInWindow();
      return;
    }
    task(
        "Memeriksa akun...",
        () -> service.login(user, secret),
        session -> {
          password.setText("");
          if (session == null) {
            status.setText("Username atau password salah, atau akun tidak aktif.");
            password.requestFocusInWindow();
          } else Ui.home(service, this);
        });
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          Ui.install();
          new LoginForm().setVisible(true);
        });
  }
}
