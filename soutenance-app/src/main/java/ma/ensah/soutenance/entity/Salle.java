package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String batiment;

    private Integer capacite;

    private String type; // "SALLE", "AMPHI", "LABO"

    private boolean disponible = true;
}
