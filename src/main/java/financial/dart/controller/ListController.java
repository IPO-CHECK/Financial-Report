package financial.dart.controller;

import financial.dart.service.CorporationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ListController {

    @Value("${dart.api-key}")
    private String apiKey;

    private final CorporationService corporationService;
    private final RestTemplate restTemplate;

    @GetMapping("/list")
    public String checkCriteria(@RequestParam String corpCode) {
        // 1. [1번 기준] 상장 후 3개월 경과 여부 체크
        boolean isOldEnough = checkListingDate(corpCode); //

        // 2. [3번 기준] 최근 2년간 M&A(합병/분할) 이력 체크
        boolean hasNoMnA = checkMnAHistory(corpCode); //

        // 3. [2번 기준] 최근 2년간 감사의견 '적정' 여부 체크
        boolean auditOpinionOk = checkAuditOpinion(corpCode); //

        return String.format("상장 3개월 경과: %b, 2년간 M&A 없음: %b, 2년간 감사의견 적정 여부 : %b", isOldEnough, hasNoMnA, auditOpinionOk);
    }

    // [1번 기준 로직] 가장 오래된 공시일 찾기
    private boolean checkListingDate(String corpCode) {
        String url = UriComponentsBuilder.fromUriString("https://opendart.fss.or.kr/api/list.json")
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bgn_de", "19500101")
                .queryParam("pblntf_ty", "A")    // 핵심: 'A'는 사업/반기/분기보고서만 가져옵니다
                .queryParam("sort", "date")
                .queryParam("sort_mth", "asc")   // 옛날순
                .queryParam("page_count", "30")  // 넉넉하게 30건 정도 가져와서 검사
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<Map<String, String>> list = (List<Map<String, String>>) response.get("list");

        if (list != null && !list.isEmpty()) {
            String businessReportDt = null;

            // 리스트를 돌면서 보고서명에 "사업보고서"가 포함된 가장 첫 번째(가장 오래된) 날짜 추출
            for (Map<String, String> report : list) {
                String reportNm = report.get("report_nm");

                // "사업보고서"가 포함되어 있고, "기재정정" 등은 제외하고 싶다면 조건 추가 가능
                if (reportNm.contains("사업보고서")) {
                    businessReportDt = report.get("rcept_dt");
                    break; // 가장 오래된 것 하나만 찾으면 되므로 탈락
                }
            }

            // 사업보고서를 찾지 못했을 경우를 대비해 리스트의 첫 번째(분기/반기)라도 사용
            if (businessReportDt == null) {
                businessReportDt = list.get(0).get("rcept_dt");
            }

            LocalDate firstDate = LocalDate.parse(businessReportDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return firstDate.isBefore(LocalDate.now().minusMonths(3)); // 오늘 기준 3개월 전인지 확인
        }
        return false;
    }

    // [3번 기준 로직] 특정 코드로 2년치 공시 검색
    private boolean checkMnAHistory(String corpCode) {
        String twoYearsAgo = LocalDate.now().minusYears(2).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String[] targetDetails = {"B001", "B002", "B005", "C004"};

        for (String detail : targetDetails) {
            String url = UriComponentsBuilder.fromUriString("https://opendart.fss.or.kr/api/list.json")
                    .queryParam("crtfc_key", apiKey)
                    .queryParam("corp_code", corpCode)
                    .queryParam("bgn_de", twoYearsAgo)
                    .queryParam("pblntf_detail_ty", detail)
                    .toUriString();

            System.out.println("[Debug] 호출 URL: " + url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // 1. response 자체가 null인지 확인
            if (response != null && !"0".equals(String.valueOf(response.get("total_count")))) {
                List<Map<String, String>> reports = (List<Map<String, String>>) response.get("list");

                // 🌟 [핵심 수정] reports가 null이 아닌지 한 번 더 확인합니다!
                if (reports != null) {
                    for (Map<String, String> report : reports) {
                        String reportNm = report.get("report_nm");

                        if (reportNm != null && (reportNm.contains("합병") || reportNm.contains("분할") ||
                                reportNm.contains("양수") || reportNm.contains("양도"))) {

                            System.err.printf("[진짜 탈락] 보고서명: %s | 날짜: %s\n", reportNm, report.get("rcept_dt"));
                            return false;
                        }
                    }
                } else {
                    // total_count는 0이 아닌데 list가 null인 경우 로그 출력 (디버깅용)
                    System.out.println("[Warning] total_count가 존재하지만 list가 비어있습니다. (Corp: " + corpCode + ")");
                }
            }
        }
        return true;
    }

    // [Criterion 2] 최근 2년간 감사의견 '적정' 여부 체크
    private boolean checkAuditOpinion(String corpCode) {
        // 최근 2년치
        int currentYear = LocalDate.now().getYear();
        String[] years = {String.valueOf(currentYear - 1), String.valueOf(currentYear - 2)};

        for (String year : years) {
            String url = UriComponentsBuilder.fromUriString("https://opendart.fss.or.kr/api/accnutAdtorNmNdAdtOpinion.json")
                    .queryParam("crtfc_key", apiKey)
                    .queryParam("corp_code", corpCode)
                    .queryParam("bsns_year", year)
                    .queryParam("reprt_code", "11011") // 사업보고서 고정
                    .toUriString();

            try {
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                // 1. 응답 데이터 존재 확인 (NPE 방어)
                if (response != null && response.get("list") != null) {
                    List<Map<String, String>> list = (List<Map<String, String>>) response.get("list");

                    if (!list.isEmpty()) {
                        // 2. adt_opinion 필드 추출
                        String opinion = list.get(0).get("adt_opinion");

                        if (opinion == null || !opinion.contains("적정")) {
                            System.err.println(year + "년 감사의견 부적정 발견: " + opinion);
                            return false; // "적정"이 아니면 즉시 탈락
                        }
                    } else {
                        // 데이터가 아예 없는 경우 (상장한 지 얼마 안 된 경우 등)
                        System.out.println(year + "년 감사의견 데이터 없음");
                    }
                }
            } catch (Exception e) {
                System.err.println("API 호출 중 오류 발생: " + e.getMessage());
            }
        }
        return true; // 2년 모두 적정이면 통과
    }
}