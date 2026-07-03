package com.medplus.agreement_tracker_backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public final class JbpExcelValidationErrorAnnotator {

    public static final String VALIDATION_ERRORS_HEADER = "Validation Errors";

    private JbpExcelValidationErrorAnnotator() {
    }

    public static byte[] annotateWorkbook(Workbook workbook, Map<String, Map<Integer, String>> rowErrors) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Map<Integer, String> sheetErrors = rowErrors.get(sheet.getSheetName());
            if (sheetErrors == null || sheetErrors.isEmpty()) {
                continue;
            }
            annotateSheet(workbook, sheet, sheetErrors);
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write annotated JBP error workbook", ex);
        }
    }

    private static void annotateSheet(Workbook workbook, Sheet sheet, Map<Integer, String> sheetErrors) {
        int errorColumnIndex = resolveErrorColumnIndex(sheet);
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }

        Cell headerCell = headerRow.createCell(errorColumnIndex);
        headerCell.setCellValue(VALIDATION_ERRORS_HEADER);
        headerCell.setCellStyle(createErrorHeaderStyle(workbook));

        CellStyle errorStyle = createErrorBodyStyle(workbook);
        for (Map.Entry<Integer, String> entry : sheetErrors.entrySet()) {
            Row dataRow = sheet.getRow(entry.getKey());
            if (dataRow == null) {
                dataRow = sheet.createRow(entry.getKey());
            }
            Cell errorCell = dataRow.createCell(errorColumnIndex);
            errorCell.setCellValue(entry.getValue());
            errorCell.setCellStyle(errorStyle);
        }

        sheet.autoSizeColumn(errorColumnIndex);
    }

    private static int resolveErrorColumnIndex(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        int maxColumn = 0;
        if (headerRow != null) {
            for (int column = 0; column <= headerRow.getLastCellNum(); column++) {
                Cell cell = headerRow.getCell(column);
                if (cell != null) {
                    maxColumn = Math.max(maxColumn, column + 1);
                }
            }
        }
        return Math.max(maxColumn, 1);
    }

    private static CellStyle createErrorHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle createErrorBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        return style;
    }
}
