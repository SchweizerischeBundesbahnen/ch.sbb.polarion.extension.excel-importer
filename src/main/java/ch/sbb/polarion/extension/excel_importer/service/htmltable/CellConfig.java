package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cascade properties for the cell. Used for easy access to the properties of parent row or the whole table.
 */
@Builder
public class CellConfig {

    private Map<String, String> table;
    private Map<String, String> row;
    private Map<String, String> self;

    public CellConfig(@NotNull Map<String, String> table, @NotNull Map<String, String> row, @NotNull Map<String, String> self) {
        this.table = table;
        this.row = row;
        this.self = self;
    }

    public Map<String, String> table() {
        return table;
    }

    public Map<String, String> row() {
        return row;
    }

    public Map<String, String> get() {
        return self;
    }

    public Map<String, String> merged() {
        return Stream.of(table, row, self)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> replacement // Overwrite existing value
                ));
    }
}
