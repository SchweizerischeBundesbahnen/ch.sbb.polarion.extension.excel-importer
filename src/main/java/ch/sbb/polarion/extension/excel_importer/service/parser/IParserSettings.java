package ch.sbb.polarion.extension.excel_importer.service.parser;

import java.io.Serializable;
import java.util.Set;

public interface IParserSettings extends Serializable {

    String getSheetName();
    int getStartFromRow();
    Set<String> getUsedColumnsLetters();

}
