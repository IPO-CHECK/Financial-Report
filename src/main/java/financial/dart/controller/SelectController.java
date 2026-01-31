package financial.dart.controller;

import financial.dart.domain.Financial;
import financial.dart.service.CorporationService;
import financial.dart.service.FinancialService;
import financial.dart.service.SimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SelectController {

    private final FinancialService financialService;
    private final CorporationService corporationService;
    private final SimilarityService similarityService;

    @GetMapping("/select")
    public void selectSimilarCorporations() {

        // TODO 신규 상장 종목의 재무제표를 가져와야 함.
        Financial targetFinancial = financialService.findByFinancialId(5L);
        log.info("신규 상장 종목: {}, 매출액={}, 자산총계={}, 자본총계={}",
                targetFinancial.getCorporation().getCorpName(),
                targetFinancial.getRevenue(),
                targetFinancial.getTotalAssets(),
                targetFinancial.getTotalEquity());

        // TODO 0. 분류 및 품목 필터링

        // 1. 상장 등록 후 3개월 경과, 최근 2년간 M&A 없음, 최근 2년간 감사의견 '적정'
        // TODO 로직이 제대로 동작 안 하는 것 같음.. 나중에 검토하기 or 빼기
        List<Long> corpIds = corporationService.findQualifiedCorpIds();

        // 2. 매출액, 자산총계, 자본총계 0.2배 ~ 5배 이내
        // TODO 신규 상장 종목의 상장 날짜가 Y년도 M분기인지 필요, 파라미터로 넘겨서 쿼리 where절에 추가
        List<Financial> financials = financialService.findSimilarCorporations(corpIds, targetFinancial, "2024", 1);

        for (Financial f : financials) {
            log.info("후보 종목: {}, 매출액={}, 자산총계={}, 자본총계={}",
                    f.getCorporation().getCorpName(),
                    f.getRevenue(),
                    f.getTotalAssets(),
                    f.getTotalEquity());
        }

        // TODO 3. 후보군 중 코사인 유사도 TOP 3개 선정, 어떻게 비교할 지 더 고민해야 함
        List<SimilarityService.SimilarityResult> top3Results = similarityService.findTopSimilarCorp(targetFinancial, financials, 3);

        String[] labels = {"매출증가율", "영업이익증가율", "순익증가율", "영업이익률", "순이익률", "자산회전율"};

        log.info("🎯 [타겟] {} : {}",
                targetFinancial.getCorporation().getCorpName(),
                formatVector(targetFinancial.getAnalysisVector(), labels));

        int rank = 1;
        for (SimilarityService.SimilarityResult res : top3Results) {
            double[] zScores = res.getVector(); // 정규화된 값
            double[] rawVector = res.getFinancial().getAnalysisVector(); // 원본 값

            String rawStr = formatVector(rawVector, labels);
            String zStr = formatVector(zScores, labels);

            log.info("🥈 TOP{} {} (점수: {})\n\t└─ 📊 Raw Data: {}\n\t└─ 📐 Z-Score : {}",
                    rank++,
                    res.getFinancial().getCorporation().getCorpName(),
                    String.format("%.4f", res.getScore()),
                    rawStr,
                    zStr);
        }
     }

    private String formatVector(double[] vec, String[] labels) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length && i < labels.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(labels[i]).append("=").append(String.format("%.4f", vec[i]));
        }
        sb.append("]");
        return sb.toString();
    }
 }
