package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    Optional<Salle> findByNomIgnoreCase(String nom);
    List<Salle> findByDisponibleTrue();
    @Query("SELECT s FROM Salle s WHERE s.disponible = true AND s.id NOT IN (SELECT so.salle.id FROM Soutenance so WHERE so.salle IS NOT NULL AND so.date = :date AND so.heureDebut < :heureFin AND so.heureFin > :heureDebut)")
    List<Salle> findSallesDisponibles(@Param("date") LocalDate date, @Param("heureDebut") LocalTime heureDebut, @Param("heureFin") LocalTime heureFin);
}
