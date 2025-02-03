package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CellData {

    private boolean header;
    private DataType type;
    private Object value;
    private String link;
    private CellConfig styles;
    private CellConfig attrs;

    public enum DataType {
        TEXT,
        LINK,
        IMAGE
    }

}
