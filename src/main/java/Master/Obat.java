package Master;

import app.core.*;

public class Obat extends app.ui.CrudFrame {
  public Obat() {
    this(AppRuntime.service());
  }

  public Obat(AppService service) {
    super(service, Entity.OBAT);
  }
}
