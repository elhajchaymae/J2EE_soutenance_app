package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.Professeur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ProfesseurRepository extends JpaRepository<Professeur, Long> {
    Optional<Professeur> findByNomIgnoreCaseAndPrenomIgnoreCase(String nom, String prenom);
    List<Professeur> findBySpecialitesContainingIgnoreCase(String specialite);
    @Query("SELECT p FROM Professeur p LEFT JOIN p.membresJury mj GROUP BY p ORDER BY COUNT(mj) ASC")
    List<Professeur> findAllOrderByChargeAsc();
}
