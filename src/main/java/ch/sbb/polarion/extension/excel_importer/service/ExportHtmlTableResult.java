package ch.sbb.polarion.extension.excel_importer.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Represents the result of a html table export operation")
public class ExportHtmlTableResult {
    @Schema(description = "Base64-encoded excel file content")
    private String content;
}
