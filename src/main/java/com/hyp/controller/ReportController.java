package com.hyp.controller;

import com.hyp.dto.ReportDto;
import com.hyp.entity.Report;
import com.hyp.enums.ReportFormat;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.request.ReportRequest;
import com.hyp.response.Response;
import com.hyp.service.PdfGeneratorService;
import com.hyp.service.ReportService;
import com.hyp.translation.ReportTranslation;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController extends BaseController<ReportDto, Report, String> {

    @Autowired
    public ReportTranslation reportTranslation;

    @Autowired
    public ReportService reportService;

    @Autowired
    public PdfGeneratorService pdfGeneratorService;

    @PostMapping("/{reportId}/generate")
    public ResponseEntity<Response> generateReport(
            @PathVariable String reportId, @RequestBody @Valid ReportRequest reportRequest) throws Exception {
        Report report = Optional.ofNullable(reportService.findById(reportId))
                .orElseThrow(() -> new EntityNotFoundException(Report.class.getSimpleName(), reportId));
        List<Document> reportData = reportService.executeReport(report, reportRequest);
        if (reportRequest.getFormat().equals(ReportFormat.PDF)) {
            String reportUrl = pdfGeneratorService.getReport(report.getName(), reportData);
            return ResponseEntity.ok(
                    new Response(Collections.singletonList(reportUrl), false, "Report generated successfully."));
        }
        return ResponseEntity.ok(new Response(reportData, false, "Report generated successfully."));
    }
}
