package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "professeurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professeur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String email;

    private String grade; // PH, PA, MC, etc.

    @Column(columnDefinition = "TEXT")
    private String specialites; // ex: "IA,NLP,BigData" - stocké comma-separated

    // Charge max de jury (par défaut calculée automatiquement)
    private Integer chargeMaxJury;

    @OneToMany(mappedBy = "encadrant", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Etudiant> etudiants;

    @OneToMany(mappedBy = "professeur", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MembreJury> membresJury;

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public List<String> getListeSpecialites() {
        if (specialites == null || specialites.isBlank()) return List.of();
        return List.of(specialites.split(","));
    }
}
