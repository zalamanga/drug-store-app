package Laporan;

import app.core.*;

public class LaporanPembelian extends app.ui.ReportFrame {
  public LaporanPembelian() {
    this(AppRuntime.service());
  }

  public LaporanPembelian(AppService service) {
    super(service, true);
  }
}
