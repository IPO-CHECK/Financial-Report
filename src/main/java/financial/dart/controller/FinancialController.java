package financial.dart.controller;

import financial.dart.service.FinancialService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;

    @Value("${dart.api-key}")
    private String apiKey;

    @GetMapping(value = "/data", produces = MediaType.TEXT_HTML_VALUE)
    public void getAllFinancialData(
            @RequestParam(defaultValue = "2023") String bsns_year) {
        financialService.syncQuarterlyData(bsns_year);
    }

    @Getter
    @Builder
    static class FinancialData {
        private String sj_div;      // 재무제표 구분 (BS, IS 등)
        private String sj_nm;       // 재무제표 명 (재무상태표 등)
        private String account_nm;  // 계정명 (자산총계 등)
        private String thstrm_amount; // 당기금액
        private String frmtrm_amount; // 전기금액
        private String bfefrmtrm_amount; // 전전기금액
        private String ord;         // 정렬순서
    }

    @GetMapping(value = "/search", produces = MediaType.TEXT_HTML_VALUE)
    public String getAllFinancialData(
            @RequestParam(defaultValue = "00126380") String corp_code,
            @RequestParam(defaultValue = "2024") String bsns_year,
            @RequestParam(defaultValue = "11011") String reprt_code,
            @RequestParam(defaultValue = "CFS") String fs_div
    ) {
        String url = String.format(
                "https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=%s&fs_div=%s",
                apiKey, corp_code, bsns_year, reprt_code, fs_div
        );

        RestTemplate restTemplate = new RestTemplate();
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, String>> list = (List<Map<String, String>>) response.get("list");

            if (list == null || list.isEmpty()) {
                return "<h3>데이터가 없거나 호출에 실패했습니다. (메시지: " + response.get("message") + ")</h3>";
            }

            return renderFullHtml(list, corp_code, bsns_year);
        } catch (Exception e) {
            return "<h1>에러 발생: " + e.getMessage() + "</h1>";
        }
    }

    @GetMapping(value = "/check/major", produces = MediaType.TEXT_HTML_VALUE)
    public String checkMajorAccount(
            @RequestParam(defaultValue = "00126380") String code, // 삼성전자
            @RequestParam(defaultValue = "2024") String year,
            @RequestParam(defaultValue = "11011") String report) { // 11011: 사업보고서

        RestTemplate restTemplate = new RestTemplate();

        // 1. 주요계정 API (fnlttSinglAcnt) 호출
        String url = "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json?crtfc_key=" + apiKey
                + "&corp_code=" + code
                + "&bsns_year=" + year
                + "&reprt_code=" + report;

        List<Map<String, Object>> list = new ArrayList<>();
        String status = "";
        String message = "";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                status = (String) response.get("status");
                message = (String) response.get("message");
                if (response.get("list") != null) {
                    list = (List<Map<String, Object>>) response.get("list");
                }
            }
        } catch (Exception e) {
            return "<h1>API 호출 실패: " + e.getMessage() + "</h1>";
        }

        // 2. HTML 렌더링
        return renderHtml(list, code, year, report, status, message);
    }

    private String renderHtml(List<Map<String, Object>> list, String code, String year, String report, String status, String msg) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>")
                .append("<style>body{padding:20px;} table{font-size:13px;} .th-custom{background:#f8f9fa;} .num{text-align:right;}</style>")
                .append("</head><body>")
                .append("<div class='container-fluid'>")
                .append("<h3 class='text-primary fw-bold'>🔎 주요계정(API 1번) 조회 결과</h3>")
                .append("<div class='alert alert-secondary'>")
                .append("기업코드: <b>").append(code).append("</b> / 연도: <b>").append(year).append("</b> / 보고서: <b>").append(report).append("</b><br>")
                .append("API 상태: ").append(status).append(" (").append(msg).append(")")
                .append("</div>");

        if (list.isEmpty()) {
            html.append("<h4 class='text-danger'>데이터가 없습니다. (리스트 비어있음)</h4>");
        } else {
            html.append("<table class='table table-bordered table-hover'>")
                    .append("<thead class='th-custom'><tr>")
                    .append("<th>구분(FS)</th>") // 연결/개별
                    .append("<th>재무제표(SJ)</th>") // BS/IS
                    .append("<th>계정명(NM)</th>") // 매출액, 영업이익 등
                    .append("<th>계정ID(account_id)</th>") // 중요! 이걸로 매핑해야 함
                    .append("<th>당기금액(3개월)</th>")
                    .append("<th>당기누적(Add)</th>")
                    .append("<th>전기금액(YoY)</th>")
                    .append("</tr></thead><tbody>");

            for (Map<String, Object> row : list) {
                html.append("<tr>")
                        // 연결(CFS)인지 개별(OFS)인지 확인
                        .append("<td><span class='badge bg-" + (row.get("fs_div").equals("CFS") ? "primary" : "secondary") + "'>")
                        .append(row.get("fs_div")).append("</span></td>")

                        // BS(재무상태표) / IS(손익계산서)
                        .append("<td>").append(row.get("sj_div")).append("</td>")

                        // 한글 계정명 (매출액, 자산총계 등)
                        .append("<td class='fw-bold'>").append(row.get("account_nm")).append("</td>")

                        // 계정 ID (매핑할 때 쓸 코드)
                        .append("<td class='text-danger'>").append(row.get("account_id")).append("</td>")

                        // 금액들
                        .append("<td class='num'>").append(format(row.get("thstrm_amount"))).append("</td>")
                        .append("<td class='num'>").append(format(row.get("thstrm_add_amount"))).append("</td>")
                        .append("<td class='num'>").append(format(row.get("frmtrm_amount"))).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private String format(Object val) {
        if (val == null || val.toString().trim().isEmpty() || val.toString().equals("-")) return "-";
        try {
            long v = Long.parseLong(val.toString().replace(",", ""));
            return String.format("%,d", v);
        } catch (Exception e) { return val.toString(); }
    }

    private String renderFullHtml(List<Map<String, String>> list, String corpCode, String year) {
        // 1. 모든 컬럼 키 가져오기 (가이드에 있는 모든 필드 대응)
        Object[] keys = list.get(0).keySet().toArray();

        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
                .append("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>")
                .append("<link rel='stylesheet' href='https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css'>")
                .append("<style>")
                .append("body{padding:20px; background-color:#f8f9fa;} ")
                .append(".container-fluid{background:white; padding:20px; border-radius:10px; box-shadow:0 0 10px rgba(0,0,0,0.1);}")
                .append("table{font-size: 12px;} th{white-space: nowrap;}") // 글자 크기 조절 및 줄바꿈 방지
                .append("</style>")
                .append("</head><body>")
                .append("<div class='container-fluid'>")
                .append("<h2 class='mb-4 text-primary'>DART 재무제표 무삭제 전체 데이터 [" + corpCode + "]</h2>")
                .append("<table id='fullTable' class='table table-bordered table-hover display nowrap' style='width:100%'>")
                .append("<thead class='table-light'><tr>");

        // 헤더 자동 생성
        for (Object key : keys) {
            html.append("<th>").append(key).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        // 데이터 행 생성
        for (Map<String, String> row : list) {
            html.append("<tr>");
            for (Object key : keys) {
                String value = String.valueOf(row.get(key));
                // 숫자 형태면 콤마 포맷팅, 아니면 그대로 출력
                html.append("<td>").append(formatIfNumber(value)).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</tbody></table></div>")
                .append("<script src='https://code.jquery.com/jquery-3.6.0.min.js'></script>")
                .append("<script src='https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js'></script>")
                .append("<script src='https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js'></script>")
                .append("<script>")
                .append("$(document).ready(function() {")
                .append("  $('#fullTable').DataTable({")
                .append("    scrollX: true,") // 가로 스크롤 활성화
                .append("    pageLength: 50,")
                .append("    dom: 'Bfrtip',")
                .append("    language: { search: '전체 필드 검색:' }")
                .append("  });")
                .append("});")
                .append("</script>")
                .append("</body></html>");

        return html.toString();
    }

    private String formatIfNumber(String val) {
        if (val == null || val.equals("null") || val.isEmpty()) return "-";
        // 숫자로만 이루어져 있거나 음수 기호가 있는 경우만 체크 (간단한 정규식)
        if (val.matches("-?\\d+")) {
            try {
                return String.format("%,d", Long.parseLong(val));
            } catch (Exception e) {
                return val;
            }
        }
        return val;
    }
}
