package ma.ensah.soutenance.controller;

import lombok.RequiredArgsConstructor;
import ma.ensah.soutenance.repository.*;
import ma.ensah.soutenance.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ExcelImportService excelImportService;
    private final MatchingService matchingService;
    private final PlanningService planningService;
    private final EtudiantRepository etudiantRepository;
    private final ProfesseurRepository professeurRepository;
    private final SoutenanceRepository soutenanceRepository;
    private final SalleRepository salleRepository;
    private final FiliereRepository filiereRepository;
    private final JourSoutenanceRepository jourSoutenanceRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("nbEtudiants", etudiantRepository.count());
        model.addAttribute("nbProfs", professeurRepository.count());
        model.addAttribute("nbSoutenances", soutenanceRepository.count());
        model.addAttribute("nbSalles", salleRepository.count());
        model.addAttribute("nbFilieres", filiereRepository.count());
        model.addAttribute("nbJours", jourSoutenanceRepository.count());
        model.addAttribute("nbSansEncadrant", etudiantRepository.findByEncadrantIsNull().size());
        return "dashboard";
    }

    // ======== Import Excel ========

    @GetMapping("/import")
    public String importPage() {
        return "import";
    }

    @PostMapping("/import")
    public String importerFichier(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner un fichier Excel.");
            return "redirect:/import";
        }
        try {
            ExcelImportService.ImportResult result = excelImportService.importerFichierExcel(file);
            redirectAttributes.addFlashAttribute("importResult", result);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Import réussi : %d filières, %d professeurs, %d salles, %d étudiants, %d jours",
                            result.filieres(), result.professeurs(), result.salles(), result.etudiants(), result.jours()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'import : " + e.getMessage());
        }
        return "redirect:/import";
    }

    // ======== Matching ========

    @GetMapping("/matching")
    public String matchingPage(Model model) {
        model.addAttribute("stats", matchingService.getStatistiquesCharge());
        model.addAttribute("etudiants", etudiantRepository.findAll());
        return "matching";
    }

    @PostMapping("/matching/generer")
    public String genererMatching(RedirectAttributes redirectAttributes) {
        try {
            MatchingService.MatchingResult result = matchingService.effectuerMatching();
            redirectAttributes.addFlashAttribute("matchingResult", result);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Matching terminé : %d affectations réussies, %d échecs", result.affectations(), result.echecs()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur matching : " + e.getMessage());
        }
        return "redirect:/matching";
    }

    @PostMapping("/matching/reinitialiser")
    public String reinitialiserMatching(RedirectAttributes redirectAttributes) {
        matchingService.reinitialiserMatching();
        redirectAttributes.addFlashAttribute("success", "Matching réinitialisé.");
        return "redirect:/matching";
    }

    // ======== Planning ========

    @GetMapping("/planning")
    public String planningPage(Model model) {
        model.addAttribute("soutenances", soutenanceRepository.findAll()
                .stream().sorted(java.util.Comparator.comparing(s -> s.getDate().toString() + s.getHeureDebut().toString()))
                .toList());
        model.addAttribute("jours", jourSoutenanceRepository.findByActifTrueOrderByDateAsc());
        model.addAttribute("stats", planningService.getStatistiquesPlanning());
        return "planning";
    }

    @PostMapping("/planning/generer")
    public String genererPlanning(RedirectAttributes redirectAttributes) {
        try {
            PlanningService.PlanningResult result = planningService.genererPlanning();
            redirectAttributes.addFlashAttribute("planningResult", result);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Planning généré : %d soutenances planifiées, %d non planifiées",
                            result.planifiees(), result.echecs()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur planning : " + e.getMessage());
        }
        return "redirect:/planning";
    }

    @PostMapping("/planning/supprimer")
    public String supprimerPlanning(RedirectAttributes redirectAttributes) {
        planningService.supprimerPlanningExistant();
        redirectAttributes.addFlashAttribute("success", "Planning supprimé.");
        return "redirect:/planning";
    }

    // ======== Pages de données ========

    @GetMapping("/etudiants")
    public String etudiants(Model model) {
        model.addAttribute("etudiants", etudiantRepository.findAll());
        model.addAttribute("filieres", filiereRepository.findAll());
        return "etudiants";
    }

    @GetMapping("/professeurs")
    public String professeurs(Model model) {
        model.addAttribute("professeurs", professeurRepository.findAll());
        return "professeurs";
    }

    @GetMapping("/salles")
    public String salles(Model model) {
        model.addAttribute("salles", salleRepository.findAll());
        return "salles";
    }
}
