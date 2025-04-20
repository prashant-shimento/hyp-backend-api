package com.hyp.playground;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PdfGenerator {

    public static void main(String[] args) throws Exception {
        generateInvoicePdf("src/main/resources/invoice.pdf");
    }
    private final static String TOP_IMAGE_PATH = "src/main/resources/images/top.png";
    private final static String BOTTOM_IMAGE_PATH = "src/main/resources/images/bottom.png";
    private final static String BOLD_FONT_PATH = "src/main/resources/fonts/noto-bold.ttf";
    private final static String REGULAR_FONT_PATH = "src/main/resources/fonts/noto-regular.ttf";
    private final static String INVOICE_TOP_IMAGE_PATH = "src/main/resources/images/top_invoice.png";
    private final static String INVOICE_BOTTOM_IMAGE_PATH = "src/main/resources/images/bottom_invoice.png";

    public static void generatePdf(String outputPath, Map<String, Object> reportData) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addNewPage();

        addPageBanners(pdf);

        Document doc = new Document(pdf);
        PdfFont bold = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont regular = PdfFontFactory.createFont(REGULAR_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginTop(140)
                .setMarginBottom(30);

        String startDate = (String) reportData.get("startDate");
        String endDate = (String) reportData.get("endDate");
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);
        String formattedSettlementDate = formatSettlementDate();

        infoTable.addCell(createCell("BILL TO:", bold, TextAlignment.LEFT, false));
        infoTable.addCell(createLabelValueCell("Period:", formattedStartDate + " - " + formattedEndDate, bold, regular, TextAlignment.RIGHT));

        infoTable.addCell(createCell((String) reportData.get("partner_name"), bold, TextAlignment.LEFT, false));
        infoTable.addCell(createLabelValueCell("Settlement Date:", formattedSettlementDate, bold, regular, TextAlignment.RIGHT));

        doc.add(infoTable);

        addSummary(doc, reportData, bold, regular);

        // Styled Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();

        addRow(table, "Particular", "INR", bold, bold, ColorConstants.LIGHT_GRAY, true);
        addDynamicRows(table, reportData, bold, regular);
        doc.add(table.setMarginTop(10).setMarginBottom(20));

        doc.close();
    }

    public static void generateInvoicePdf(String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdf = new PdfDocument(writer);

        pdf.addNewPage();
        addPageBanners(pdf);

        Document doc = new Document(pdf);
        doc.setMargins(160, 36, 100, 36);

        PdfFont bold = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont regular = PdfFontFactory.createFont(REGULAR_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        Table mainInfoTable = new Table(1)
                .useAllAvailableWidth()
                .setMarginTop(0)
                .setMarginBottom(30);

        Table invoiceTable = new Table(1)
                .setWidth(UnitValue.createPercentValue(100));

        Cell invoiceCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(createCell("PAID", bold, TextAlignment.RIGHT, false))
                .add(createLabelValueCell("Invoice No:", "1008471", bold, regular, TextAlignment.RIGHT))
                .add(createLabelValueCell("Invoice Date:", "18th Mar, 2025", bold, regular, TextAlignment.RIGHT));

        invoiceTable.addCell(invoiceCell);

        Cell invoiceRowWrapper = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(invoiceTable);

        mainInfoTable.addCell(invoiceRowWrapper);

        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginTop(10);

        Cell customerColumn = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(createCell("Bill To:", bold, TextAlignment.LEFT, false))
                .add(createLabelValueCell("Customer Name:", "Lognath", bold, regular, TextAlignment.LEFT))
                .add(createLabelValueCell("Customer Contact:", "9965861660", bold, regular, TextAlignment.LEFT))
                .add(createCell("Delivery Address:", bold, TextAlignment.LEFT, false))
                .add(createCell("12D Building, Mindspace,", regular, TextAlignment.LEFT, false))
                .add(createCell("Madhapur, Raheja Park,", regular, TextAlignment.LEFT, false))
                .add(createCell("Hyderabad", regular, TextAlignment.LEFT, false))
                .add(createCell("Telangana - 500081", regular, TextAlignment.LEFT, false));

        Cell restaurantColumn = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(createCell("On Behalf Of:", bold, TextAlignment.LEFT, false))
                .add(createLabelValueCell("Restaurant Name:", "Ankapur Village", bold, regular, TextAlignment.LEFT))
                .add(createLabelValueCell("Restaurant Contact:", "8865861662", bold, regular, TextAlignment.LEFT))
                .add(createCell("Restaurant Address:", bold, TextAlignment.LEFT, false))
                .add(createCell("Near MaxCure Hospital Lane,", regular, TextAlignment.LEFT, false))
                .add(createCell("Hi-Tec City,", regular, TextAlignment.LEFT, false))
                .add(createCell("Hyderabad", regular, TextAlignment.LEFT, false))
                .add(createCell("Telangana - 500081", regular, TextAlignment.LEFT, false));

        detailTable.addCell(customerColumn);
        detailTable.addCell(restaurantColumn);

        Cell detailsRowWrapper = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(detailTable);

        mainInfoTable.addCell(detailsRowWrapper);

        doc.add(mainInfoTable);
        
        Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth();

        addRow(table, "Particular", "INR", bold, bold, ColorConstants.LIGHT_GRAY, true);

        List<String[]> items = Arrays.asList(
                new String[]{"Chicken Clay Pot Boneless", "479"},
                new String[]{"Chicken Clay Pot Bone", "419"},
                new String[]{"Chicken Clay Pot Boneless x 2", "1038"},
                new String[]{"Chicken Clay Pot Boneless", "479"},
                new String[]{"Chicken Clay Pot Bone", "419"}
        );


        for (String[] item : items) {
            addRow(table, item[0], item[1], regular, regular, false);
        }

        addRow(table, "Total", "1522", bold, bold, true);
        addRow(table, "Discount (25%)", "-484", bold, bold, true);
        addRow(table, "CGST (2.5%)", "35", bold, bold, true);
        addRow(table, "SGST (2.5%)", "35", bold, bold, true);
        addRow(table, "Delivery Charge", "29", bold, bold, true);
        addRow(table, "Final Amount", "1550", bold, bold, true);

        doc.add(table.setMarginTop(10).setMarginBottom(20));

        doc.close();
    }

    private static void addRow(Table table, String key, String value, PdfFont keyFont, PdfFont valueFont, boolean header) {
        Paragraph p1 = new Paragraph(key)
                .setFont(keyFont)
                .setFontSize(header ? 12 : 11)
                .setMargin(0).setPadding(0);

        Paragraph p2 = new Paragraph(value)
                .setFont(valueFont)
                .setFontSize(header ? 12 : 11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMargin(0).setPadding(0);

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


    private static void addSummary(Document doc, Map<String, Object> reportData, PdfFont bold, PdfFont regular) {
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
            Image topImage = new Image(ImageDataFactory.create(INVOICE_TOP_IMAGE_PATH));
            topImage.scaleToFit(pageWidth, 1000);
            float topMargin = 40;
            float topImageHeight = topImage.getImageScaledHeight();
            topImage.setFixedPosition(0, pageSize.getTop() - topImageHeight - topMargin);

            new Canvas(page, pageSize).add(topImage);
        } catch (Exception e) {
            System.err.println("Failed to add top banner: " + e.getMessage());
        }

        try {
            Image bottomImage = new Image(ImageDataFactory.create(INVOICE_BOTTOM_IMAGE_PATH));
            bottomImage.scaleToFit(pageWidth, 1000);
            float bottomImageHeight = bottomImage.getImageScaledHeight();
            bottomImage.setFixedPosition(0, pageSize.getBottom());

            new Canvas(page, pageSize).add(bottomImage);
        } catch (Exception e) {
            System.err.println("Failed to add bottom banner: " + e.getMessage());
        }
    }


    private static String formatDate(String isoDateString) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(isoDateString);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy");
        return offsetDateTime.format(formatter);
    }

    private static String formatSettlementDate() {
        DateTimeFormatter dateFormatterForCurrentDate = DateTimeFormatter.ofPattern("dd MMM, yyyy");
        return LocalDate.now().format(dateFormatterForCurrentDate);
    }
}