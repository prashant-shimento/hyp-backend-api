package com.hyp.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;

public class BannerHandler implements IEventHandler {

    private final ImageData topImageData;
    private final ImageData bottomImageData;

    public BannerHandler(String topImagePath, String bottomImagePath) throws Exception {
        this.topImageData = ImageDataFactory.create(topImagePath);
        this.bottomImageData = ImageDataFactory.create(bottomImagePath);
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), docEvent.getDocument());
        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();

        Canvas layoutCanvas = new Canvas(page, pageSize);

        Image topImage = new Image(topImageData);
        topImage.scaleToFit(pageWidth, 1000);
        float topMargin = 40;
        float topImageHeight = topImage.getImageScaledHeight();
        topImage.setFixedPosition(0, pageSize.getTop() - topImageHeight - topMargin);
        layoutCanvas.add(topImage);

        Image bottomImage = new Image(bottomImageData);
        bottomImage.scaleToFit(pageWidth, 1000);
        float bottomImageHeight = bottomImage.getImageScaledHeight();
        bottomImage.setFixedPosition(0, pageSize.getBottom());
        layoutCanvas.add(bottomImage);

        layoutCanvas.close();
        canvas.release();
    }
}
