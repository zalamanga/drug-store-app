package Master;

import app.core.*;

public class Customer extends app.ui.CrudFrame {
  public Customer() {
    this(AppRuntime.service());
  }

  public Customer(AppService service) {
    super(service, Entity.CUSTOMER);
  }
}
