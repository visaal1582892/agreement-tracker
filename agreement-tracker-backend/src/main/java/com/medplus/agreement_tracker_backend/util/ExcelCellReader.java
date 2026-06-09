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

        String raw = readAsString(cell);
        if (raw.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean isBlank(Cell cell) {
        return readAsString(cell).isEmpty();
    }
}
