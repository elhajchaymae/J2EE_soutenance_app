package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.MembreJury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MembreJuryRepository extends JpaRepository<MembreJury, Long> {
    List<MembreJury> findBySoutenanceId(Long soutenanceId);
    List<MembreJury> findByProfesseurId(Long professeurId);
    @Query("SELECT COUNT(mj) FROM MembreJury mj WHERE mj.professeur.id = :profId")
    long countByProfesseurId(@Param("profId") Long profId);
}
