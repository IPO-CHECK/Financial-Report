package financial.dart.service;

import financial.dart.domain.Corporation;
import financial.dart.repository.CorporationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CorporationService {

    private final CorporationRepository corporationRepository;

    @Transactional
    public void saveCorporationData(List<Corporation> corporations) {
        try {
            corporationRepository.deleteAllInBatch();
            corporationRepository.saveAll(corporations);
        } catch (Exception e) {
            throw new RuntimeException("데이터 동기화 실패", e);
        }
    }


    public List<Corporation> getCorps() {
        return corporationRepository.findCorps();
    }
}
