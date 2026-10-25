package app.ui;

import app.core.AppService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class BaseFrame extends JFrame {
  protected final AppService service;
  protected final JPanel body = new JPanel(new BorderLayout(12, 12));
  protected final JTextArea status = new JTextArea(2, 30);
  private boolean working;

  protected BaseFrame(AppService service, String title, String subtitle) {
    super(title + " | HaloTek");
    this.service = service;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());
    add(Ui.header(title, subtitle), BorderLayout.NORTH);
    body.setBorder(BorderFactory.createEmptyBorder(6, 18, 12, 18));
    add(body);
    status.setEditable(false);
    status.setFocusable(false);
    status.setLineWrap(true);
    status.setWrapStyleWord(true);
    status.setOpaque(false);
    status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    status.getAccessibleContext().setAccessibleName("Status operasi");
    status.setBorder(BorderFactory.createEmptyBorder(8, 18, 10, 18));
    add(status, BorderLayout.SOUTH);
    setSize(1180, 760);
    setMinimumSize(new Dimension(780, 560));
    setLocationRelativeTo(null);
    getRootPane()
        .registerKeyboardAction(
            e -> Ui.home(service, this),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  protected <T> void task(String message, Callable<T> action, Consumer<T> done) {
    if (working) return;
    working = true;
    status.setText(message);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    SwingWorker<T, Void> worker =
        new SwingWorker<>() {
          @Override
          protected T doInBackground() throws Exception {
            return action.call();
          }

          @Override
          protected void done() {
            working = false;
            setCursor(Cursor.getDefaultCursor());
            if (!isDisplayable()) return;
            try {
              done.accept(get());
            } catch (Exception ex) {
              status.setText("Operasi belum berhasil. Anda dapat mencoba lagi.");
              Ui.error(BaseFrame.this, ex);
            }
          }
        };
    worker.execute();
  }

  public boolean isWorking() {
    return working;
  }
}
