package ch.sbb.polarion.extension.excel_importer.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Represents the result of an Excel import operation")
public class ImportResult {
    @Schema(description = "List of IDs that were updated during the import process")
    private List<String> updatedIds;

    @Schema(description = "List of IDs that were created during the import process")
    private List<String> createdIds;

    @Schema(description = "List of IDs that were unchanged during the import process")
    private List<String> unchangedIds;
}
