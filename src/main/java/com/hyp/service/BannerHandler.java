package com.hyp.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;

public class BannerHandler extends AbstractPdfDocumentEventHandler {

    private final ImageData topImageData;
    private final ImageData bottomImageData;

    public BannerHandler(ImageData topImageData, ImageData bottomImageData) {
        this.topImageData = topImageData;
        this.bottomImageData = bottomImageData;
    }

    @Override
    protected void onAcceptedEvent(AbstractPdfDocumentEvent abstractPdfDocumentEvent) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) abstractPdfDocumentEvent;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();

        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
        Canvas canvas = new Canvas(pdfCanvas, pageSize);

        float pageWidth = pageSize.getWidth();
        float topMargin = 40;

        // Top Banner
        Image topImage = new Image(topImageData);
        topImage.scaleToFit(pageWidth, 1000);
        float topImageHeight = topImage.getImageScaledHeight();
        topImage.setFixedPosition(0, pageSize.getTop() - topImageHeight - topMargin);
        canvas.add(topImage);

        // Bottom Banner
        Image bottomImage = new Image(bottomImageData);
        bottomImage.scaleToFit(pageWidth, 1000);
        bottomImage.setFixedPosition(0, pageSize.getBottom());
        canvas.add(bottomImage);

        canvas.close();
        pdfCanvas.release();
    }
}
