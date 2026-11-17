package ch.sbb.polarion.extension.excel_importer.service.parser;

import java.io.Serializable;
import java.util.Set;

public interface IParserSettings extends Serializable {

    String getSheetName();
    int getStartFromRow();
    /**
     * @return 1-based last row to import (inclusive), or {@code null} to import up to the end of the sheet
     */
    Integer getEndAtRow();
    Set<String> getUsedColumnsLetters();

}
