package ma.ensah.soutenance.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import lombok.RequiredArgsConstructor;
import ma.ensah.soutenance.entity.*;
import ma.ensah.soutenance.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final SoutenanceRepository soutenanceRepository;
    private final EtudiantRepository etudiantRepository;
    private final JourSoutenanceRepository jourSoutenanceRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final DeviceRgb COLOR_PRIMARY   = new DeviceRgb(0x1a, 0x56, 0xdb);
    private static final DeviceRgb COLOR_SECONDARY = new DeviceRgb(0xe1, 0xec, 0xff);
    private static final DeviceRgb COLOR_ALT_ROW   = new DeviceRgb(0xf8, 0xf9, 0xff);
    private static final DeviceRgb COLOR_WHITE      = new DeviceRgb(0xff, 0xff, 0xff);

    // ======== Planning PDF ========

    public byte[] exporterPlanningPdf() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());
        doc.setMargins(30, 30, 30, 30);

        ajouterEnteteGlobal(doc, "PLANNING DES SOUTENANCES", "École Nationale des Sciences Appliquées d'Al-Hoceima");

        List<JourSoutenance> jours = jourSoutenanceRepository.findByActifTrueOrderByDateAsc();
        for (JourSoutenance jour : jours) {
            List<Soutenance> soutenances = soutenanceRepository.findByDateOrderByHeureDebutAsc(jour.getDate());
            if (soutenances.isEmpty()) continue;

            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            ajouterTitreJour(doc, jour.getDate());

            Table table = new Table(UnitValue.createPercentArray(new float[]{10, 15, 8, 25, 13, 13, 13, 8}))
                    .useAllAvailableWidth();

            ajouterHeadersPlanning(table);

            for (int i = 0; i < soutenances.size(); i++) {
                Soutenance s = soutenances.get(i);
                DeviceRgb bgColor = (i % 2 == 0) ? COLOR_WHITE : COLOR_ALT_ROW;

                ajouterCellule(table, s.getHeureDebut().format(HEURE_FMT) + "-" + s.getHeureFin().format(HEURE_FMT), bgColor, false);
                ajouterCellule(table, s.getEtudiant().getNomComplet(), bgColor, false);
                ajouterCellule(table, s.getEtudiant().getFiliere() != null ? s.getEtudiant().getFiliere().getCode() : "-", bgColor, true);
                String sujet = s.getEtudiant().getSujetStage();
                ajouterCellule(table, sujet.length() > 55 ? sujet.substring(0, 55) + "…" : sujet, bgColor, false);
                ajouterCellule(table, s.getEncadrant() != null ? s.getEncadrant().getNomComplet() : "-", bgColor, false);
                ajouterCellule(table, s.getPresident() != null ? s.getPresident().getNomComplet() : "-", bgColor, false);
                ajouterCellule(table, s.getExaminateur() != null ? s.getExaminateur().getNomComplet() : "-", bgColor, false);
                ajouterCellule(table, s.getSalle() != null ? s.getSalle().getNom() : "-", bgColor, true);
            }

            doc.add(table);
            doc.add(new Paragraph("Nombre de soutenances : " + soutenances.size())
                    .setFontSize(9).setFontColor(ColorConstants.GRAY).setMarginTop(5));
        }

        doc.close();
        return out.toByteArray();
    }

    // ======== Matching PDF ========

    public byte[] exporterMatchingPdf() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(30, 30, 30, 30);

        ajouterEnteteGlobal(doc, "AFFECTATION ÉTUDIANTS - ENCADRANTS", "École Nationale des Sciences Appliquées d'Al-Hoceima");

        List<Etudiant> etudiants = etudiantRepository.findAll();
        etudiants.sort(Comparator.comparing(e -> e.getFiliere() != null ? e.getFiliere().getCode() : "ZZZ"));

        // Grouper par filière
        Map<String, java.util.List<Etudiant>> parFiliere = etudiants.stream()
                .collect(Collectors.groupingBy(e -> e.getFiliere() != null ? e.getFiliere().getNom() : "Sans filière",
                        LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, java.util.List<Etudiant>> entry : parFiliere.entrySet()) {
            doc.add(new Paragraph(entry.getKey())
                    .setFontSize(12).setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginTop(15).setMarginBottom(5));

            Table table = new Table(UnitValue.createPercentArray(new float[]{8, 20, 35, 22}))
                    .useAllAvailableWidth();

            // Headers
            String[] headers = {"CNE", "Étudiant", "Sujet de Stage", "Encadrant"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(9))
                        .setBackgroundColor(COLOR_PRIMARY)
                        .setFontColor(COLOR_WHITE)
                        .setPadding(5));
            }

            for (int i = 0; i < entry.getValue().size(); i++) {
                Etudiant e = entry.getValue().get(i);
                DeviceRgb bg = (i % 2 == 0) ? COLOR_WHITE : COLOR_ALT_ROW;

                ajouterCellule(table, e.getCne(), bg, false);
                ajouterCellule(table, e.getNomComplet(), bg, false);
                ajouterCellule(table, e.getSujetStage(), bg, false);
                String encadrantNom = e.getEncadrant() != null ? e.getEncadrant().getNomComplet() : "⚠ Non affecté";
                DeviceRgb encBg = e.getEncadrant() == null ? new DeviceRgb(0xff, 0xee, 0xee) : bg;
                ajouterCellule(table, encadrantNom, encBg, false);
            }
            doc.add(table);
        }

        // Footer stats
        long avecEncadrant = etudiants.stream().filter(e -> e.getEncadrant() != null).count();
        doc.add(new Paragraph(String.format("\nTotal : %d étudiants | Affectés : %d | Non affectés : %d",
                etudiants.size(), avecEncadrant, etudiants.size() - avecEncadrant))
                .setFontSize(9).setFontColor(ColorConstants.GRAY).setMarginTop(20));

        doc.close();
        return out.toByteArray();
    }

    // ======== Helpers ========

    private void ajouterEnteteGlobal(Document doc, String titre, String sousTitre) {
        doc.add(new Paragraph(sousTitre).setFontSize(10).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(titre).setFontSize(18).setBold().setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
        doc.add(new Paragraph("Généré le : " + LocalDate.now().format(DATE_FMT))
                .setFontSize(9).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT));
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginBottom(10));
    }

    private void ajouterTitreJour(Document doc, LocalDate date) {
        doc.add(new Paragraph("Soutenances du " + date.format(DATE_FMT))
                .setFontSize(14).setBold().setFontColor(COLOR_PRIMARY)
                .setBackgroundColor(COLOR_SECONDARY).setPadding(8).setMarginBottom(8));
    }

    private void ajouterHeadersPlanning(Table table) {
        String[] headers = {"Horaire", "Étudiant", "Filière", "Sujet", "Encadrant", "Président", "Examinateur", "Salle"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(8))
                    .setBackgroundColor(COLOR_PRIMARY)
                    .setFontColor(COLOR_WHITE)
                    .setPadding(4)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private void ajouterCellule(Table table, String valeur, DeviceRgb bgColor, boolean centre) {
        Cell cell = new Cell()
                .add(new Paragraph(valeur != null ? valeur : "-").setFontSize(8))
                .setBackgroundColor(bgColor)
                .setPadding(3);
        if (centre) cell.setTextAlignment(TextAlignment.CENTER);
        table.addCell(cell);
    }
}
