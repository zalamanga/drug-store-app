package app.ui;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportsTest {
  @Test
  void syntheticPurchaseAndSalesReportsCompileAndExportPdf() throws Exception {
    Map<String, Object> buy =
        Map.of(
            "id_pembelian",
            "TRB_DEMO",
            "nama_supplier",
            "Supplier sintetis",
            "tgl_pembelian",
            Date.valueOf("2026-10-25"),
            "total_harga_beli",
            new BigDecimal("2500.00"));
    var purchase = Reports.create(true, 2026, 10, List.of(buy));
    assertFalse(purchase.getPages().isEmpty());
    byte[] pdf = net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(purchase);
    assertTrue(
        new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));
    Map<String, Object> sale =
        Map.of(
            "id_penjualan",
            "TRJ_DEMO",
            "nama_obat",
            "Obat sintetis",
            "jumlah_obat",
            2,
            "tgl_penjualan",
            Date.valueOf("2026-10-25"),
            "jumlah_harga",
            new BigDecimal("2500.00"));
    assertFalse(Reports.create(false, 2026, 10, List.of(sale)).getPages().isEmpty());
  }
}
