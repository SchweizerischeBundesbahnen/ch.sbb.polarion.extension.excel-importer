package ch.sbb.polarion.extension.excel_importer.service.parser.impl;

import ch.sbb.polarion.extension.excel_importer.service.parser.IParser;
import ch.sbb.polarion.extension.excel_importer.service.parser.IParserSettings;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class XlsxParser implements IParser {

    private static final String LAST_COLUMN_LITERAL = "Z";

    @Override
    @SneakyThrows
    @SuppressWarnings("java:S1166")
    public List<Map<String, Object>> parseFileStream(InputStream inputStream, IParserSettings parserSettings) {
        List<Map<String, Object>> result = new ArrayList<>();

        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (NotOfficeXmlFileException | POIXMLException e) {
            throw new IllegalArgumentException("File isn't an xlsx: " + e.getMessage(), e);
        }
        int sheetIndex = 0;
        String sheetName = parserSettings.getSheetName();
        if (!StringUtils.isEmpty(sheetName) && (sheetIndex = workbook.getSheetIndex(sheetName)) == -1) {
            throw new IllegalArgumentException(String.format("File doesn't contain sheet '%s'", sheetName));
        }
        XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
        Set<String> usedColumnsLetters = parserSettings.getUsedColumnsLetters();
        int rowNumber = 0;
        for (Row row : sheet) {
            if (++rowNumber < parserSettings.getStartFromRow()) {
                continue;
            }
            Map<String, Object> map = parseCellValue(row, usedColumnsLetters);
            if (map.values().stream().anyMatch(Objects::nonNull)) {
                //sometimes we get a row full of nulls - we have to skip it
                result.add(map);
            }
        }
        inputStream.close();

        return result;
    }

    @NotNull
    private Map<String, Object> parseCellValue(Row row, Set<String> usedColumnsLetters) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < CellReference.convertColStringToIndex(LAST_COLUMN_LITERAL); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String cellLetter = CellReference.convertNumToColString(cell.getColumnIndex());
            if (!usedColumnsLetters.contains(cellLetter)) {
                continue;
            }
            switch (cell.getCellType().equals(CellType.FORMULA) ? cell.getCachedFormulaResultType(): cell.getCellType()) {
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        map.put(cellLetter, cell.getDateCellValue());
                    } else {
                        map.put(cellLetter, cell.getNumericCellValue());
                    }
                }
                case BOOLEAN -> map.put(cellLetter, cell.getBooleanCellValue());
                case ERROR -> throw new IllegalArgumentException("%s%s contains bad/error value".formatted(cellLetter, (cell.getRowIndex() + 1)));
                default -> map.put(cellLetter, StringUtils.getNullIfEmpty(cell.getStringCellValue()));
            }
        }
        return map;
    }
}
