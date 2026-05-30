package ma.ensah.soutenance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensah.soutenance.entity.*;
import ma.ensah.soutenance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de planification des soutenances.
 *
 * Contraintes respectées :
 *  - Un professeur ne peut pas avoir deux soutenances consécutives (pause d'au moins 1h)
 *  - Distribution équitable des créneaux de jury
 *  - Pas de conflit de salle (une salle = un seul créneau)
 *  - Pas de conflit professeur (un prof = un seul créneau simultané)
 *  - Jury composé de 3 membres : encadrant + président + examinateur
 *  - L'examinateur != encadrant, le président != encadrant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningService {

    private final EtudiantRepository etudiantRepository;
    private final SoutenanceRepository soutenanceRepository;
    private final SalleRepository salleRepository;
    private final ProfesseurRepository professeurRepository;
    private final MembreJuryRepository membreJuryRepository;
    private final JourSoutenanceRepository jourSoutenanceRepository;

    @Value("${app.soutenance.duree-minutes:60}")
    private int dureeMinutes;

    @Value("${app.soutenance.heure-debut:8}")
    private int heureDebut;

    @Value("${app.soutenance.heure-fin:18}")
    private int heureFin;

    public record PlanningResult(int planifiees, int echecs, List<String> erreurs, List<String> avertissements) {}

    @Transactional
    public PlanningResult genererPlanning() {
        // Supprimer l'ancien planning
        supprimerPlanningExistant();

        List<Etudiant> etudiants = etudiantRepository.findAll().stream()
                .filter(e -> e.getEncadrant() != null)
                .collect(Collectors.toList());

        List<JourSoutenance> jours = jourSoutenanceRepository.findByActifTrueOrderByDateAsc();
        List<Salle> salles = salleRepository.findByDisponibleTrue();
        List<Professeur> tousProfs = professeurRepository.findAll();

        List<String> erreurs = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();

        if (jours.isEmpty()) { return new PlanningResult(0, etudiants.size(), List.of("Aucun jour de soutenance défini"), avertissements); }
        if (salles.isEmpty()) { return new PlanningResult(0, etudiants.size(), List.of("Aucune salle disponible"), avertissements); }

        // Générer tous les créneaux disponibles
        List<Creneau> creneaux = genererCreneaux(jours, salles);
        log.info("{} créneaux disponibles pour {} étudiants", creneaux.size(), etudiants.size());

        // Tracker les créneaux occupés par prof
        Map<Long, Set<String>> occupationProf = new HashMap<>(); // profId -> set de "date_heureDebut"
        // Tracker créneaux salle
        Map<Long, Set<String>> occupationSalle = new HashMap<>(); // salleId -> set de "date_heureDebut"

        int planifiees = 0;
        int echecs = 0;

        // Mélanger pour répartir équitablement sur les jours
        Collections.shuffle(etudiants);

        for (Etudiant etudiant : etudiants) {
            boolean planifie = false;

            for (Creneau creneau : creneaux) {
                String cleHeure = creneau.date + "_" + creneau.heureDebut;
                String cleSuivante = creneau.date + "_" + creneau.heureDebut.plusMinutes(dureeMinutes);

                // Vérifier disponibilité salle
                if (occupationSalle.computeIfAbsent(creneau.salle.getId(), k -> new HashSet<>()).contains(cleHeure)) continue;

                // Vérifier disponibilité encadrant
                Professeur encadrant = etudiant.getEncadrant();
                if (encadrant == null) { echecs++; break; }

                Set<String> occupEncadrant = occupationProf.computeIfAbsent(encadrant.getId(), k -> new HashSet<>());
                if (occupEncadrant.contains(cleHeure)) continue;
                if (occupEncadrant.contains(cleSuivante)) continue; // pas deux consécutifs
                // Vérifier créneau précédent aussi
                String clePrecedente = creneau.date + "_" + creneau.heureDebut.minusMinutes(dureeMinutes);
                if (occupEncadrant.contains(clePrecedente)) continue;

                // Trouver président et examinateur
                Professeur president = trouverProfDisponible(tousProfs, encadrant, null, cleHeure, cleSuivante, clePrecedente, occupationProf);
                if (president == null) continue;

                Professeur examinateur = trouverProfDisponible(tousProfs, encadrant, president, cleHeure, cleSuivante, clePrecedente, occupationProf);
                if (examinateur == null) continue;

                // Planifier !
                Soutenance s = Soutenance.builder()
                        .etudiant(etudiant)
                        .salle(creneau.salle)
                        .date(creneau.date)
                        .heureDebut(creneau.heureDebut)
                        .heureFin(creneau.heureDebut.plusMinutes(dureeMinutes))
                        .statut(Soutenance.StatutSoutenance.PLANIFIEE)
                        .build();
                soutenanceRepository.save(s);

                // Créer membres jury
                List<MembreJury> jury = List.of(
                    MembreJury.builder().soutenance(s).professeur(encadrant).role(MembreJury.RoleJury.ENCADRANT).build(),
                    MembreJury.builder().soutenance(s).professeur(president).role(MembreJury.RoleJury.PRESIDENT).build(),
                    MembreJury.builder().soutenance(s).professeur(examinateur).role(MembreJury.RoleJury.EXAMINATEUR).build()
                );
                membreJuryRepository.saveAll(jury);
                s.setJury(jury);

                // Marquer occupés
                occupationSalle.get(creneau.salle.getId()).add(cleHeure);
                occupEncadrant.add(cleHeure);
                occupationProf.get(president.getId()).add(cleHeure);
                occupationProf.get(examinateur.getId()).add(cleHeure);

                planifiees++;
                planifie = true;
                break;
            }

            if (!planifie) {
                echecs++;
                avertissements.add("Impossible de planifier : " + etudiant.getNomComplet() + " (conflits non résolus)");
            }
        }

        if (!etudiants.stream().anyMatch(e -> e.getEncadrant() == null)) {
            avertissements.add("Note: " + etudiantRepository.findByEncadrantIsNull().size() + " étudiant(s) sans encadrant non planifiés.");
        }

        return new PlanningResult(planifiees, echecs, erreurs, avertissements);
    }

    private Professeur trouverProfDisponible(List<Professeur> profs, Professeur exclu1, Professeur exclu2,
                                               String cleHeure, String cleSuivante, String clePrecedente,
                                               Map<Long, Set<String>> occupationProf) {
        // Trier par charge totale (équité)
        return profs.stream()
                .filter(p -> !p.getId().equals(exclu1.getId()))
                .filter(p -> exclu2 == null || !p.getId().equals(exclu2.getId()))
                .filter(p -> {
                    Set<String> occ = occupationProf.computeIfAbsent(p.getId(), k -> new HashSet<>());
                    return !occ.contains(cleHeure) && !occ.contains(cleSuivante) && !occ.contains(clePrecedente);
                })
                .min(Comparator.comparingInt(p -> occupationProf.getOrDefault(p.getId(), new HashSet<>()).size()))
                .orElse(null);
    }

    private List<Creneau> genererCreneaux(List<JourSoutenance> jours, List<Salle> salles) {
        List<Creneau> creneaux = new ArrayList<>();
        for (JourSoutenance jour : jours) {
            LocalTime heure = LocalTime.of(heureDebut, 0);
            LocalTime fin = LocalTime.of(heureFin, 0);
            while (heure.plusMinutes(dureeMinutes).compareTo(fin) <= 0) {
                for (Salle salle : salles) {
                    creneaux.add(new Creneau(jour.getDate(), heure, salle));
                }
                heure = heure.plusMinutes(dureeMinutes);
            }
        }
        return creneaux;
    }

    @Transactional
    public void supprimerPlanningExistant() {
        membreJuryRepository.deleteAll();
        soutenanceRepository.deleteAll();
    }

    private record Creneau(LocalDate date, LocalTime heureDebut, Salle salle) {}

    // ======== Stats pour l'aperçu ========

    public Map<String, Object> getStatistiquesPlanning() {
        List<Soutenance> toutes = soutenanceRepository.findAll();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", toutes.size());
        stats.put("planifiees", toutes.stream().filter(s -> s.getStatut() == Soutenance.StatutSoutenance.PLANIFIEE).count());

        // Par jour
        Map<LocalDate, Long> parJour = toutes.stream().collect(Collectors.groupingBy(Soutenance::getDate, Collectors.counting()));
        stats.put("parJour", parJour);

        // Par salle
        Map<String, Long> parSalle = toutes.stream()
                .filter(s -> s.getSalle() != null)
                .collect(Collectors.groupingBy(s -> s.getSalle().getNom(), Collectors.counting()));
        stats.put("parSalle", parSalle);

        // Charge prof (nombre de jury)
        Map<String, Long> chargeProf = new LinkedHashMap<>();
        professeurRepository.findAll().forEach(p -> {
            long count = membreJuryRepository.countByProfesseurId(p.getId());
            chargeProf.put(p.getNomComplet(), count);
        });
        stats.put("chargeJury", chargeProf);

        return stats;
    }
}
