package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellData;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true)
@Setter
public class CellValue {
    private String text;
    private String link;
    private byte[] image;

    public CellData.DataType detectType() {
        if (image != null) {
            return CellData.DataType.IMAGE;
        }
        if (link != null) {
            return CellData.DataType.LINK;
        }
        return CellData.DataType.TEXT;
    }
}
