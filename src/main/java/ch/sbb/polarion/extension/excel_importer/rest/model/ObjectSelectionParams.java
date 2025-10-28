package ch.sbb.polarion.extension.excel_importer.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Parameters required to select an object within a project")
public class ObjectSelectionParams {
    @Schema(description = "Type of the object (e.g., Work Item, Attachment)")
    private String objectType;

    @Schema(description = "Unique identifier of the object")
    private String objectId;
}
