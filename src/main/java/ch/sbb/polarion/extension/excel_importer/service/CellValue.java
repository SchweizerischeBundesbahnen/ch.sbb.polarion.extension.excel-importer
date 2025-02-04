package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellData;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CellValue {
    private String text;
    private String link;
    private byte[] image;

    public CellData.DataType getType() {
        if (image != null) {
            return CellData.DataType.IMAGE;
        }
        if (link != null) {
            return CellData.DataType.LINK;
        }
        return CellData.DataType.TEXT;
    }

    public Object getValue() {
        if (image != null) {
            return image;
        }
        return text;
    }
}
