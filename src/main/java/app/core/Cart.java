package app.core;

import java.math.BigDecimal;
import java.util.*;

public final class Cart {
  public record Line(String medicineId, String name, BigDecimal price, int quantity) {
    public Line {
      if (medicineId == null
          || medicineId.isBlank()
          || name == null
          || price == null
          || price.signum() < 0
          || price.scale() > 2
          || price.compareTo(new BigDecimal("1000000000")) > 0
          || quantity < 1
          || quantity > 1_000_000)
        throw new IllegalArgumentException("Obat, harga dan jumlah tidak valid.");
    }

    public BigDecimal total() {
      return price.multiply(BigDecimal.valueOf(quantity));
    }
  }

  private final Map<String, Line> lines = new LinkedHashMap<>();

  public void add(Line line) {
    Line previous = lines.get(line.medicineId);
    if (previous != null && previous.price.compareTo(line.price) != 0)
      throw new IllegalArgumentException("Harga obat berubah. Hapus baris lama lalu tambah lagi.");
    int quantity =
        previous == null ? line.quantity : Math.addExact(previous.quantity, line.quantity);
    lines.put(line.medicineId, new Line(line.medicineId, line.name, line.price, quantity));
  }

  public void remove(String id) {
    lines.remove(id);
  }

  public void clear() {
    lines.clear();
  }

  public List<Line> lines() {
    return List.copyOf(lines.values());
  }

  public BigDecimal total() {
    return lines.values().stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
