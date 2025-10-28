package ch.sbb.polarion.extension.excel_importer.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Parameters required to attach a table to a project")
public class AttachTableParams extends ObjectSelectionParams {

    @Schema(description = "HTML representation of the table to attach")
    private String htmlTable;

    @Schema(description = "Name of the file to be created")
    private String fileName;

    @Schema(description = "Title of the file to be displayed")
    private String fileTitle;
}
