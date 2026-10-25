package Kepala;

import app.core.*;

public class DashKepala extends app.ui.Dashboard {
  public DashKepala() {
    this(AppRuntime.service());
  }

  public DashKepala(AppService service) {
    super(service, Role.KEPALA);
  }
}
