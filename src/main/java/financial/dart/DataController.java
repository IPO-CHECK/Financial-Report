package financial.dart;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
public class DataController {

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

    @GetMapping(value = "/data", produces = MediaType.TEXT_HTML_VALUE)
    public String getAllFinancialData(
            @RequestParam(defaultValue = "00126380") String corp_code,
            @RequestParam(defaultValue = "2024") String bsns_year,
            @RequestParam(defaultValue = "11011") String reprt_code,
            @RequestParam(defaultValue = "CFS") String fs_div
    ) {
        String apiKey = "34fb88f333d40856fd64c1ced2f5b052feee2eeb";
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
            } catch (Exception e) { return val; }
        }
        return val;
    }
}
