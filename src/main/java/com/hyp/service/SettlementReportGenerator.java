package com.hyp.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(SettlementReportGenerator.REPORT_NAME)
public class SettlementReportGenerator implements  ReportPdfGenerator{

    final static String REPORT_NAME = "SETTLEMENT_REPORT";
    private final static String TOP_IMAGE_PATH = "src/main/resources/images/top.png";
    private final static String BOTTOM_IMAGE_PATH = "src/main/resources/images/bottom.png";
    private final static String BOLD_FONT_PATH = "src/main/resources/fonts/noto-bold.ttf";
    private final static String REGULAR_FONT_PATH = "src/main/resources/fonts/noto-regular.ttf";

    @Autowired
    private BucketService bucketService;

    @Override
    public String generatePdf(List<Document> reportDoc) throws Exception {
        Map<String, Object> reportData = reportDoc.isEmpty() ? new HashMap<>() : reportDoc.get(0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addNewPage();

        addPageBanners(pdf);

        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
        PdfFont bold = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont regular = PdfFontFactory.createFont(REGULAR_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginTop(140)
                .setMarginBottom(30);

        Date startDate = (Date) reportData.get("startDate");
        Date endDate = (Date) reportData.get("endDate");
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);
        String formattedSettlementDate = formatSettlementDate();

        infoTable.addCell(createCell("Bill To:", bold, TextAlignment.LEFT, false));
        infoTable.addCell(createLabelValueCell("Period:", formattedStartDate + " - " + formattedEndDate, bold, regular, TextAlignment.RIGHT));

        infoTable.addCell(createCell((String) reportData.get("partner_name"), bold, TextAlignment.LEFT, false));
        infoTable.addCell(createLabelValueCell("Settlement Date:", formattedSettlementDate, bold, regular, TextAlignment.RIGHT));

        doc.add(infoTable);

        addSummary(doc, reportData, bold, regular);

        Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();

        addRow(table, "Particular", "INR", bold, bold, ColorConstants.LIGHT_GRAY, true);
        addDynamicRows(table, reportData, bold, regular);
        doc.add(table.setMarginTop(10).setMarginBottom(20));

        doc.close();

        String fileName = REPORT_NAME + "-" + System.currentTimeMillis() + ".pdf";
        String filePath = REPORT_NAME + "/" + ((String) reportData.get("partner_name")).trim().replaceAll("[^a-zA-Z0-9_-]", "_") + "/" ;
        return bucketService.uploadFileFromStream(outputStream, fileName, filePath,"application/pdf");
    }

    private static void addSummary(com.itextpdf.layout.Document doc, Map<String, Object> reportData, PdfFont bold, PdfFont regular) {
        Paragraph totalOrders = new Paragraph()
                .add(new Text("Total Orders: ").setFont(bold))
                .add(new Text(String.valueOf(reportData.get("totalOrders"))).setFont(regular))
                .setFontSize(11)
                .setMargin(0)
                .setPadding(0)
                .setMultipliedLeading(1f);

        Paragraph totalAmount = new Paragraph()
                .add(new Text("Total Amount: ").setFont(bold))
                .add(new Text("INR " + reportData.get("totalAmount")).setFont(regular))
                .setFontSize(11)
                .setMarginTop(0)
                .setMarginBottom(15)
                .setPadding(0)
                .setMultipliedLeading(1f);

        doc.add(totalOrders);
        doc.add(totalAmount);
    }

    private static void addDynamicRows(Table table, Map<String, Object> reportData, PdfFont bold, PdfFont regular) {
        addRow(table, "Item Total", String.valueOf(reportData.get("netBillValue")), regular, regular, null, false);
        addRow(table, "Restaurant Discounts", String.valueOf(reportData.get("totalDiscounts")), regular, regular, ColorConstants.LIGHT_GRAY, false);
        addRow(table, "Taxes (GST)", String.valueOf(reportData.get("totalTaxes")), regular, regular, null, false);
        addRow(table, "Net Bill Value", String.valueOf(reportData.get("netBillValue")), bold, bold, ColorConstants.LIGHT_GRAY, false);
        addRow(table, "Platform Service Fee", String.valueOf(reportData.get("platformFee")), regular, regular, null, false);
        addRow(table, "Payment Gateway Charges (2%)", String.valueOf(reportData.get("paymentGatewayCharges")), regular, regular, null, false);
        addRow(table, "PetPooja API Charges (1%)", String.valueOf(reportData.get("petPoojaApiCharges")), regular, regular, null, false);
        addRow(table, "Delivery API Charges (Rs.5/Order)", String.valueOf(reportData.get("deliveryApiCharges")), regular, regular, null, false);
        addRow(table, "Total Service Fees", String.valueOf(reportData.get("netServiceFee")), bold, bold, ColorConstants.LIGHT_GRAY, false);
        addRow(table, "Restaurant Shared Delivery Fee (30%)", String.valueOf(reportData.get("restaurantSharedDeliveryFee")), regular, regular, null, false);
        addRow(table, "Refund for customer complaints", String.valueOf(reportData.get("totalRefunded")), regular, regular, null, false);
    }

    private static void addRow(Table table, String key, String value, PdfFont keyFont, PdfFont valueFont,
                               com.itextpdf.kernel.colors.Color ignored, boolean header) {

        Paragraph p1 = new Paragraph(key)
                .setFont(keyFont)
                .setFontSize(header ? 12 : 11)
                .setMargin(0).setPadding(0);
        Paragraph p2 = new Paragraph(value)
                .setFont(valueFont)
                .setFontSize(header ? 12 : 11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMargin(0).setPadding(0);

        p1.setMultipliedLeading(1.0f); // normal
        p2.setMultipliedLeading(1.0f); // slightly tighter text block

        Cell cell1 = new Cell().add(p1)
                .setPadding(1)
                .setBorderTop(new SolidBorder(ColorConstants.GRAY, 0.75f))
                .setBorderBottom(new SolidBorder(ColorConstants.GRAY, 0.75f))
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);

        Cell cell2 = new Cell().add(p2)
                .setPadding(1)
                .setBorderTop(new SolidBorder(ColorConstants.GRAY, 0.75f))
                .setBorderBottom(new SolidBorder(ColorConstants.GRAY, 0.75f))
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);


        table.addCell(cell1);
        table.addCell(cell2);
    }

