package ch.sbb.polarion.extension.excel_importer.rest.model.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Details of the import job including status and error message if any")
public class ImportJobDetails {

    @Schema(description = "Current status of the import job",
            example = "IN_PROGRESS",
            implementation = ImportJobStatus.class
    )
    private ImportJobStatus status;

    @Schema(description = "Error message if the import failed")
    private String errorMessage;
}
