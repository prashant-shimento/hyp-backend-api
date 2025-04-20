package com.hyp.service;

import org.bson.Document;

import java.util.List;

public interface ReportPdfGenerator {
    String generatePdf(List<Document> reportData) throws Exception;
}
