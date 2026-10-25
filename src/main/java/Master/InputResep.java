package Master;

import app.core.*;

public class InputResep extends app.ui.CrudFrame {
  public InputResep() {
    this(AppRuntime.service());
  }

  public InputResep(AppService service) {
    super(service, Entity.RESEP);
  }
}
