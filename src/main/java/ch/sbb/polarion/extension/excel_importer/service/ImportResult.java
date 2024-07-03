package ch.sbb.polarion.extension.excel_importer.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ImportResult {
    private List<String> updatedIds;
    private List<String> createdIds;
    private List<String> unchangedIds;

}
