package financial.dart.controller;

import financial.dart.domain.Corporation;
import financial.dart.service.CorporationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ListController {

    private final CorporationService corporationService;

    @GetMapping("/list")
    public void checkCriteria() throws InterruptedException {

        List<Corporation> corps = corporationService.getCorps();
        int total = corps.size();
        log.info("총 점검 대상 기업 수: {}", total);

        for (Corporation corp : corps) {
            // 1. [1번 기준] 상장 후 3개월 경과 여부 체크
            corporationService.checkListingDate(corp.getCorpCode());

            // 2. [2번 기준] 최근 2년간 감사의견 '적정' 여부 체크
            corporationService.checkAuditOpinion(corp.getCorpCode());

            // 3. [3번 기준] 최근 2년간 M&A(합병/분할) 이력 체크
            corporationService.checkMnAHistory(corp.getCorpCode());

            Thread.sleep(1000); // API 호출 간 1초 대기

            log.info("점검 완료: {} (남은 기업 수: {})", corp.getCorpName(), --total);
        }
    }
}