    private static Cell createCell(String text, PdfFont font, TextAlignment align, boolean bold) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(11)
                        .setMargin(0)
                        .setPadding(0)
                        .setMultipliedLeading(1f))
                .setTextAlignment(align)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setMargin(0);
    }

    private static Cell createLabelValueCell(String label, String value, PdfFont boldFont, PdfFont regularFont, TextAlignment alignment) {
        Paragraph p = new Paragraph()
                .add(new Text(label).setFont(boldFont))
                .add(new Text(" " + value).setFont(regularFont))
                .setFontSize(11)
                .setTextAlignment(alignment)
                .setMargin(0)
                .setPadding(0)
                .setMultipliedLeading(1f);

        return new Cell()
                .add(p)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setMargin(0);
    }

    public static void addPageBanners(PdfDocument pdfDoc) {
        PdfPage page = pdfDoc.getLastPage();
        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();

        try {
            Image topImage = new Image(ImageDataFactory.create(TOP_IMAGE_PATH));
            topImage.scaleToFit(pageWidth, 1000);
            float topMargin = 40;
            float topImageHeight = topImage.getImageScaledHeight();
            topImage.setFixedPosition(0, pageSize.getTop() - topImageHeight - topMargin);

            new Canvas(page, pageSize).add(topImage);
        } catch (Exception e) {
            System.err.println("Failed to add top banner: " + e.getMessage());
        }

        try {
            Image bottomImage = new Image(ImageDataFactory.create(BOTTOM_IMAGE_PATH));
            bottomImage.scaleToFit(pageWidth, 1000);
            float bottomImageHeight = bottomImage.getImageScaledHeight();
            bottomImage.setFixedPosition(0, pageSize.getBottom());

            new Canvas(page, pageSize).add(bottomImage);
        } catch (Exception e) {
            System.err.println("Failed to add bottom banner: " + e.getMessage());
        }
    }

    private static String formatDate(Date date) {
        if (date != null) {
            LocalDate localDate = Instant.ofEpochMilli(date.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy");
            return localDate.format(formatter);
        }
        return "";
    }

    private static String formatSettlementDate() {
        DateTimeFormatter dateFormatterForCurrentDate = DateTimeFormatter.ofPattern("dd MMM, yyyy");
        return LocalDate.now().format(dateFormatterForCurrentDate);
    }
}
