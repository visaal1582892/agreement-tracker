package com.medplus.agreement_tracker_backend.util;

public final class PriceOffExcelLayout {

    public static final String SHEET_NAME = "Price Off Campaigns";
    public static final String LISTS_SHEET = "ValidationLists";

    public static final String COL_PRODUCT_ID = "Product ID";
    public static final String COL_START_DATE = "Start Date";
    public static final String COL_DURATION_MONTHS = "Duration (Months)";
    public static final String COL_MAX_UNIT_CAP = "Max Unit Cap";
    public static final String COL_LOCATION = "Location";
    public static final String COL_CHANNEL = "Channel";
    public static final String COL_DISCOUNT_TYPE = "Discount Type";
    public static final String COL_CP = "CP";
    public static final String COL_MRP = "MRP";
    public static final String COL_BASE_OFFER = "Base Offer";
    public static final String COL_MEDPLUS_CONTRIBUTION = "Medplus Contribution";
    public static final String COL_FROM_QTY = "From Qty";
    public static final String COL_REMARKS = "Remarks";

    public static final int COL_START_DATE_INDEX = 1;
    public static final String EXCEL_DATE_FORMAT = "yyyy-MM-dd";

    public static final String PAN_INDIA = "Pan India";
    public static final String ALL_CHANNELS = "All";

    public static final int HEADER_ROW = 0;
    public static final int DATA_START_ROW = 1;
    public static final int MAX_DATA_ROWS = 2000;

    private PriceOffExcelLayout() {}
}
