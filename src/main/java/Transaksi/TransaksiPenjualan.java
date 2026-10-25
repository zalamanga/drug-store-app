package Transaksi;

import app.core.*;

public class TransaksiPenjualan extends app.ui.TradeFrame {
  public TransaksiPenjualan() {
    this(AppRuntime.service());
  }

  public TransaksiPenjualan(AppService service) {
    super(service, false);
  }
}
