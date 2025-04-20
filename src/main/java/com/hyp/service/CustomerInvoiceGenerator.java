package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.dto.CustomerDto;
import com.hyp.enums.OrderStatusType;
import com.hyp.model.CustomerInvoice;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component(CustomerInvoiceGenerator.REPORT_NAME)
public class CustomerInvoiceGenerator implements  ReportPdfGenerator{

    final static String REPORT_NAME = "CUSTOMER_INVOICE";
    private final static String TOP_IMAGE_PATH = "src/main/resources/images/top_invoice.png";
    private final static String BOTTOM_IMAGE_PATH = "src/main/resources/images/bottom_invoice.png";
    private final static String BOLD_FONT_PATH = "src/main/resources/fonts/noto-bold.ttf";
    private final static String REGULAR_FONT_PATH = "src/main/resources/fonts/noto-regular.ttf";
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM, yyyy");
    @Autowired
    private BucketService bucketService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String generatePdf(List<Document> reportDoc) throws Exception {

        CustomerInvoice invoice = objectMapper.convertValue(reportDoc.get(0), CustomerInvoice.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE,
                new BannerHandler(TOP_IMAGE_PATH, BOTTOM_IMAGE_PATH));
        pdf.addNewPage();

        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
        doc.setMargins(160, 36, 100, 36);

        PdfFont bold = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont regular = PdfFontFactory.createFont(REGULAR_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);


        Table mainInfoTable = new Table(1)
                .useAllAvailableWidth()
                .setMarginTop(0)
                .setMarginBottom(30);

        Table invoiceTable = new Table(1)
                .setWidth(UnitValue.createPercentValue(100));

        String status = invoice.getStatus().equalsIgnoreCase(OrderStatusType.PAID.name()) ||
                invoice.getStatus().equalsIgnoreCase(OrderStatusType.DELIVERED.name()) ?
                "PAID" : "UNPAID";
        String formattedOrderDate = formatOrderDate(invoice.getOrderTime());
        Cell invoiceCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(createCell(status, bold, TextAlignment.RIGHT, false))
                .add(createLabelValueCell("Invoice No:", invoice.getInvoiceNumber(), bold, regular, TextAlignment.RIGHT))
                .add(createLabelValueCell("Invoice Date:", formattedOrderDate, bold, regular, TextAlignment.RIGHT));

        invoiceTable.addCell(invoiceCell);

        Cell invoiceRowWrapper = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(invoiceTable);

        mainInfoTable.addCell(invoiceRowWrapper);

        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginTop(10);

        CustomerDto customer = invoice.getCustomer();
        CustomerInvoice.Restaurant restaurant = invoice.getRestaurant();
        CustomerInvoice.Delivery delivery = invoice.getDelivery();
        CustomerInvoice.Address address = delivery.getAddress();

        Cell customerColumn = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(createCell("Bill To:", bold, TextAlignment.LEFT, false))
                .add(createLabelValueCell("Customer Name:", customer.getName(), bold, regular, TextAlignment.LEFT))
                .add(createLabelValueCell("Customer Contact:", customer.getMobile(), bold, regular, TextAlignment.LEFT))
                .add(createCell("Delivery Address:", bold, TextAlignment.LEFT, false))
                .add(createCell(address.getAddressLine1(), regular, TextAlignment.LEFT, false))
                .add(createCell(address.getAddressLine2(), regular, TextAlignment.LEFT, false))
                .add(createCell(address.getCity(), regular, TextAlignment.LEFT, false))
                .add(createCell(address.getState() + " - " + address.getPincode(), regular, TextAlignment.LEFT, false));

        Cell restaurantColumn = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(createCell("On Behalf Of:", bold, TextAlignment.LEFT, false))
                .add(createLabelValueCell("Restaurant Name:", restaurant.getName(), bold, regular, TextAlignment.LEFT))
                .add(createLabelValueCell("Restaurant Contact:", restaurant.getContact(), bold, regular, TextAlignment.LEFT))
                .add(createCell("Restaurant Address:", bold, TextAlignment.LEFT, false))
                .add(createCell(restaurant.getAddress(), regular, TextAlignment.LEFT, false));

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

        invoice.getItems().forEach( orderItem -> {
            addRow(table, orderItem.getName() + " x " + orderItem.getQuantity(), String.valueOf(orderItem.getPrice()), regular, regular, null, false);
        });

        addRow(table, "Total", String.valueOf(invoice.getTotalAmount()), bold, regular, null,false);
        addRow(table, "Discount","- " + invoice.getDiscountAmount() , bold, regular, null, false);
        addRow(table, "CGST (2.5%)", String.valueOf(invoice.getCgstAmount()), bold, regular, null, false);
        addRow(table, "SGST (2.5%)", String.valueOf(invoice.getSgstAmount()), bold, regular, null, false);
        addRow(table, "Delivery Charge", String.valueOf(invoice.getDeliveryCharge()), bold, regular, null, false);
        addRow(table, "Final Amount", String.valueOf(invoice.getGrantTotalAmount()), bold, regular, null, false);

        doc.add(table.setMarginTop(10).setMarginBottom(20));

        doc.close();

        String fileName = REPORT_NAME + "-" + System.currentTimeMillis() + ".pdf";
        String filePath = REPORT_NAME + "/" + invoice.getCustomer().getId() + "/" + invoice.getInvoiceNumber() + "/";
        return bucketService.uploadFileFromStream(outputStream, fileName, filePath,"application/pdf");
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

    public static String formatOrderDate(String orderTimeStr) {
        if (orderTimeStr == null || orderTimeStr.isEmpty()) return "";

        Instant instant = Instant.parse(orderTimeStr); // e.g., "2025-04-20T15:45:00.000Z"
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        return zonedDateTime.format(OUTPUT_FORMATTER);
    }
}
