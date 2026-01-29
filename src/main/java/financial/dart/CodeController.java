package financial.dart;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class CodeController {

    @Value("${dart.api-key}")
    private String apiKey;

    @Getter
    @Builder
    static class Corp {
        private String crp; // corp_code (짧게 줄임)
        private String nm;  // corp_name
        private String sc;  // stock_code
        private String md;  // modify_date
    }

    @GetMapping(value = "/code", produces = MediaType.TEXT_HTML_VALUE)
    public String getCode() {
        String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + apiKey;

        List<Corp> corpList = new ArrayList<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            byte[] response = restTemplate.getForObject(url, byte[].class);

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response))) {
                ZipEntry entry = zis.getNextEntry();
                if (entry != null) {
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zis);
                    NodeList nodes = doc.getElementsByTagName("list");

                    // 9만 건 전체 파싱
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element el = (Element) nodes.item(i);
                        corpList.add(Corp.builder()
                                .crp(el.getElementsByTagName("corp_code").item(0).getTextContent())
                                .nm(el.getElementsByTagName("corp_name").item(0).getTextContent().replace("'", "\\'"))
                                .sc(el.getElementsByTagName("stock_code").item(0).getTextContent().trim())
                                .md(el.getElementsByTagName("modify_date").item(0).getTextContent())
                                .build());
                    }
                }
            }

            // 데이터를 JSON으로 변환
            String jsonOutput = new ObjectMapper().writeValueAsString(corpList);
            return renderHtml(jsonOutput);

        } catch (Exception e) {
            return "<h1>데이터 로딩 실패: " + e.getMessage() + "</h1>";
        }
    }

    private String renderHtml(String jsonData) {
        return "<html><head>" +
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>" +
                "<link rel='stylesheet' href='https://cdn.datatables.net/1.13.4/css/dataTables.bootstrap5.min.css'>" +
                "<style>body { padding: 20px; } .container { max-width: 100%; }</style>" +
                "</head><body>" +
                "<div class='container-fluid'>" +
                "   <h2 class='mb-4'>DART 전체 고유번호 (" + jsonData.length() + " bytes 로딩됨)</h2>" +
                "   <table id='corpTable' class='table table-bordered table-sm'>" +
                "       <thead><tr><th>고유번호</th><th>기업명</th><th>종목코드</th><th>최종변경일</th></tr></thead>" +
                "   </table>" +
                "</div>" +
                "<script src='https://code.jquery.com/jquery-3.6.0.min.js'></script>" +
                "<script src='https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js'></script>" +
                "<script src='https://cdn.datatables.net/1.13.4/js/dataTables.bootstrap5.min.js'></script>" +
                "<script>" +
                "   const data = " + jsonData + ";" +
                "   $(document).ready(function() {" +
                "       $('#corpTable').DataTable({" +
                "           data: data," +
                "           deferRender: true," + // 핵심: 화면에 보이는 부분만 그립니다.
                "           columns: [" +
                "               { data: 'crp' }," +
                "               { data: 'nm', render: function(d){ return '<b>'+d+'</b>'; } }," +
                "               { data: 'sc' }," +
                "               { data: 'md' }" +
                "           ]," +
                "           pageLength: 50," +
                "           language: { search: '통합 검색:' }" +
                "       });" +
                "   });" +
                "</script>" +
                "</body></html>";
    }
}