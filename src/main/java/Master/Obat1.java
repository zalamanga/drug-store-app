package Master;

import app.core.*;

public class Obat1 extends app.ui.CrudFrame {
  public Obat1() {
    this(AppRuntime.service());
  }

  public Obat1(AppService service) {
    super(service, Entity.OBAT);
  }
}
