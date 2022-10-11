/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.core.api.TypeRegistry;
import io.cucumber.core.api.TypeRegistryConfigurer;
import io.cucumber.datatable.DataTableType;
import io.cucumber.datatable.TableEntryTransformer;

import java.util.Locale;
import java.util.Map;

/**
 * This class stands for custom mappings from data table entries.
 * Each new custom mapping should be added inside configureTypeRegistry as follows:
 *
 * typeRegistry.defineDataTableType(new DataTableType(Custom.class, new TableEntryTransformer<Custom>() {
 *             @Override
 *             public CSVCellInput transform(Map<String, String> entry) {
 *                 String customProp = VariablesTransformer.transformSingleValue(entry.get("prop"));
 *                 return new Custom(customProp);
 *             }
 *         }));
 *
 */

public class CustomDataTableTypesConfigurer implements TypeRegistryConfigurer {
    @Override
    public Locale locale() {
        return Locale.ENGLISH;
    }

    /**
     * Method containing all custom type datatable type mapping definitions
     * @param typeRegistry Contains mappings for custom datatable entries.
     */
    @Override
    public void configureTypeRegistry(TypeRegistry typeRegistry) {
        // The method defineDataTableType is called to register new custom mapping for in this case CSVCellInput
        typeRegistry.defineDataTableType(new DataTableType(CSVCellInput.class, new TableEntryTransformer<CSVCellInput>() {
            /**
             * This method is used to extract all custom properties values from single datatable entry in the correct format
             * needed for our custom class
             * @param entry represents single row values from datatable from Gherkin
             * @return returns the mapped value from entry to CSVCellInput
             */
            @Override
            public CSVCellInput transform(Map<String, String> entry) {
                String cellVall = VariablesTransformer.transformSingleValue(entry.get("value"));
                Integer row = Integer.parseInt(entry.get("row"));
                Integer col = Integer.parseInt(entry.get("col"));
                return new CSVCellInput(cellVall, row, col);
            }
        }));
    }
}
