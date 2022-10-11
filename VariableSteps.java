/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import com.experian.automation.helpers.Variables;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import com.opencsv.CSVReader;
import io.cucumber.java.en.And;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

/**
 * The type Variable steps.
 */
public class VariableSteps {

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Variable steps.
   */
  public VariableSteps() {
    // Blank Constructor
  }

  /**
   * Save variable value.
   *
   * @param name  the name
   * @param value the value
   * @throws Throwable the throwable
   */
  @And("^I save variable (.*) with value (.*)$")
  public void saveVariableValue(String name, String value) throws Throwable {

    value = VariablesTransformer.transformSingleValue(value);

    Variables.set(name, value);
  }

  /**
   * Save variable content.
   *
   * @param name  the name
   * @param value the value
   * @throws Throwable the throwable
   */
  @And("^I save variable (.*) with content:$")
  public void saveVariableContent(String name, String value) throws Throwable {

    value = VariablesTransformer.transformSingleValue(value);

    saveVariableValue(name, value);
  }

 /**
   * Variable is an empty string.
   * @param name  the name
   */
  @And("^I save variable (.*) with no value")
  public void saveVariableWithNoValue(String name)  {

    Variables.set(name, "");
  }
  /**
   * Replace in variable content.
   *
   * @param search   the search
   * @param replace  the replace
   * @param variable the variable
   * @throws Throwable the throwable
   */
  @And("^I replace all occurrences of \"(.*)\" with \"(.*)\" in the value of variable (.*)$")
  public void replaceInVariableContent(String search, String replace, String variable) throws Throwable {

    replace = VariablesTransformer.transformSingleValue(replace);

    String value = Variables.get(variable).toString().replaceAll(search, replace);
    Variables.set(variable, value);
  }

  /**
   * Extract a substring from a given string based on the reqular expression defined
   *  This step will only captured single substring, which is the 1st that matches.
   *
   * @param newVar   the name of new variable to save the extracted data
   * @param origVar  the variable holding the original string
   * @param regex the regular expression to meet the string passed with the expected substring enclosed in a bracket
   */
  @And("^I save value from variable (.*) which matches regex (.*) as new variable (.*)$")
  public void extractFromStringWithRegex(String origVar, String regex, String newVar) {
    origVar = VariablesTransformer.transformSingleValue(origVar);

    logger.info("Original string: " + origVar);
    logger.info("Regex: " + regex);

    Pattern r = Pattern.compile(regex);
    Matcher m = r.matcher(origVar);

    String extractedString = "";
    if (m.find()) {
      extractedString = m.group(1);
    }
    Assert.assertTrue(!extractedString.isEmpty(),"Matched string with pattern '"+ regex + "'");

    Variables.set(newVar,extractedString);
    logger.info(newVar + " : " + Variables.get(newVar));
  }

  /**
   *  Count matches regex against a given string and save the count as variable
   *
   * @param newVar   the name of new variable to save the count
   * @param origVar  the variable holding the original string
   * @param regex the regular expression to be check against
   */
  @And("^I count number of matches of variable (.*) with regex (.*) and save as new variable (.*)$")
  public void countNumberOfMatchesForRegex(String origVar, String regex, String newVar) {
    origVar = VariablesTransformer.transformSingleValue(origVar);
    regex = VariablesTransformer.transformSingleValue(regex);

    logger.info(String.format("Original string: %s", origVar)); //NOI18N
    logger.info(String.format("Regex: %s", regex)); //NOI18N

    Pattern r = Pattern.compile(regex);
    Matcher m = r.matcher(origVar);
    int from = 0;
    Integer count = 0;
    while (m.find(from)) {
      count++;
      from = m.start() + 1; // handle overlap of regex
    }

    Variables.set(newVar, String.valueOf(count));
    logger.info(String.format("Regex match count save as variable %s: %s", newVar, Variables.get(newVar))); //NOI18N
  }

  /**
   * Trim variable value.
   *
   * @param variable the variable
   * @throws Throwable the throwable
   */
  @And("^I trim the value of variable (.*)$")
  public void trimVariableValue(String variable) throws Throwable {
    String value = Variables.get(variable).toString().trim();
    Variables.set(variable, value);
  }

