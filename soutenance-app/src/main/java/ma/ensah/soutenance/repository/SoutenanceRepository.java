package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.Soutenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface SoutenanceRepository extends JpaRepository<Soutenance, Long> {
    List<Soutenance> findByDate(LocalDate date);
    List<Soutenance> findByDateOrderByHeureDebutAsc(LocalDate date);
    Optional<Soutenance> findByEtudiantId(Long etudiantId);
    List<Soutenance> findBySalleIdAndDate(Long salleId, LocalDate date);
    @Query("SELECT s FROM Soutenance s JOIN s.jury j WHERE j.professeur.id = :profId AND s.date = :date ORDER BY s.heureDebut")
    List<Soutenance> findByProfesseurAndDate(@Param("profId") Long profId, @Param("date") LocalDate date);
}
