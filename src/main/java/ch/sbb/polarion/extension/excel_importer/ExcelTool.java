package ch.sbb.polarion.extension.excel_importer;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.HtmlUtils;
import ch.sbb.polarion.extension.excel_importer.utils.ExportXlsRunnable;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.exporter.IExport;
import com.polarion.alm.tracker.exporter.IExportTemplate;
import com.polarion.alm.tracker.exporter.ITableExportConfiguration;
import com.polarion.alm.tracker.exporter.TableExportConfiguration;
import com.polarion.alm.tracker.model.IAttachmentBase;
import com.polarion.alm.tracker.model.IWithAttachments;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.subterra.base.data.identification.IContextId;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static ch.sbb.polarion.extension.excel_importer.utils.ExportXlsRunnable.*;

@SuppressWarnings({"unused", "java:S1118"})
public class ExcelTool {

    public static final String DOT_XLSX = ".xlsx";

    private static final Logger logger = Logger.getLogger(ExcelTool.class);

    @SneakyThrows
    public static String init() {

        @Language("HTML")
        String response = """
                           <script>
                               function exportHtmlTable(tableId, sheetName, fileName) {
                                   const formData = new FormData();
                                   formData.append("tableHtml", window.btoa(unescape(encodeURIComponent(document.getElementById(tableId).outerHTML))));
                                   formData.append("sheetName", sheetName || "");
                                   fetch("/polarion/excel-importer/rest/internal/excel-tool/export-html-table", {
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

    /**
     * Converts HTML table to Excel file and creates an attachment from this Excel file.
     *
     * @param htmlTable       A String containing exactly one HTML table
     * @param withAttachments Reference to the Object to add the attachment
     * @param fileName        The name of the created Excel file
     * @param fileTitle       The name of the attachment
     * @return Whether the operation was successful
     */
    @SuppressWarnings("rawtypes")
    public static boolean attachTable(@NotNull String htmlTable, @NotNull IWithAttachments withAttachments, @NotNull String fileName, @NotNull String fileTitle) {
        try {
            String excelFileName = fileName.toLowerCase().endsWith(DOT_XLSX) ? fileName : (fileName + DOT_XLSX);

            byte[] bytes = ExcelTool.convertHtmlTableToXlsx(Base64.getEncoder().encodeToString(htmlTable.getBytes(StandardCharsets.UTF_8)), null);

            try (InputStream xlsxStream = new ByteArrayInputStream(bytes)) {
                IAttachmentBase newAttachment = withAttachments.createAttachment(excelFileName, fileTitle, xlsxStream);
                TransactionalExecutor.executeInWriteTransaction(writeTransaction -> {
                    newAttachment.save();
                    return null;
                });
                return true;
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    /**
     * Converts HTML table to Excel file
     *
     * @param tableHtmlBase64Encoded a String containing exactly one HTML table
     * @param sheetName name of the sheet in the Excel file
     * @return byte array representing the Excel file
     */
    public static byte[] convertHtmlTableToXlsx(@NotNull String tableHtmlBase64Encoded, @Nullable String sheetName) {
        return (byte[]) BundleJarsPrioritizingRunnable.execute(
                ExportXlsRunnable.class, Map.of(
                        PARAM_SHEET_NAME, StringUtils.getEmptyIfNull(sheetName),
                        PARAM_CONTENT, tableHtmlBase64Encoded
                ), true).get(PARAM_RESULT);
    }

    /**
     * Create a table from the nested list
     *
     * @param dataList list of (rows = list of row elements)
     * @return an HTML table as String
     */
    @NotNull
    public static String getHTMLTable(@NotNull List<List<String>> dataList) {
        Document document = Jsoup.parse("<html><body></body></html>");
        Element body = document.body();

        Element table = body.appendElement("table");
        table.addClass("table-outlined");
        Element thead = table.appendElement("thead");
        Element tbody = table.appendElement("tbody");

        // build header
        List<String> headerRow = dataList.get(0);
        Element tr = thead.appendElement("tr");
        for (String value : headerRow) {
            Element th = tr.appendElement("th");
            th.addClass("header-td");
            th.text(value);
        }

        // build body
        for (int i = 1; i < dataList.size(); i++) {
            tr = tbody.appendElement("tr");
            List<String> rowData = dataList.get(i);
            for (String value : rowData) {
                Element td = tr.appendElement("td");
                td.addClass("row-td");
                td.text(value);
            }
        }

        return body.html() + HtmlUtils.CSS;
    }

    /**
     * Creates the export configuration for a table
     *
     * @param workItems list of objects (probably workitems)
     * @param query     search query
     * @param charset   charset
     * @param fields    list of fields
     * @param template  export template
     * @param params    additional parameters as a dictionary
     * @param contextId context
     * @return table export configuration
     */
    @NotNull
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ITableExportConfiguration getTableExportConfiguration(
            IPObjectList workItems, String query, String charset, List<?> fields, IExportTemplate template, Map<?, ?> params, IContextId contextId
    ) {
        TableExportConfiguration tableExportConfiguration = new TableExportConfiguration(workItems, query, charset, fields, template, params);
        tableExportConfiguration.setContextId(contextId);
        return tableExportConfiguration;
    }

    /**
     * Waits until IExport has status finished
     *
     * @param export         export process to be checked
     * @param timeoutSeconds expected timeout in seconds
     * @return true if export is finished within the expected timeout, otherwise -- false
     */
    @SneakyThrows
    public static boolean waitForExport(@NotNull IExport export, int timeoutSeconds) {
        int timer = timeoutSeconds;

        while (!export.isFinished()) {
            TimeUnit.SECONDS.sleep(1);
            timer--;
            if (timer == 0) {
                return false;
            }
        }

        return true;
    }

}
