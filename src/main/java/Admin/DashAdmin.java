package Admin;

import app.core.*;

public class DashAdmin extends app.ui.Dashboard {
  public DashAdmin() {
    this(AppRuntime.service());
  }

  public DashAdmin(AppService service) {
    super(service, Role.ADMIN);
  }
}
