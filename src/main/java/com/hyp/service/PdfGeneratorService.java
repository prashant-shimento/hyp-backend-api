package com.hyp.service;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PdfGeneratorService {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private BucketService bucketService;

	public String getReport(String reportName, List<Document> reportData) throws Exception {
		try {
			ReportPdfGenerator generator = (ReportPdfGenerator) context.getBean(reportName);
		    return generator.generatePdf(reportData);
		} catch (BeansException e) {
			return "No PDF generator component found for report: " + reportName + " Try JSON format";
		}
	}
}
