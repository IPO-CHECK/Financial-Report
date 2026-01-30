package financial.dart.controller;

import financial.dart.domain.Corporation;
import financial.dart.service.CorporationService;
import financial.dart.service.FinancialService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(defaultValue = "2024") String bsns_year
    ) {
        financialService.syncQuarterlyData(bsns_year);
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
