package ch.sbb.polarion.extension.excel_importer;

import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;

import java.util.Objects;

@SuppressWarnings({"unused", "java:S1118"})
public class ExcelTool {

    @SneakyThrows
    public static String init() {

        @Language("HTML")
        String response = """
                           <script>
                               function exportHtmlTable(tableId, sheetName, fileName) {
                                   const formData = new FormData();
                                   formData.append("tableHtml", window.btoa(unescape(encodeURIComponent(document.getElementById(tableId).outerHTML))));
                                   formData.append("sheetName", sheetName || "");
                                   fetch("/polarion/excel-importer/rest/internal/exportHtmlTable", {
                                       method: "POST",
                                       headers: {
                                           'Accept': 'application/json'
                                       },
                                       body: formData
                                   }).then(response =>
                                        response.json()
                                            .then(json => ({
                                                status: response.status,
                                                body: json
                                            }))
                                    ).then(({ status, body }) => {
                                        if (status === 200) {
                                            const link = document.createElement('a');
                                            link.href = 'data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,' + body.content;
                                            link.download = (fileName || "export") + ".xlsx";
                                            link.click();
                                        } else {
                                            alert(`Error: ${body?.message || "Cannot export html table. Please contact administrator."}`);
                                        }
                                   });
                               }
                           </script>
                """;
        return response;
    }

    public static String exportHtmlTable(String tableId) {
        return exportHtmlTable(tableId, null, null);
    }

    public static String exportHtmlTable(String tableId, String sheetName) {
        return exportHtmlTable(tableId, sheetName, null);
    }

    public static String exportHtmlTable(String tableId, String sheetName, String fileName) {
        return "exportHtmlTable('%s', '%s', '%s')".formatted(Objects.requireNonNull(tableId, "Table ID required"), StringUtils.getEmptyIfNull(sheetName), StringUtils.getEmptyIfNull(fileName));
    }
}
