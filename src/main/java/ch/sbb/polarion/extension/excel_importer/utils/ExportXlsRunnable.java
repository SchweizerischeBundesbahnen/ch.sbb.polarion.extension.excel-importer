package ch.sbb.polarion.extension.excel_importer.utils;

import ch.sbb.polarion.extension.excel_importer.service.ExportService;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;

import java.util.Map;

public class ExportXlsRunnable implements BundleJarsPrioritizingRunnable {

    public static final String PARAM_SHEET_NAME = "sheetName";
    public static final String PARAM_CONTENT = "content";
    public static final String PARAM_RESULT = "result";

    @Override
    public Map<String, Object> run(Map<String, Object> params) {
        String result = new ExportService().exportHtmlTable(
                (String) params.get(PARAM_SHEET_NAME),
                (String) params.get(PARAM_CONTENT)
        );
        return Map.of(PARAM_RESULT, result);
    }

}
