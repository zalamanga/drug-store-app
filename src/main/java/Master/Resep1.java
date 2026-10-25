package Master;

import app.core.*;

public class Resep1 extends app.ui.CrudFrame {
  public Resep1() {
    this(AppRuntime.service());
  }

  public Resep1(AppService service) {
    super(service, Entity.RESEP);
  }
}
