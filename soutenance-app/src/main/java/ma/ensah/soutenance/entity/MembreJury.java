package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membres_jury")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreJury {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "soutenance_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Soutenance soutenance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professeur_id", nullable = false)
    private Professeur professeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleJury role;

    public enum RoleJury {
        ENCADRANT, PRESIDENT, EXAMINATEUR
    }
}
