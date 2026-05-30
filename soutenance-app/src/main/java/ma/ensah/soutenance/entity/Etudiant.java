package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etudiants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cne;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sujetStage;

    @Column(columnDefinition = "TEXT")
    private String motsClesStage; // pour le matching par spécialité

    private String entreprise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "encadrant_id")
    private Professeur encadrant;

    @OneToOne(mappedBy = "etudiant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Soutenance soutenance;

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
