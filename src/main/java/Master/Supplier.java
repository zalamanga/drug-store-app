package Master;

import app.core.*;

public class Supplier extends app.ui.CrudFrame {
  public Supplier() {
    this(AppRuntime.service());
  }

  public Supplier(AppService service) {
    super(service, Entity.SUPPLIER);
  }
}
