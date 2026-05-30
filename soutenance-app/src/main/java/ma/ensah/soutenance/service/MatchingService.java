package ma.ensah.soutenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensah.soutenance.entity.*;
import ma.ensah.soutenance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de matching automatique étudiant ↔ encadrant.
 * Algorithme :
 *   1. Calculer score de compatibilité (mots-clés sujet vs spécialité prof)
 *   2. Distribuer équitablement la charge
 *   3. Affecter l'encadrant optimal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final EtudiantRepository etudiantRepository;
    private final ProfesseurRepository professeurRepository;

    public record MatchingResult(int affectations, int echecs, List<String> details) {}

    @Transactional
    public MatchingResult effectuerMatching() {
        List<Etudiant> etudiants = etudiantRepository.findByEncadrantIsNull();
        List<Professeur> professeurs = professeurRepository.findAll();

        if (professeurs.isEmpty()) {
            return new MatchingResult(0, etudiants.size(), List.of("Aucun professeur disponible"));
        }

        List<String> details = new ArrayList<>();
        int affectations = 0;
        int echecs = 0;

        // Charger current charge
        Map<Long, Integer> chargeActuelle = new HashMap<>();
        for (Professeur p : professeurs) {
            int charge = etudiantRepository.findByEncadrantId(p.getId()).size();
            chargeActuelle.put(p.getId(), charge);
        }

        // Trier étudiants par difficulté de matching (sujet plus spécifique = à traiter en premier)
        etudiants.sort(Comparator.comparingInt(e -> -getMotsClesCount(e)));

        for (Etudiant etudiant : etudiants) {
            Professeur meilleur = trouverMeilleurEncadrant(etudiant, professeurs, chargeActuelle);
            if (meilleur != null) {
                etudiant.setEncadrant(meilleur);
                etudiantRepository.save(etudiant);
                chargeActuelle.merge(meilleur.getId(), 1, Integer::sum);
                affectations++;
                details.add(String.format("✓ %s → Prof. %s (score spécialité + charge équitable)",
                        etudiant.getNomComplet(), meilleur.getNomComplet()));
            } else {
                echecs++;
                details.add(String.format("✗ %s → Aucun encadrant trouvé", etudiant.getNomComplet()));
            }
        }

        return new MatchingResult(affectations, echecs, details);
    }

    /**
     * Trouve le meilleur encadrant en combinant score spécialité et équité de charge.
     * Score final = (score_specialite * 0.6) + (score_charge * 0.4)
     */
    private Professeur trouverMeilleurEncadrant(Etudiant etudiant, List<Professeur> professeurs,
                                                  Map<Long, Integer> chargeActuelle) {
        int maxCharge = chargeActuelle.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        maxCharge = Math.max(maxCharge, 1);

        Professeur meilleur = null;
        double meilleurScore = -1;

        for (Professeur prof : professeurs) {
            double scoreSpecialite = calculerScoreSpecialite(etudiant, prof);
            int charge = chargeActuelle.getOrDefault(prof.getId(), 0);
            double scoreCharge = 1.0 - ((double) charge / (maxCharge + 1));

            double scoreFinal = (scoreSpecialite * 0.6) + (scoreCharge * 0.4);

            if (scoreFinal > meilleurScore) {
                meilleurScore = scoreFinal;
                meilleur = prof;
            }
        }

        return meilleur;
    }

    /**
     * Calcule le score de compatibilité sujet ↔ spécialité (0.0 à 1.0)
     */
    private double calculerScoreSpecialite(Etudiant etudiant, Professeur prof) {
        List<String> specialiteProf = prof.getListeSpecialites();
        if (specialiteProf.isEmpty()) return 0.3; // Professeur généraliste

        String sujetLower = (etudiant.getSujetStage() + " " +
                Optional.ofNullable(etudiant.getMotsClesStage()).orElse("")).toLowerCase();

        long matchs = specialiteProf.stream()
                .map(String::toLowerCase)
                .filter(sp -> sujetLower.contains(sp) || nettoyerTerse(sujetLower).contains(nettoyerTerste(sp)))
                .count();

        if (matchs == 0) {
            // Recherche partielle sur les mots
            matchs = specialiteProf.stream()
                    .flatMap(sp -> Arrays.stream(sp.toLowerCase().split("\\s+")))
                    .filter(mot -> mot.length() > 3 && sujetLower.contains(mot))
                    .count();
            return Math.min(0.5, matchs * 0.1);
        }

        return Math.min(1.0, 0.5 + (matchs * 0.25));
    }

    private String nettoyerTerse(String s) {
        return s.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String nettoyerTerste(String s) {
        return s.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private int getMotsClesCount(Etudiant e) {
        String mots = Optional.ofNullable(e.getMotsClesStage()).orElse("");
        return mots.split("[,;\\s]+").length;
    }

    /**
     * Réinitialise tous les encadrants pour refaire le matching.
     */
    @Transactional
    public void reinitialiserMatching() {
        List<Etudiant> tous = etudiantRepository.findAll();
        tous.forEach(e -> e.setEncadrant(null));
        etudiantRepository.saveAll(tous);
    }

    /**
     * Retourne les statistiques de distribution de charge.
     */
    public Map<String, Object> getStatistiquesCharge() {
        List<Professeur> profs = professeurRepository.findAll();
        Map<String, Integer> charges = new LinkedHashMap<>();
        for (Professeur p : profs) {
            charges.put(p.getNomComplet(), etudiantRepository.findByEncadrantId(p.getId()).size());
        }

        int total = charges.values().stream().mapToInt(Integer::intValue).sum();
        double moyenne = profs.isEmpty() ? 0 : (double) total / profs.size();
        int max = charges.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = charges.values().stream().mapToInt(Integer::intValue).min().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("charges", charges);
        stats.put("total", total);
        stats.put("moyenne", String.format("%.1f", moyenne));
        stats.put("max", max);
        stats.put("min", min);
        stats.put("equite", max - min <= 2 ? "Équitable ✓" : "Déséquilibré ⚠");
        return stats;
    }
}
