package ch.sbb.polarion.extension.excel_importer.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ImportJobParams {

    private String projectId;
    private String mappingName;
    private byte[] fileContent;

}
