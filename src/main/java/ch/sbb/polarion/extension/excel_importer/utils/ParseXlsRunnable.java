package ch.sbb.polarion.extension.excel_importer.utils;

import ch.sbb.polarion.extension.excel_importer.service.parser.IParserSettings;
import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class ParseXlsRunnable implements BundleJarsPrioritizingRunnable {

    public static final String PARAM_FILE_CONTENT = "fileContent";
    public static final String PARAM_SETTINGS = "settings";
    public static final String PARAM_RESULT = "result";

    @Override
    public Map<String, Object> run(Map<String, Object> params) {
        List<Map<String, Object>> result = new XlsxParser().parseFileStream(
                new ByteArrayInputStream((byte[]) params.get(PARAM_FILE_CONTENT)),
                (IParserSettings) params.get(PARAM_SETTINGS)
        );
        return Map.of(PARAM_RESULT, result);
    }

}
