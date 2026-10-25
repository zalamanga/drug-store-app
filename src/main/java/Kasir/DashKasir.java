package Kasir;

import app.core.*;

public class DashKasir extends app.ui.Dashboard {
  public DashKasir() {
    this(AppRuntime.service());
  }

  public DashKasir(AppService service) {
    super(service, Role.KASIR);
  }
}
