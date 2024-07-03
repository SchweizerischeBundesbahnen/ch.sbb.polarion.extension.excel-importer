package ch.sbb.polarion.extension.excel_importer.service.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IParser {

    List<Map<String, Object>> parseFileStream(InputStream inputStream, IParserSettings parserSettings);

}
