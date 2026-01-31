package financial.dart.repository;

import financial.dart.domain.Corporation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CorporationRepository extends JpaRepository<Corporation, Long> {
    boolean existsByCorpCode(String corpCode);

    @Query("SELECT distinct c " +
            "FROM Corporation c " +
            "join Financial f ON f.corporation.id = c.id " +
            "WHERE c.stockCode IS NOT NULL ")
    List<Corporation> findCorps();

    @Query("SELECT c FROM Corporation c WHERE c.corpCode = :corpCode")
    Corporation findByCorpCode(@Param("corpCode") String corpCode);

    @Query("SELECT c.id " +
            "FROM Corporation c " +
            "WHERE c.hasNoMajorChanges = true " +
            "and c.hasUnqualifiedOpinion = true " +
            "and c.isOver3Months = true ")
    List<Long> findQualifiedCorporationIds();
}