package com.hyp.service;

import java.util.List;
import org.bson.Document;

public interface ReportPdfGenerator {
    String generatePdf(List<Document> reportData) throws Exception;
}