  /**
   * Eval variable value.
   *
   * @param variable the variable
   * @throws Throwable the throwable
   */
  @And("^I calculate the value of variable (.*)$")
  public void evalVariableValue(String variable) throws Throwable {

    String value = Variables.get(variable).toString();

    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");

    String evalScript = String.format("eval(%s).toString()", value);
    String calcValue = engine.eval(evalScript).toString();

    Variables.set(variable, calcValue);
  }

  /**
   * Save variable value condition.
   *
   * @param valueToSave     the value to save
   * @param variableToSave  the variable to save
   * @param variableToCheck the variable to check
   * @param caseSensitivity the case sensitivity
   * @param condition       the condition
   * @param valueToCheck    the value to check
   */
  @And("^I save value \"?(.*)\"? to variable (.*) if (.*) is(?: (.*?) case)? (equal to|not equal to|less than|greater than|string containing string|string not containing string) (.*)$")
  public void saveVariableValueCondition(String valueToSave, String variableToSave, String variableToCheck,
      String caseSensitivity, String condition, String valueToCheck) {

    valueToSave = VariablesTransformer.transformSingleValue(valueToSave);
    variableToCheck = VariablesTransformer.transformSingleValue(variableToCheck);

    String actualValue = Variables.get(variableToCheck).toString();
    Double actualDouble;
    Double expectedDouble;
    if (StringUtils.isNotEmpty(caseSensitivity)) {
      actualValue = actualValue.toUpperCase();
      valueToCheck = valueToCheck.toUpperCase();
    }
    switch (condition) {
      case "equal to":
        if (valueToCheck.equals(actualValue)) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      case "not equal to":
        if (!valueToCheck.equals(actualValue)) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      case "less than":
        actualDouble = Double.parseDouble(actualValue);
        expectedDouble = Double.parseDouble(valueToCheck);
        if (actualDouble < expectedDouble) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      case "greater than":
        actualDouble = Double.parseDouble(actualValue);
        expectedDouble = Double.parseDouble(valueToCheck);
        if (actualDouble > expectedDouble) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      case "string containing string":
        if (valueToCheck.contains(actualValue)) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      case "string not containing string":
        if (!valueToCheck.contains(actualValue)) {
          Variables.set(variableToSave, valueToSave);
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported condition " + condition);
    }
  }

  /**
   * Verify variable value.
   *
   * @param name      the name
   * @param condition the condition
   * @param value     the value
   */
  @And("^I verify variable (.*) value is( different than| containing)?: (.*)$")
  public void verifyVariableValue(String name, String condition, String value) {

    value = VariablesTransformer.transformSingleValue(value);
    value = VariablesTransformer.normalizeNewLineSymbols(value);

    String actualValue = Variables.get(name);
    actualValue = VariablesTransformer.transformSingleValue(actualValue);
    actualValue = VariablesTransformer.normalizeNewLineSymbols(actualValue);

    if (StringUtils.isEmpty(condition)) { //no different, therefore expect same
      assertEquals(actualValue, value, "Value of variable " + name);
    } else if(condition.equals(" containing")) {
      assertTrue(actualValue.contains(value));
    } else {
      assertNotEquals(actualValue, value, "Value of variable " + name);
    }
  }

  /**
   * Reformat variable value with locale.
   *
   * @param variableName the variable name
   * @param locale       the locale
   * @throws Throwable the throwable
   */
/*
      Example call:
          And I re-format number variable accountID value to default locale
   */
  @And("I re-format number variable (.*) value to (.*) locale")
  public void reformatVariableValueWithLocale(String variableName, String locale) throws Throwable {
    Locale targetLocale;
    if (locale.equals("default")) {
      targetLocale = Locale.getDefault();
    } else {
      targetLocale = Locale.forLanguageTag("locale");
    }
    String valueToSave = NumberFormat.getInstance(targetLocale).format(
        Double.parseDouble(Variables.get(variableName)));
    Variables.set(variableName, valueToSave);
  }

  /**
   * Sort value list.
   *
   * @param type         the type
   * @param order        the order
   * @param variableName the variable name
   * @param delimiter    the delimiter
   * @param stringList   the string list
   * @throws Throwable the throwable
   */
  /*
   * And I sort numeric value list by ascending order and save result to variable expected using delimiter ,:
   *   | 3.14    |
   *   | 1       |
   *   | 22      |
   *   | 123     |
   *   | 100.123 |
   *   | 755     |
   */
  @And("^I sort (numeric|string) value list by (ascending|descending) order and save result to variable (.*) using delimiter (.*):")
  public void sortValueList(String type, String order, String variableName, String delimiter, List<String> stringList)
      throws Throwable {
    stringList = VariablesTransformer.transformList(stringList);

    List<Comparable> parsedList = new ArrayList<>();
    boolean ascendingOrder = order.equals("ascending");

    switch (type) {
      case "numeric":
        for (String value : stringList) {
          parsedList.add(new BigDecimal(value));
        }
        break;
      case "string":
        parsedList = (List<Comparable>) ((List) stringList);
        break;
      default:
        throw new RuntimeException("Unsupported value type : " + type);
    }

    if (ascendingOrder) {
      Collections.sort(parsedList);
    } else {
      Collections.sort(parsedList, Collections.reverseOrder());
    }
    String result = StringUtils.join(parsedList, delimiter);
    Variables.set(variableName, result);
  }

  /**
   * Verify variables equality.
   *
   * @param var1           the first variable
   * @param var2           the second variable
   * @param comparisonType the comparison type
   * @throws Throwable     the throwable
   */
  /*
   * Example usage:
   *  Then I verify variables ${decryptedString} and ${stringToEncrypt} are equal
   */
  @And("^I verify variables (.*) and (.*) are (equal|not equal)$")
  public void verifyVariablesEquality(String var1, String var2, String comparisonType) throws Throwable {
    var1 = VariablesTransformer.transformSingleValue(var1);
    var2 = VariablesTransformer.transformSingleValue(var2);

    final String NOT_EQUAL_ERROR_MESSAGE = String.format("The variable %s is equal to variable %s.", var1, var2);
    final String EQUAL_ERROR_MESSAGE = String.format("The variable %s not equal to variable %s.", var1, var2);

    if (comparisonType.equals("equal")) {
      assertEquals(var1, var2, EQUAL_ERROR_MESSAGE);
    } else {
      assertNotEquals(var1, var2, NOT_EQUAL_ERROR_MESSAGE);
    }
  }

  /**
   *  Verify a string variable matches a regular expression
   *
   *  Usage example(s):
   *    And I verify variable CSV Grid-20201231-094611-1.csv matches regex CSV Grid-.*-${jobId}\.csv
   *
   * @param var the variable to be verified
   * @param regex the regular expression string
   */
  @And("^I verify variable (.*) matches regex (.*)$")
  public void verifyVariableWithRegex(String var, String regex) {
    List<List<String>> table = new ArrayList<>();
    List<String> rowlist = new ArrayList<>();

    rowlist.add(var);
    rowlist.add(regex);
    table.add(rowlist);

    verifyListOfVariableWithRegex(table);
  }

  /**
   * Example usage:
   *  And I verify list of variables against regex:
   *       | ${streamClientId} | (.*)${cms.client.id}(.*)                                                                               |
   *       | ${bpsStartedLogs} | (.*)(Business Process Engine .* started successfully in .* seconds \(JVM running for .* seconds\))(.*) |
   *       | ${deploymentLogs} | (.*)Business Process Composition \[Business Process Composition.bpc.zip\] deployment completed(.*)     |
   *       | ${deploymentLogs} | (.*)Entity Model \[Business Process Composition.dbpdm.zip\] deployment completed(.*)                   |
   *       | ${deploymentLogs} | (.*)Strategy \[ace.jar\] deployment for all tenant completed(.*)                                       |
   *       | ${deploymentLogs} | (.*)Strategy \[ULD.jar\] deployment for all tenant completed(.*)                                       |
   *       | ${deploymentLogs} | (.*)Strategy \[Pre.jar\] deployment for all tenant completed(.*)                                       |
   *       | ${deploymentLogs} | (.*)Strategy \[Mia.jar\] deployment for all tenant completed(.*)                                       |
   *
   * Verify list of variables against regex.
   *
   * @param dataTable Variable vs regex (without header)
   */
  @And("^I verify list of variables against regex:$")
  public void verifyListOfVariableWithRegex(List<List<String>> dataTable) {
    // Notes: using list of list for datatable so that we can compare against the same variable repeatedly
    SoftAssert softAssert = new SoftAssert();

    for (int i = 0; i < dataTable.size(); i++) {

      String var = dataTable.get(i).get(0);
      String regex = dataTable.get(i).get(1);
      var = VariablesTransformer.transformSingleValue(var);
      regex = VariablesTransformer.transformSingleValue(regex);

      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(var);

      softAssert.assertTrue(
          matcher.matches(),
          String.format("Variable %s matches regex pattern %s", var, regex) //NOI18N
      );
    }
    softAssert.assertAll();
  }

  /**
   *  Modify a string variable matches a regular expression and save it to another variable
   *
   *  Usage example(s):
   *    And I modify variable ${dateTime} base on regex ([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}) and save group 1 to variable ${newDateTime}
   *
   * @param inputVariable the existing variable
   * @param regex the regular expression string
   * @param group the regular expression matched group
   * @param outputVariable the new variable
   */
  @And("^I modify variable (.*) base on regex (.*) and save group (.*) to variable (.*)$")
  public void modifyVariableWithRegex(String inputVariable, String regex, int group, String outputVariable) {
    inputVariable = VariablesTransformer.transformSingleValue(inputVariable);
    regex = VariablesTransformer.transformSingleValue(regex);

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(inputVariable);

    if (matcher.find())
    {
      Variables.set(outputVariable, matcher.group(group));
      logger.info("Output variable value: "+matcher.group(group));
    }
    else {
      throw new NoSuchElementException("Regex " + regex + " not found on variable " + inputVariable); // NOI18N
    }
  }

  /**
   * Sort multi line variable and save in ascending or descending order
   *
   * @param variable    Variable name
   * @param sortingOder order of sorting
   *                    <p>
   *                    Usage example(s):
   *                    And I sort multi lines variable myVariable in ascending order
   */

  @And("^I sort multi lines variable (.*) in (ascending|descending) order$")
  public void sortVariable(String variable, String sortingOder) {
    String variableValue = Variables.get(variable);
    List<String> variableList = Arrays.asList(variableValue.split("\\n")); //NOI18N
    if (sortingOder.equals("ascending")) { //NOI18N
      Collections.sort(variableList);
    } else {
      Collections.sort(variableList, Collections.reverseOrder());
    }
    String newValue = StringUtils.join(variableList, "\n"); //NOI18N
    Variables.set(variable, newValue);
  }

    /**
     * Reads CSV file using filter and saves columns data as variables
     * The CSV consist of three columns. The first column is the filter - based on the filterName it will pick only the matching filter
     * Second column is the variable name and third is the variable value - based on the matching filter the name and it's value will be saved
     * <p>
     * Example:
     * I set variables from file ${features.path}/BIServices/BITableauProjects/data/CA/applications_received/ca_applications_received_filters.csv by filter language-pt-br-<ViewAndFilters>
     *
     * @param filePath   the file path to the CSV file
     * @param filterName the filter name
     * @throws IOException the exception thrown if unable to read file from the provided file path
     */
    @And("^I set variables from file (.*) by filter (.*)$")
    public void setVariablesFromFileByFilter(String filePath, String filterName) throws IOException {
        filePath = VariablesTransformer.transformSingleValue(filePath);
        filterName = VariablesTransformer.transformSingleValue(filterName);

        try (
                CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))
        ) {
            String[] line;

            while ((line = reader.readNext()) != null) {

                String filter = line[0];
                String variableName = line[1];
                String variableValue = line[2];

                if (filter.equals(filterName)) {
                    Variables.set(variableName, variableValue);
                }
            }
        }
    }

}