package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "soutenances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Soutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salle_id")
    private Salle salle;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime heureDebut;

    @Column(nullable = false)
    private LocalTime heureFin;

    @Enumerated(EnumType.STRING)
    private StatutSoutenance statut = StatutSoutenance.PLANIFIEE;

    @OneToMany(mappedBy = "soutenance", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<MembreJury> jury;

    public enum StatutSoutenance {
        PLANIFIEE, EN_COURS, TERMINEE, ANNULEE
    }

    // Helpers
    public Professeur getEncadrant() {
        return jury == null ? null : jury.stream()
                .filter(m -> m.getRole() == MembreJury.RoleJury.ENCADRANT)
                .map(MembreJury::getProfesseur)
                .findFirst().orElse(null);
    }

    public Professeur getPresident() {
        return jury == null ? null : jury.stream()
                .filter(m -> m.getRole() == MembreJury.RoleJury.PRESIDENT)
                .map(MembreJury::getProfesseur)
                .findFirst().orElse(null);
    }

    public Professeur getExaminateur() {
        return jury == null ? null : jury.stream()
                .filter(m -> m.getRole() == MembreJury.RoleJury.EXAMINATEUR)
                .map(MembreJury::getProfesseur)
                .findFirst().orElse(null);
    }
}
