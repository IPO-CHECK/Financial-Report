package financial.dart.repository;

import financial.dart.domain.Corporation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CorporationRepository extends JpaRepository<Corporation, Long> {
    boolean existsByCorpCode(String corpCode);

    @Query("SELECT c FROM Corporation c WHERE c.stockCode is not null AND c.stockCode <> ''")
    List<Corporation> findCorps();
}
