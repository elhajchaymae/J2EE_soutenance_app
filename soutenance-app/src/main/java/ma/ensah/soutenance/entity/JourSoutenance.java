package ma.ensah.soutenance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "jours_soutenance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourSoutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    private String description;

    private boolean actif = true;
}
