package financial.dart.controller;

import financial.dart.domain.Financial;
import financial.dart.service.CorporationService;
import financial.dart.service.FinancialService;
import financial.dart.service.SimilarityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class SelectController {

    private final FinancialService financialService;
    private final CorporationService corporationService;
    private final SimilarityService similarityService;

    @GetMapping("/select")
    public void selectSimilarCorporations() {

        // TODO 신규 상장 종목
        Financial targetFinancial = financialService.findByFinancialId(10619L);

        // TODO 0. 분류 및 품목 필터링

        // 1. 상장 등록 후 3개월 경과, 최근 2년간 M&A 없음, 최근 2년간 감사의견 '적정'
        List<Long> corpIds1st = corporationService.findQualifiedCorpIds();

        // 2. 매출액, 자산총계, 자본총계 0.2배 ~ 5배 이내
        // TODO 신규 상장 종목의 상장 날짜가 Y년도 M분기인지 필요, 파라미터로 넘겨서 쿼리 where절에 추가
        List<Financial> financials = financialService.findSimilarCorporations(corpIds1st, null, null, null);

        // TODO 3. 코사인 유사도 TOP 3개 선정
        List<SimilarityService.SimilarityResult> top3Results = similarityService.findTopSimilarCorp(targetFinancial, financials, 3);

        int rank = 1;
        for (SimilarityService.SimilarityResult res : top3Results) {
            Financial f = res.getFinancial();
            double score = res.getScore(); // 1.0 만점

            System.out.printf("[%d위] %s (유사도: %.2f%%)\n",
                    rank++,
                    f.getCorporation().getCorpName(),
                    score * 100);

            // 검증용: 왜 뽑혔는지 벡터값 찍어보기
            System.out.println("   ㄴ 벡터: " + Arrays.toString(f.getAnalysisVector()));
        }
    }
}
