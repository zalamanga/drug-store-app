package Master;

import app.core.*;

public class InputCustomer extends app.ui.CrudFrame {
  public InputCustomer() {
    this(AppRuntime.service());
  }

  public InputCustomer(AppService service) {
    super(service, Entity.CUSTOMER);
  }
}
