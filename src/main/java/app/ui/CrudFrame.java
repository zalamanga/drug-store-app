package app.ui;

import app.core.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class CrudFrame extends BaseFrame {
  private final Entity entity;
  private final Map<String, JComponent> inputs = new LinkedHashMap<>();
  private final JTextField search = new JTextField(24);
  private final DefaultTableModel model =
      new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int r, int c) {
          return false;
        }
      };
  private final JTable table = new JTable(model);
  private final JButton edit, remove;
  private List<Map<String, Object>> rows = List.of();
  private String selected;
  private boolean loading;

  private record Loaded(
      List<Map<String, Object>> rows, Map<String, List<AppService.Option>> options) {}

  public CrudFrame(AppService service, Entity entity) {
    super(service, entity.label, "Pilih baris untuk mengubah data. ID baru dibuat otomatis.");
    entity.requireAccess(service.session(), false);
    this.entity = entity;
    JPanel form = new JPanel(new GridBagLayout());
    int row = 0;
    for (Entity.Field f : entity.fields) {
      JComponent control =
          switch (f.type()) {
            case "password" -> new JPasswordField(22);
            case "role" -> new JComboBox<>(new String[] {"Admin", "Kasir", "Kepala"});
            case "status" -> new JComboBox<>(new String[] {"Aktif", "Tidak Aktif"});
            case "category", "customer" -> new JComboBox<AppService.Option>();
            default -> new JTextField(22);
          };
      control.setName(f.column());
      inputs.put(f.column(), control);
      Ui.label(form, f.label(), control, row++);
    }
    ((JTextField) inputs.get(entity.idColumn())).setEditable(false);
    JPanel formActions = new JPanel(new GridLayout(0, 2, 8, 8));
    JButton create = Ui.button("Simpan baru", () -> persist(false));
    Ui.primary(create);
    edit = Ui.button("Ubah", () -> persist(true));
    remove = Ui.button("Hapus", this::remove);
    formActions.add(create);
    formActions.add(edit);
    formActions.add(remove);
    formActions.add(Ui.button("Form baru", this::clearForm));
    JPanel left = new JPanel(new BorderLayout());
    left.add(form, BorderLayout.NORTH);
    left.add(formActions, BorderLayout.SOUTH);
    JScrollPane formScroll = new JScrollPane(left);
    formScroll.setMinimumSize(new Dimension(390, 100));
    table.setName("records");
    table.setRowHeight(28);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    entity.visibleFields().forEach(f -> model.addColumn(f.label()));
    table
        .getSelectionModel()
        .addListSelectionListener(
            e -> {
              if (!e.getValueIsAdjusting() && !loading) select();
            });
    JScrollPane list = new JScrollPane(table);
    list.setMinimumSize(new Dimension(280, 120));
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formScroll, list);
    split.setResizeWeight(.48);
    body.add(split);
    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel searchLabel = new JLabel("Cari ID / nama");
    searchLabel.setLabelFor(search);
    search.setName("search");
    top.add(searchLabel);
    top.add(search);
    top.add(Ui.button("Cari", this::refresh));
    top.add(
        Ui.button(
            "Muat ulang",
            () -> {
              search.setText("");
              refresh();
            }));
    top.add(Ui.button("Menu utama", () -> Ui.home(service, this)));
    body.add(top, BorderLayout.NORTH);
    search.addActionListener(e -> refresh());
    clearForm();
    refresh();
  }

  public void refresh() {
    String term = search.getText();
    task(
        "Memuat " + entity.label.toLowerCase(Locale.ROOT) + "...",
        () -> {
          Map<String, List<AppService.Option>> choices = new HashMap<>();
          for (Entity.Field f : entity.fields) {
            if (f.type().equals("category"))
              choices.put(f.column(), service.options(Entity.KATEGORI));
            if (f.type().equals("customer"))
              choices.put(f.column(), service.options(Entity.CUSTOMER));
          }
          return new Loaded(service.list(entity, term), choices);
        },
        loaded -> {
          loading = true;
          rows = loaded.rows();
          model.setRowCount(0);
          for (Map<String, Object> value : rows)
            model.addRow(entity.visibleFields().stream().map(f -> value.get(f.column())).toArray());
          loaded
              .options()
              .forEach(
                  (key, values) -> {
                    @SuppressWarnings("unchecked")
                    JComboBox<AppService.Option> box =
                        (JComboBox<AppService.Option>) inputs.get(key);
                    box.removeAllItems();
                    values.forEach(box::addItem);
                  });
          for (int n = 0; n < table.getColumnCount(); n++)
            table.getColumnModel().getColumn(n).setPreferredWidth(n == 0 ? 150 : 160);
          loading = false;
          clearForm();
          status.setText(
              rows.isEmpty()
                  ? "Belum ada data yang cocok. Tambahkan data baru atau ubah pencarian."
                  : rows.size()
                      + " data ditampilkan (maksimal 500). Pilih satu baris untuk mengubah.");
        });
  }

  public void clearForm() {
    selected = null;
    table.clearSelection();
    inputs.forEach(
        (key, field) -> {
          if (field instanceof JTextField text) text.setText("");
          else if (field instanceof JComboBox<?> box && box.getItemCount() > 0)
            box.setSelectedIndex(0);
        });
    ((JTextField) inputs.get(entity.idColumn())).setText(entity.newId());
    if (inputs.get("tgl_input") instanceof JTextField date)
      date.setText(LocalDate.now().toString());
    if (inputs.get("exp_date") instanceof JTextField date)
      date.setText(LocalDate.now().plusYears(1).toString());
    for (String name : List.of("stok", "harga"))
      if (inputs.get(name) instanceof JTextField number) number.setText("0");
    edit.setEnabled(false);
    remove.setEnabled(false);
  }

  private void select() {
    int index = table.getSelectedRow();
    if (index < 0) return;
    Map<String, Object> value = rows.get(table.convertRowIndexToModel(index));
    selected = value.get(entity.idColumn()).toString();
    inputs.forEach(
        (key, field) -> {
          String text = Objects.toString(value.get(key), "");
          if (field instanceof JPasswordField secret) secret.setText("");
          else if (field instanceof JTextField input) input.setText(text);
          else if (field instanceof JComboBox<?> box) {
            for (int n = 0; n < box.getItemCount(); n++) {
              Object choice = box.getItemAt(n);
              if (choice instanceof AppService.Option option
                  ? option.id().equals(text)
                  : choice.toString().equals(text)) {
                box.setSelectedIndex(n);
                break;
              }
            }
          }
        });
    edit.setEnabled(true);
    remove.setEnabled(true);
    status.setText(
        entity == Entity.USER
            ? "Password tidak ditampilkan. Kosongkan password saat mengubah jika tetap memakai password lama."
            : "Data dipilih. Gunakan Ubah, Hapus, atau Form baru.");
  }

  private Map<String, String> values() {
    Map<String, String> result = new LinkedHashMap<>();
    inputs.forEach(
        (key, input) -> {
          if (input instanceof JPasswordField password) {
            char[] chars = password.getPassword();
            result.put(key, new String(chars));
            Arrays.fill(chars, '\0');
          } else if (input instanceof JTextField text) result.put(key, text.getText());
          else if (input instanceof JComboBox<?> box) {
            Object value = box.getSelectedItem();
            result.put(
                key,
                value instanceof AppService.Option option
                    ? option.id()
                    : Objects.toString(value, ""));
          }
        });
    return result;
  }

  private void persist(boolean update) {
    if (update && selected == null || !update && selected != null) {
      status.setText(
          update ? "Pilih data terlebih dahulu." : "Klik Form baru sebelum menambah data.");
      return;
    }
    Map<String, String> input = values();
    try {
      entity.validate(input, update);
    } catch (IllegalArgumentException ex) {
      status.setText(ex.getMessage());
      return;
    }
    task(
        "Menyimpan data...",
        () -> {
          service.save(entity, input, update);
          return true;
        },
        ok -> {
          status.setText("Data tersimpan.");
          refresh();
        });
  }

  private void remove() {
    if (selected == null) return;
    if (JOptionPane.showConfirmDialog(
            this,
            "Hapus/nonaktifkan data " + selected + "?",
            "Konfirmasi",
            JOptionPane.OK_CANCEL_OPTION)
        != JOptionPane.OK_OPTION) return;
    String id = selected;
    task(
        "Menghapus data...",
        () -> {
          service.delete(entity, id);
          return true;
        },
        ok -> refresh());
  }
}
