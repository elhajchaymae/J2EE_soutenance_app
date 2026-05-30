package ma.ensah.soutenance.controller;

import lombok.RequiredArgsConstructor;
import ma.ensah.soutenance.service.ExcelExportService;
import ma.ensah.soutenance.service.PdfExportService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ======== Planning ========

    @GetMapping("/planning/excel")
    public ResponseEntity<byte[]> planningExcel() {
        try {
            byte[] data = excelExportService.exporterPlanningExcel();
            String filename = "planning_soutenances_" + LocalDate.now().format(DATE_FMT) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/planning/pdf")
    public ResponseEntity<byte[]> planningPdf() {
        try {
            byte[] data = pdfExportService.exporterPlanningPdf();
            String filename = "planning_soutenances_" + LocalDate.now().format(DATE_FMT) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ======== Matching ========

    @GetMapping("/matching/excel")
    public ResponseEntity<byte[]> matchingExcel() {
        try {
            byte[] data = excelExportService.exporterMatchingExcel();
            String filename = "matching_encadrants_" + LocalDate.now().format(DATE_FMT) + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/matching/pdf")
    public ResponseEntity<byte[]> matchingPdf() {
        try {
            byte[] data = pdfExportService.exporterMatchingPdf();
            String filename = "matching_encadrants_" + LocalDate.now().format(DATE_FMT) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
