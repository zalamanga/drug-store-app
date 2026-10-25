package Transaksi;

import app.core.*;

public class TransaksiPembelian extends app.ui.TradeFrame {
  public TransaksiPembelian() {
    this(AppRuntime.service());
  }

  public TransaksiPembelian(AppService service) {
    super(service, true);
  }
}
