/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

/**
 * Represents CSV cell value
 */
public class CSVCellInput {
    private String value;
    private Integer row;
    private Integer col;

    public CSVCellInput(String value, Integer row, Integer col) {
        this.value = value;
        this.row = row;
        this.col = col;
    }

    public String getValue() {
        return this.value;
    }

    public Integer getRow() {
        return this.row;
    }

    public Integer getCol() {
        return this.col;
    }
}
