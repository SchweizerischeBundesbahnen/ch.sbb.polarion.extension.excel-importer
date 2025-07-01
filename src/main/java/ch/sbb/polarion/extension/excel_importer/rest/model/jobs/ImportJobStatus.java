package ch.sbb.polarion.extension.excel_importer.rest.model.jobs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of the import job")
public enum ImportJobStatus {

    @Schema(description = "The import is currently in progress")
    IN_PROGRESS,

    @Schema(description = "The import has finished successfully")
    SUCCESSFULLY_FINISHED,

    @Schema(description = "The import has failed")
    FAILED,

    @Schema(description = "The import was cancelled")
    CANCELLED
}
