package ch.sbb.polarion.extension.excel_importer.service.parser;

import java.util.Set;

public interface IParserSettings {

    String getSheetName();
    int getStartFromRow();
    Set<String> getUsedColumnsLetters();

}
