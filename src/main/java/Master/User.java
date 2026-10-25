package Master;

import app.core.*;

public class User extends app.ui.CrudFrame {
  public User() {
    this(AppRuntime.service());
  }

  public User(AppService service) {
    super(service, Entity.USER);
  }
}
