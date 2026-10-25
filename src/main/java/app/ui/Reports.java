package app.ui;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Reports {
  private Reports() {}

  public static JasperPrint create(
      boolean purchase, int year, int month, List<Map<String, Object>> data) throws JRException {
    String[] fields =
        purchase
            ? new String[] {"id_pembelian", "nama_supplier", "tgl_pembelian", "total_harga_beli"}
            : new String[] {
              "id_penjualan", "nama_obat", "jumlah_obat", "tgl_penjualan", "jumlah_harga"
            };
    String title =
        "HaloTek - Laporan " + (purchase ? "Pembelian" : "Penjualan") + " " + month + "/" + year;
    StringBuilder xml =
        new StringBuilder(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" name=\"halotek\" pageWidth=\"842\" pageHeight=\"595\" orientation=\"Landscape\" columnWidth=\"802\" leftMargin=\"20\" rightMargin=\"20\" topMargin=\"20\" bottomMargin=\"20\">");
    for (String f : fields)
      xml.append("<field name=\"").append(f).append("\" class=\"java.lang.Object\"/>");
    xml.append(
            "<title><band height=\"45\"><staticText><reportElement x=\"0\" y=\"0\" width=\"802\" height=\"35\"/><textElement><font size=\"18\" isBold=\"true\"/></textElement><text><![CDATA[")
        .append(title)
        .append("]]></text></staticText></band></title><columnHeader><band height=\"30\">");
    int width = 802 / fields.length;
    for (int n = 0; n < fields.length; n++)
      xml.append("<staticText><reportElement x=\"")
          .append(n * width)
          .append("\" y=\"0\" width=\"")
          .append(width)
          .append(
              "\" height=\"28\"/><textElement><font isBold=\"true\"/></textElement><text><![CDATA[")
          .append(fields[n].replace('_', ' '))
          .append("]]></text></staticText>");
    xml.append("</band></columnHeader><detail><band height=\"34\">");
    for (int n = 0; n < fields.length; n++)
      xml.append("<textField isBlankWhenNull=\"true\"><reportElement x=\"")
          .append(n * width)
          .append("\" y=\"0\" width=\"")
          .append(width - 8)
          .append("\" height=\"32\"/><textFieldExpression><![CDATA[$F{")
          .append(fields[n])
          .append("}]]></textFieldExpression></textField>");
    java.math.BigDecimal total =
        data.stream()
            .map(row -> (java.math.BigDecimal) row.get(fields[fields.length - 1]))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    xml.append(
            "</band></detail><summary><band height=\"35\"><staticText><reportElement x=\"0\" y=\"8\" width=\"802\" height=\"25\"/><textElement><font size=\"12\" isBold=\"true\"/></textElement><text><![CDATA[Total: Rp ")
        .append(total.toPlainString())
        .append("]]></text></staticText></band></summary></jasperReport>");
    JasperReport report =
        JasperCompileManager.compileReport(
            new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
    List<Map<String, ?>> records = new ArrayList<>(data);
    return JasperFillManager.fillReport(
        report, new HashMap<>(), new JRMapCollectionDataSource(records));
  }
}
