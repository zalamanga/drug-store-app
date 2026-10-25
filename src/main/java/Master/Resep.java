package Master;

import app.core.*;

public class Resep extends app.ui.CrudFrame {
  public Resep() {
    this(AppRuntime.service());
  }

  public Resep(AppService service) {
    super(service, Entity.RESEP);
  }
}
