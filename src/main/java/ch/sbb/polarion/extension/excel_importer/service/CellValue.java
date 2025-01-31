package ch.sbb.polarion.extension.excel_importer.service;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CellValue {
    private String text;
    private String link;
}
