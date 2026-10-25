package app.core;

import Config.DBConnect;

public final class AppRuntime {
  private static final AppService SERVICE = new AppService(DBConnect::open);

  private AppRuntime() {}

  public static AppService service() {
    return SERVICE;
  }
}
