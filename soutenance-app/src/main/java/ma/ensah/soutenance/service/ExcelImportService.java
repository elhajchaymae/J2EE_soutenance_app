package ma.ensah.soutenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensah.soutenance.entity.*;
import ma.ensah.soutenance.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final FiliereRepository filiereRepository;
    private final ProfesseurRepository professeurRepository;
    private final SalleRepository salleRepository;
    private final EtudiantRepository etudiantRepository;
    private final JourSoutenanceRepository jourSoutenanceRepository;

    public record ImportResult(
        int filieres, int professeurs, int salles, int etudiants,
        int jours, List<String> erreurs, List<String> avertissements
    ) {}

    @Transactional
    public ImportResult importerFichierExcel(MultipartFile file) throws IOException {
        List<String> erreurs = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();
        int[] compteurs = new int[5]; // filieres, profs, salles, etudiants, jours

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String nomFeuille = sheet.getSheetName().toLowerCase().trim();
                log.info("Traitement feuille: {}", nomFeuille);

                try {
                    if (matcheFiliere(nomFeuille)) {
                        compteurs[0] += importerFilieres(sheet, erreurs, avertissements);
                    } else if (matcheProfesseur(nomFeuille)) {
                        compteurs[1] += importerProfesseurs(sheet, erreurs, avertissements);
                    } else if (matcheSalle(nomFeuille)) {
                        compteurs[2] += importerSalles(sheet, erreurs, avertissements);
                    } else if (matcheEtudiant(nomFeuille)) {
                        compteurs[3] += importerEtudiants(sheet, erreurs, avertissements);
                    } else if (matcheJour(nomFeuille)) {
                        compteurs[4] += importerJours(sheet, erreurs, avertissements);
                    } else {
                        avertissements.add("Feuille ignorée : " + sheet.getSheetName() + " (non reconnue)");
                    }
                } catch (Exception e) {
                    erreurs.add("Erreur feuille '" + sheet.getSheetName() + "': " + e.getMessage());
                    log.error("Erreur import feuille {}", sheet.getSheetName(), e);
                }
            }
        }

        return new ImportResult(compteurs[0], compteurs[1], compteurs[2], compteurs[3], compteurs[4], erreurs, avertissements);
    }

    // ======== Détection automatique des feuilles ========

    private boolean matcheFiliere(String nom) {
        return nom.contains("filiere") || nom.contains("filière") || nom.contains("formation");
    }

    private boolean matcheProfesseur(String nom) {
        return nom.contains("prof") || nom.contains("enseignant") || nom.contains("encadrant") || nom.contains("jury");
    }

    private boolean matcheSalle(String nom) {
        return nom.contains("salle") || nom.contains("local") || nom.contains("amphi");
    }

    private boolean matcheEtudiant(String nom) {
        return nom.contains("etudiant") || nom.contains("étudiant") || nom.contains("stagiaire") || nom.contains("stage");
    }

    private boolean matcheJour(String nom) {
        return nom.contains("jour") || nom.contains("date") || nom.contains("planning") || nom.contains("calendrier");
    }

    // ======== Import Filières ========

    private int importerFilieres(Sheet sheet, List<String> erreurs, List<String> warns) {
        int count = 0;
        Map<String, Integer> headers = detecterHeaders(sheet.getRow(0));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || estLigneVide(row)) continue;
            try {
                String code = getCellString(row, getColIndex(headers, "code", 0));
                String nom = getCellString(row, getColIndex(headers, "nom", 1));
                String desc = getCellString(row, getColIndex(headers, "description", 2));

                if (code.isBlank() || nom.isBlank()) { warns.add("Ligne " + (r+1) + " filière ignorée: code ou nom vide"); continue; }

                Filiere f = filiereRepository.findByCode(code.toUpperCase())
                        .orElse(Filiere.builder().code(code.toUpperCase()).build());
                f.setNom(nom);
                f.setDescription(desc);
                filiereRepository.save(f);
                count++;
            } catch (Exception e) {
                erreurs.add("Filière ligne " + (r+1) + ": " + e.getMessage());
            }
        }
        return count;
    }

    // ======== Import Professeurs ========

    private int importerProfesseurs(Sheet sheet, List<String> erreurs, List<String> warns) {
        int count = 0;
        Map<String, Integer> headers = detecterHeaders(sheet.getRow(0));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || estLigneVide(row)) continue;
            try {
                String nom = getCellString(row, getColIndex(headers, "nom", 0));
                String prenom = getCellString(row, getColIndex(headers, "prenom", 1));
                String email = getCellString(row, getColIndex(headers, "email", 2));
                String grade = getCellString(row, getColIndex(headers, "grade", 3));
                String specialites = getCellString(row, getColIndex(headers, "specialite", 4));

                if (nom.isBlank()) { warns.add("Ligne " + (r+1) + " professeur ignorée: nom vide"); continue; }

                Professeur p = professeurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom, prenom)
                        .orElse(Professeur.builder().build());
                p.setNom(nom);
                p.setPrenom(prenom.isBlank() ? "-" : prenom);
                p.setEmail(email);
                p.setGrade(grade);
                p.setSpecialites(normaliserSpecialites(specialites));
                professeurRepository.save(p);
                count++;
            } catch (Exception e) {
                erreurs.add("Professeur ligne " + (r+1) + ": " + e.getMessage());
            }
        }
        return count;
    }

    // ======== Import Salles ========

    private int importerSalles(Sheet sheet, List<String> erreurs, List<String> warns) {
        int count = 0;
        Map<String, Integer> headers = detecterHeaders(sheet.getRow(0));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || estLigneVide(row)) continue;
            try {
                String nom = getCellString(row, getColIndex(headers, "nom", 0));
                String batiment = getCellString(row, getColIndex(headers, "batiment", 1));
                String capStr = getCellString(row, getColIndex(headers, "capacite", 2));
                String type = getCellString(row, getColIndex(headers, "type", 3));

                if (nom.isBlank()) { warns.add("Ligne " + (r+1) + " salle ignorée: nom vide"); continue; }

                Salle s = salleRepository.findByNomIgnoreCase(nom)
                        .orElse(Salle.builder().build());
                s.setNom(nom);
                s.setBatiment(batiment);
                s.setType(type.isBlank() ? "SALLE" : type.toUpperCase());
                s.setDisponible(true);
                try { s.setCapacite(Integer.parseInt(capStr)); } catch (Exception ignored) { s.setCapacite(30); }
                salleRepository.save(s);
                count++;
            } catch (Exception e) {
                erreurs.add("Salle ligne " + (r+1) + ": " + e.getMessage());
            }
        }
        return count;
    }

    // ======== Import Étudiants ========

    private int importerEtudiants(Sheet sheet, List<String> erreurs, List<String> warns) {
        int count = 0;
        Map<String, Integer> headers = detecterHeaders(sheet.getRow(0));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || estLigneVide(row)) continue;
            try {
                String cne = getCellString(row, getColIndex(headers, "cne", 0));
                String nom = getCellString(row, getColIndex(headers, "nom", 1));
                String prenom = getCellString(row, getColIndex(headers, "prenom", 2));
                String email = getCellString(row, getColIndex(headers, "email", 3));
                String sujet = getCellString(row, getColIndex(headers, "sujet", 4));
                String motsCles = getCellString(row, getColIndex(headers, "mots", 5));
                String entreprise = getCellString(row, getColIndex(headers, "entreprise", 6));
                String codeFiliere = getCellString(row, getColIndex(headers, "filiere", 7));

                if (nom.isBlank() || sujet.isBlank()) { warns.add("Ligne " + (r+1) + " étudiant ignoré: nom ou sujet vide"); continue; }

                Etudiant e = cne.isBlank() ? new Etudiant() : etudiantRepository.findByCne(cne).orElse(new Etudiant());
                e.setCne(cne.isBlank() ? "AUTO-" + UUID.randomUUID().toString().substring(0, 8) : cne);
                e.setNom(nom);
                e.setPrenom(prenom.isBlank() ? "-" : prenom);
                e.setEmail(email);
                e.setSujetStage(sujet);
                e.setMotsClesStage(motsCles);
                e.setEntreprise(entreprise);

                if (!codeFiliere.isBlank()) {
                    filiereRepository.findByCode(codeFiliere.toUpperCase())
                            .or(() -> filiereRepository.findByNomIgnoreCase(codeFiliere))
                            .ifPresent(e::setFiliere);
                }
                etudiantRepository.save(e);
                count++;
            } catch (Exception ex) {
                erreurs.add("Étudiant ligne " + (r+1) + ": " + ex.getMessage());
            }
        }
        return count;
    }

    // ======== Import Jours ========

    private int importerJours(Sheet sheet, List<String> erreurs, List<String> warns) {
        int count = 0;
        Map<String, Integer> headers = detecterHeaders(sheet.getRow(0));

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || estLigneVide(row)) continue;
            try {
                Cell dateCell = row.getCell(getColIndex(headers, "date", 0));
                if (dateCell == null) continue;

                LocalDate date = null;
                if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    date = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    String dateStr = getCellString(row, 0);
                    if (!dateStr.isBlank()) {
                        date = LocalDate.parse(dateStr.trim());
                    }
                }

                if (date == null) continue;
                String desc = getCellString(row, getColIndex(headers, "description", 1));

                JourSoutenance j = jourSoutenanceRepository.findByDate(date)
                        .orElse(JourSoutenance.builder().date(date).build());
                j.setActif(true);
                j.setDescription(desc);
                jourSoutenanceRepository.save(j);
                count++;
            } catch (Exception e) {
                erreurs.add("Jour ligne " + (r+1) + ": " + e.getMessage());
            }
        }
        return count;
    }

    // ======== Utilitaires ========

    private Map<String, Integer> detecterHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (Cell cell : headerRow) {
            String val = getCellStringRaw(cell).toLowerCase().trim();
            map.put(val, cell.getColumnIndex());
        }
        return map;
    }

    private int getColIndex(Map<String, Integer> headers, String keyword, int defaultIndex) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().contains(keyword))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultIndex);
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return getCellStringRaw(cell);
    }

    private String getCellStringRaw(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean estLigneVide(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellStringRaw(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String normaliserSpecialites(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.replaceAll("[;|/]", ",").replaceAll("\\s*,\\s*", ",").trim();
    }
}
