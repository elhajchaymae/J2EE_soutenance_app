package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.JourSoutenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface JourSoutenanceRepository extends JpaRepository<JourSoutenance, Long> {
    List<JourSoutenance> findByActifTrueOrderByDateAsc();
    Optional<JourSoutenance> findByDate(LocalDate date);
}
