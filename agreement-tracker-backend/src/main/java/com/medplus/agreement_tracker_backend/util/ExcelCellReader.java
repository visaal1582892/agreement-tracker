package com.medplus.agreement_tracker_backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;

public final class ExcelCellReader {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private ExcelCellReader() {}

    public static String readAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    public static BigDecimal readAsDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        return readAsFormattedDecimal(cell);
    }

    /**
     * Reads numeric values from Excel cells including formatted currency strings
     * (e.g. "1,00,000", "₹50,000.00") without calling getNumericCellValue().
     */
    public static BigDecimal readAsFormattedDecimal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        String rawStr = DATA_FORMATTER.formatCellValue(cell).replaceAll("[^0-9.]", "");
        if (rawStr.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(rawStr);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean hasNonBlankNonNumericContent(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return false;
        }
        String raw = readAsString(cell);
        if (raw.isEmpty()) {
            return false;
        }
        return readAsFormattedDecimal(cell) == null;
    }

    public static boolean isBlank(Cell cell) {
        return readAsString(cell).isEmpty();
    }
}
