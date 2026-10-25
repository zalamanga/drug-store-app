package app.core;

public enum Role {
  ADMIN("Admin"),
  KASIR("Kasir"),
  KEPALA("Kepala");
  private final String label;

  Role(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static Role parse(String value) {
    for (Role role : values()) if (role.label.equals(value)) return role;
    throw new IllegalArgumentException("Jabatan tidak dikenali.");
  }
}
