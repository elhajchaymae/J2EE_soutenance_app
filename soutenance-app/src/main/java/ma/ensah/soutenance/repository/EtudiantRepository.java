package ma.ensah.soutenance.repository;
import ma.ensah.soutenance.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    Optional<Etudiant> findByCne(String cne);
    List<Etudiant> findByFiliereId(Long filiereId);
    List<Etudiant> findByEncadrantIsNull();
    List<Etudiant> findByEncadrantId(Long encadrantId);
}
