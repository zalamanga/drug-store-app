package Master;

import app.core.*;

public class Kategori extends app.ui.CrudFrame {
  public Kategori() {
    this(AppRuntime.service());
  }

  public Kategori(AppService service) {
    super(service, Entity.KATEGORI);
  }
}
