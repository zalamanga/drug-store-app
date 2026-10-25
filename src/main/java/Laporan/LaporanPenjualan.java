package Laporan;

import app.core.*;

public class LaporanPenjualan extends app.ui.ReportFrame {
  public LaporanPenjualan() {
    this(AppRuntime.service());
  }

  public LaporanPenjualan(AppService service) {
    super(service, false);
  }
}
