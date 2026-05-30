package ma.ensah.soutenance.service;

import lombok.RequiredArgsConstructor;
import ma.ensah.soutenance.entity.*;
import ma.ensah.soutenance.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final SoutenanceRepository soutenanceRepository;
    private final EtudiantRepository etudiantRepository;
    private final JourSoutenanceRepository jourSoutenanceRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ======== Planning Excel ========

    public byte[] exporterPlanningExcel() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<JourSoutenance> jours = jourSoutenanceRepository.findByActifTrueOrderByDateAsc();

            for (JourSoutenance jour : jours) {
                List<Soutenance> soutenances = soutenanceRepository.findByDateOrderByHeureDebutAsc(jour.getDate());
                if (soutenances.isEmpty()) continue;

                XSSFSheet sheet = wb.createSheet(jour.getDate().format(DATE_FMT));
                creerEnTetePlanning(wb, sheet, jour.getDate());
                remplirPlanningJour(wb, sheet, soutenances);
                autoSizeColumns(sheet, 7);
            }

            // Feuille récap globale
            creerFeuillleRecap(wb);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void creerEnTetePlanning(XSSFWorkbook wb, XSSFSheet sheet, LocalDate date) {
        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PLANNING DES SOUTENANCES - " + date.format(DATE_FMT));
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        Row headerRow = sheet.createRow(2);
        String[] headers = {"Heure", "Étudiant", "Filière", "Sujet (résumé)", "Encadrant", "Président", "Examinateur", "Salle"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
    }

    private void remplirPlanningJour(XSSFWorkbook wb, XSSFSheet sheet, List<Soutenance> soutenances) {
        CellStyle dataStyle = createDataStyle(wb);
        CellStyle altStyle = createAltStyle(wb);

        int rowIdx = 3;
        for (int i = 0; i < soutenances.size(); i++) {
            Soutenance s = soutenances.get(i);
            Row row = sheet.createRow(rowIdx++);
            CellStyle style = (i % 2 == 0) ? dataStyle : altStyle;

            setCellValue(row, 0, s.getHeureDebut().format(HEURE_FMT) + " - " + s.getHeureFin().format(HEURE_FMT), style);
            setCellValue(row, 1, s.getEtudiant().getNomComplet(), style);
            setCellValue(row, 2, s.getEtudiant().getFiliere() != null ? s.getEtudiant().getFiliere().getCode() : "-", style);
            String sujet = s.getEtudiant().getSujetStage();
            setCellValue(row, 3, sujet.length() > 60 ? sujet.substring(0, 60) + "..." : sujet, style);
            setCellValue(row, 4, s.getEncadrant() != null ? s.getEncadrant().getNomComplet() : "-", style);
            setCellValue(row, 5, s.getPresident() != null ? s.getPresident().getNomComplet() : "-", style);
            setCellValue(row, 6, s.getExaminateur() != null ? s.getExaminateur().getNomComplet() : "-", style);
            setCellValue(row, 7, s.getSalle() != null ? s.getSalle().getNom() : "-", style);
        }
    }

    private void creerFeuillleRecap(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("Récapitulatif");
        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle = createDataStyle(wb);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RÉCAPITULATIF GÉNÉRAL DES SOUTENANCES");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        Row headerRow = sheet.createRow(2);
        String[] headers = {"Étudiant", "Filière", "Encadrant", "Sujet", "Date & Heure", "Salle"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        List<Soutenance> toutes = soutenanceRepository.findAll();
        toutes.sort(Comparator.comparing(Soutenance::getDate).thenComparing(Soutenance::getHeureDebut));

        int rowIdx = 3;
        for (Soutenance s : toutes) {
            Row row = sheet.createRow(rowIdx++);
            setCellValue(row, 0, s.getEtudiant().getNomComplet(), dataStyle);
            setCellValue(row, 1, s.getEtudiant().getFiliere() != null ? s.getEtudiant().getFiliere().getCode() : "-", dataStyle);
            setCellValue(row, 2, s.getEncadrant() != null ? s.getEncadrant().getNomComplet() : "-", dataStyle);
            setCellValue(row, 3, s.getEtudiant().getSujetStage(), dataStyle);
            setCellValue(row, 4, s.getDate().format(DATE_FMT) + " " + s.getHeureDebut().format(HEURE_FMT), dataStyle);
            setCellValue(row, 5, s.getSalle() != null ? s.getSalle().getNom() : "-", dataStyle);
        }
        autoSizeColumns(sheet, 5);
    }

    // ======== Matching Encadrants Excel ========

    public byte[] exporterMatchingExcel() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Affectation Encadrants");
            CellStyle titleStyle = createTitleStyle(wb);
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("AFFECTATION ÉTUDIANTS - ENCADRANTS");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row headerRow = sheet.createRow(2);
            String[] headers = {"CNE", "Étudiant", "Filière", "Sujet de Stage", "Entreprise", "Encadrant"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            List<Etudiant> etudiants = etudiantRepository.findAll();
            etudiants.sort(Comparator.comparing(e -> e.getFiliere() != null ? e.getFiliere().getCode() : "ZZZ"));

            int rowIdx = 3;
            for (Etudiant e : etudiants) {
                Row row = sheet.createRow(rowIdx++);
                setCellValue(row, 0, e.getCne(), dataStyle);
                setCellValue(row, 1, e.getNomComplet(), dataStyle);
                setCellValue(row, 2, e.getFiliere() != null ? e.getFiliere().getCode() : "-", dataStyle);
                setCellValue(row, 3, e.getSujetStage(), dataStyle);
                setCellValue(row, 4, Optional.ofNullable(e.getEntreprise()).orElse("-"), dataStyle);
                setCellValue(row, 5, e.getEncadrant() != null ? e.getEncadrant().getNomComplet() : "⚠ Non affecté", dataStyle);
            }
            autoSizeColumns(sheet, 5);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ======== Styles ========

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0x1a, (byte)0x56, (byte)0xdb}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xe1, (byte)0xec, (byte)0xff}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createAltStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xf8, (byte)0xf9, (byte)0xff}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private void setCellValue(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i <= count; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
