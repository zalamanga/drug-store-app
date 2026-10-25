package Master;

import app.core.*;

public class Customer1 extends app.ui.CrudFrame {
  public Customer1() {
    this(AppRuntime.service());
  }

  public Customer1(AppService service) {
    super(service, Entity.CUSTOMER);
  }
}
