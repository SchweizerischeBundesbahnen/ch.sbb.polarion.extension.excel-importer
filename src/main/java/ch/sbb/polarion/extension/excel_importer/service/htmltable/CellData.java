package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import ch.sbb.polarion.extension.excel_importer.service.CellValue;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CellData {

    private boolean header;
    @Builder.Default
    private CellValue value = new CellValue();
    private CellConfig styles;
    private CellConfig attrs;

    public enum DataType {
        TEXT,
        LINK,
        IMAGE
    }

    public DataType getType() {
        return StyleUtil.getTypeForCell(this);
    }

}
