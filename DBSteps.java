/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.Variables;
import com.experian.automation.helpers.databases.DBOperationsFactory;
import com.experian.automation.logger.Logger;
import com.experian.automation.steps.exceptions.DBStepException;
import com.experian.automation.transformers.VariablesTransformer;
import com.opencsv.CSVReader;
import io.cucumber.java.en.And;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.util.Strings;

/**
 * The type Db steps.
 */
@ConfigurationProperties(
    properties = {
        "database.driver",
        "database.url",
        "database.user",
        "database.password",
        "database.platform"
    }
)
public class DBSteps {
  private static final String QUERY_RESULT_MESSAGE = "Query result: %s";
  private static final String SET_STEP_DATA_MESSAGE = "Set step data %s = %s";

  private final Logger logger = Logger.getLogger(this.getClass());
  private DBOperationsFactory dbOperationsFactory;

  /**
   * Instantiates a new Db steps.
   *
   * @throws Exception the exception
   */
  public DBSteps() throws Exception {

    dbOperationsFactory = DBOperationsFactory.getDbOperationsFactoryInstance();
  }

  /**
   * Delete from table.
   *
   * @param table           the table
   * @param conditionColumn the condition column
   * @param operator        the operator
   * @param values          the values
   * @throws Exception the exception
   */
/*
    Usage example(s):

    And I execute DELETE FROM customers

    And I execute DELETE FROM customers WHERE customer_name IN 'Company4','Company5'

   */
  @And("^I execute DELETE FROM (.*?)(?: WHERE (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?$")
  public void deleteFromTable(String table, String conditionColumn,
      String operator, String values) throws Exception {

    table = VariablesTransformer.transformSingleValue(table);
    values = VariablesTransformer.transformSingleValue(values);

    dbOperationsFactory.operationsFactory().delete(table, conditionColumn, operator, values);
  }

  /**
   * Update table.
   *
   * @param table             the table
   * @param columnToSet       the column to set
   * @param valueToSet        the value to set
   * @param searchColumn      the search column
   * @param operator          the operator
   * @param searchColumnValue the search column value
   * @throws Exception the exception
   */
/*
   Usage example(s):

    And I execute UPDATE hades_members SET member_name='TestName0000'

    And I execute UPDATE hades_members SET member_name='TestName9878' WHERE member_egn IN 12312

  */
  @And("^I execute UPDATE (.*) SET (.*?)=(.*?)(?: WHERE (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?$")
  public void updateTable(String table, String columnToSet, String valueToSet,
      String searchColumn, String operator,
      String searchColumnValue) throws Exception {

    valueToSet = VariablesTransformer.transformSingleValue(valueToSet);
    searchColumn = VariablesTransformer.transformSingleValue(searchColumn);
    searchColumnValue = VariablesTransformer.transformSingleValue(searchColumnValue);

    dbOperationsFactory.operationsFactory().update(table, columnToSet, valueToSet, searchColumn,
                                                                         operator, searchColumnValue);
  }

  /**
   * Select list.
   *
   * @param columns                     the columns
   * @param table                       the table
   * @param typeOfJoin                  the type of join
   * @param joiningTable                the joining table
   * @param joinOnColumnCondition       the join on column condition
   * @param filterColumn                the filter column
   * @param operator                    the operator
   * @param filterColumnValue           the filter column value
   * @param additionalCondition         the additional condition
   * @param additionalConditionCol      the additional condition col
   * @param additionalConditionOperator the additional condition operator
   * @param additionalConditionVal      the additional condition val
   * @param groupByValues               the group by values
   * @param orderByValues               the order by values
   * @param ascDesc                     the asc desc
   * @param action                      the action
   * @param resultsNum                  the results num
   * @param expectedData                the expected data
   * @return the list
   * @throws Exception the exception
   */
/*
    Usage example(s):

    And I execute SELECT member_id,member_name,member_surname FROM hades_members and verify results for:
    | 1 | John    | Doe     |
    | 2 | Bruce   | Wayne   |
    | 4 | Clarke  | Kent    |

    And I execute SELECT a.member_name,a.member_surname FROM hades_members a WHERE a.member_id in (1,2,4) ORDER BY a.member_id DESC and verify results for:
    | 1 | John    | Doe     |
    | 2 | Bruce   | Wayne   |
    | 4 | Clarke  | Kent    |

    And I execute SELECT member_id,member_name,member_surname FROM hades_members GROUP BY member_id,member_name,member_surname and verify results for:
    | 6 | Test    | Test     |
    | 4 | John    | Doe      |
    | 5 | Bruce   | Wayne    |

    And I execute SELECT a.member_name,a.member_surname FROM hades_members a WHERE a.member_id in (1,2,3) AND a.member_name IN 'John' and verify results for:
    | John | Doe |

    And I execute SELECT member_id,member_name,member_surname FROM hades_members GROUP BY member_id,member_name,member_surname ORDER BY member_id and verify results for:
    | 4 | Test    | Test     |
    | 5 | John    | Doe      |
    | 6 | Bruce   | Wayne    |

    And I execute SELECT member_id,member_name,member_surname FROM hades_members GROUP BY member_id,member_name,member_surname ORDER BY member_id ASC and verify results for:
    | 4 | Test    | Test     |
    | 5 | John    | Doe      |
    | 6 | Bruce   | Wayne    |

    And I execute SELECT a.member_name,a.member_surname,a.member_egn FROM hades_members a INNER JOIN customers x ON a.member_id=x.customer_id WHERE x.customer_surname NOT IN 'Dobrev' and verify results for:
    | John    | Doe     | 2132142 |
    | Bruce   | Wayne   | 2132142 |
    | Clarke  | Kent    | 2132142 |

    And I execute SELECT A1.Store_Name STORES, SUM(A2.Sales) SALES FROM Geography2 A1 LEFT OUTER JOIN Store_Infromation2 A2 ON A1.Store_Name = A2.Store_Name GROUP BY A1.Store_Name and verify results for:
    | San Diego   | 250  |
    | Los Angeles | 1800 |
    | New York    | null |
    | Boston      | 700  |

    And I execute SELECT a.member_name FROM hades_members a WHERE a.member_id in (1,2,3) and save result as:
    | index | variable |
    | 1     | Test1    |
    | 2     | Test2    |
    | 3     | Test3    |

   */
  @And("^I execute SELECT (.*) FROM (.*?)" +
      "(?: (INNER JOIN|LEFT OUTER JOIN) (.*) ON (.*?))?" +
      "(?: WHERE (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: (AND|OR) (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: GROUP BY (.*?))?" +
      "(?: ORDER BY (.*?))?" +
      "(?: (ASC|DESC))? " +
      "and (verify results for|save results as)(?: first (\\d+) rows)?:$")
  public List<List<Object>> select(String columns,
      String table,
      String typeOfJoin, String joiningTable, String joinOnColumnCondition,
      String filterColumn, String operator, String filterColumnValue,
      String additionalCondition, String additionalConditionCol, String additionalConditionOperator,
      String additionalConditionVal,
      String groupByValues,
      String orderByValues, String ascDesc,
      String action, Integer resultsNum,
      List<List<String>> expectedData) throws Exception {

    columns = VariablesTransformer.transformSingleValue(columns);
    table = VariablesTransformer.transformSingleValue(table);
    filterColumnValue = VariablesTransformer.transformSingleValue(filterColumnValue);
    additionalConditionVal = VariablesTransformer.transformSingleValue(additionalConditionVal);
    groupByValues = VariablesTransformer.transformSingleValue(groupByValues);
    orderByValues = VariablesTransformer.transformSingleValue(orderByValues);

    expectedData = VariablesTransformer.transformTable(expectedData);

    List<List<Object>> queryResultTable = dbOperationsFactory.operationsFactory()
        .select(columns, table,
                typeOfJoin, joiningTable, joinOnColumnCondition,
                filterColumn, operator, filterColumnValue,
                additionalCondition, additionalConditionCol, additionalConditionOperator, additionalConditionVal,
                groupByValues, orderByValues, ascDesc, resultsNum);
    logger.info(String.format(QUERY_RESULT_MESSAGE, queryResultTable.toString()));

    if (action.equals("verify results for")) {
      assertEquals(queryResultTable.size(), expectedData.size(), "Query result row number");

      List<Class> dataTypes = extractDataTypesFromQueryResult(queryResultTable);

      List<List<Object>> expectedResult = dbOperationsFactory.operationsFactory().transformTable(expectedData,
                                                                                                 dataTypes);
      verifyQueryResultTable(queryResultTable, expectedResult);
    } else {

      List<String> queryResultsAsList = new ArrayList<>();
      for (int j = 0; j < queryResultTable.size(); j++) {
        queryResultsAsList.add(queryResultTable.get(j).get(0).toString());
      }

      for (int i = 1; i < expectedData.size(); i++) {
        List<String> dataTableRow = expectedData.get(i);

        int rowIndex = Integer.parseInt(dataTableRow.get(0)) - 1;
        String value = queryResultsAsList.get(rowIndex);
        String varName = dataTableRow.get(1);

        logger.info(String.format(SET_STEP_DATA_MESSAGE, varName, value));
        Variables.set(varName, value);
      }
    }

    return queryResultTable;
  }

  /**
   * Select and save.
   *
   * @param columns                     the columns
   * @param table                       the table
   * @param typeOfJoin                  the type of join
   * @param joiningTable                the joining table
   * @param joinOnColumnCondition       the join on column condition
   * @param filterColumn                the filter column
   * @param operator                    the operator
   * @param filterColumnValue           the filter column value
   * @param additionalCondition         the additional condition
   * @param additionalConditionCol      the additional condition col
   * @param additionalConditionOperator the additional condition operator
   * @param additionalConditionVal      the additional condition val
   * @param groupByValues               the group by values
   * @param orderByValues               the order by values
   * @param ascDesc                     the asc desc
   * @param resultNum                   the result num
   * @param savingVariable              the saving variable
   * @throws Exception the exception
   */
/*
    Usage example(s):

    And I execute SELECT a.member_name FROM hades_members a WHERE a.member_id in (1,2,3) and save 2nd result as Test2 variable

    And I execute SELECT a.member_name FROM hades_members a WHERE a.member_id in (1) AND member_surname IN 'Karakanovski' and save result as Test1 variable

   */
  @And("^I execute SELECT (.*) FROM (.*?)" +
      "(?: (INNER JOIN|LEFT OUTER JOIN) (.*) ON (.*?))?" +
      "(?: WHERE (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: (AND|OR) (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: GROUP BY (.*?))?" +
      "(?: ORDER BY (.*?))?" +
      "(?: (ASC|DESC))? " +
      "and save (?:(\\d+)(?:st|nd|rd|th) )?result as (.*) variable$")
  public void selectAndSave(String columns, String table,
      String typeOfJoin, String joiningTable, String joinOnColumnCondition,
      String filterColumn, String operator, String filterColumnValue,
      String additionalCondition, String additionalConditionCol, String additionalConditionOperator,
      String additionalConditionVal,
      String groupByValues,
      String orderByValues, String ascDesc, Integer resultNum,
      String savingVariable) throws Exception {

    columns = VariablesTransformer.transformSingleValue(columns);
    filterColumnValue = VariablesTransformer.transformSingleValue(filterColumnValue);
    additionalConditionVal = VariablesTransformer.transformSingleValue(additionalConditionVal);
    groupByValues = VariablesTransformer.transformSingleValue(groupByValues);
    orderByValues = VariablesTransformer.transformSingleValue(orderByValues);

    List<List<Object>> results = select(columns, table,
                                        typeOfJoin, joiningTable, joinOnColumnCondition,
                                        filterColumn, operator, filterColumnValue,
                                        additionalCondition, additionalConditionCol, additionalConditionOperator,
                                        additionalConditionVal,
                                        groupByValues, orderByValues, ascDesc,
                                        "", null, new ArrayList<>());

    List<String> queryResultsAsList = new ArrayList<>();
    for (int j = 0; j < results.size(); j++) {
      queryResultsAsList.add(results.get(j).get(0).toString());
    }

    String value = queryResultsAsList.get(0);
    if (resultNum != null) {
      value = queryResultsAsList.get(resultNum - 1);
    }

    logger.info(String.format(SET_STEP_DATA_MESSAGE, savingVariable, value));
    Variables.set(savingVariable, value);

  }

  /**
   * Execute general select list.
   *
   * @param query     the query
   * @param dataTable the data table
   * @return the list
   * @throws Exception the exception
   */
/*
   Usage example(s):

   And I execute general select query: SELECT member_id,member_name,member_surname FROM hades_members and verify results for:
     | 11 | TestName0000 | surname11    |
     | 12 | TestName0000 | surname12    |
     | 13 | TestName0000 | surname13    |
     | 14 | TestName0000 | surname14    |
     | 15 | TestName0000 | surname15    |
     | 16 | TestName0000 | surname16    |
     | 17 | TestName0000 | surname17    |
   */
  @And("^I execute general select query: (.*) and verify results for:$")
  public List<List<Object>> executeGeneralSelect(String query,
      List<List<String>> dataTable) throws Exception {

    query = VariablesTransformer.transformSingleValue(query);
    dataTable = VariablesTransformer.transformTable(dataTable);

    List<List<Object>> result = dbOperationsFactory.operationsFactory().executeGeneralSelect(query);
    logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));

    assertEquals(result.size(), dataTable.size(), "Query result row number");
    List<Class> dataTypes = extractDataTypesFromQueryResult(result);
    List<List<Object>> expectedResult = dbOperationsFactory.operationsFactory().transformTable(dataTable, dataTypes);

    verifyQueryResultTable(result, expectedResult);

    return result;
  }

  /**
   * Execute general select and save.
   *
   * @param query          the query
   * @param resultType     whether you want to save whole result or just first cell
   * @param savingVariable the saving variable
   * @throws Exception the exception
   */
  /*
   * Usage Example(s):
   *      And I execute general select query: SELECT TRANSACTIONUPDATES.VALUE FROM TRANSACTIONUPDATES INNER JOIN TRANSACTIONS ON TRANSACTIONS.ID = TRANSACTIONUPDATES.TRANSACTIONID WHERE TRANSACTIONS.ACCOUNTID IN ${accountReference} AND TRANSACTIONUPDATES.DICTIONARYID IN ${transactionDetailsID} ORDER BY TRANSACTIONS.ID DESC and save result as trrefrestB variable
   */
  @And("^I execute general select query: (.*) and save( whole)? result as (.*) variable$")
  public void executeGeneralSelectAndSave(String query, String resultType, String savingVariable)
      throws Exception {

    query = VariablesTransformer.transformSingleValue(query);
    String value;
    List<List<Object>> result = dbOperationsFactory.operationsFactory().executeGeneralSelect(query);
    logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));
    if (!Strings.isNullOrEmpty(resultType)) {
      StringBuilder strBil = new StringBuilder();
      String something;
      for (List<Object> resultList :
          result) {
        something = resultList.stream()
            .map(s -> s == null ? "null" : s.toString()) // NOI18N
            .collect(Collectors.joining(",")); //NOI18N
        strBil.append(something);
        strBil.append("\n");//NOI18N
      }
      value = strBil.toString();
    } else {
      value = result.get(0).get(0).toString();
    }

    Variables.set(savingVariable, value);
    logger.info(String.format(SET_STEP_DATA_MESSAGE, savingVariable, value));
  }

  /**
   * Select queries save results.
   *
   * @param dataTable the data table
   * @throws Throwable the throwable
   */
/*
    Usage example(s):

    And I execute multiple select queries and save results:
      | query                                                                                                                                       | variable        |
      | I execute SELECT member_id FROM hades_members WHERE member_surname IN 'Dobrev'                                                              | dobrev_id       |
      | I execute SELECT a.customer_name FROM customers a INNER JOIN hades_members b ON a.customer_id=b.member_id WHERE customer_surname IN 'Test1' | Test1           |
      | I execute SELECT member_id FROM hades_members WHERE member_surname IN 'Karakanovski'                                                        | karakanovski_id |
   */
  @And("^I execute multiple select queries and save results:$")
  public void selectQueriesSaveResults(List<List<String>> dataTable) throws Throwable {

    for (int i = 1; i < dataTable.size(); i++) {
      List<String> dataTableRow = dataTable.get(i);
      dataTableRow = VariablesTransformer.transformList(dataTableRow);

      String sentence = dataTableRow.get(0);
      sentence = sentence + " and save result as " + dataTableRow.get(1) + " variable";

      Pattern pattern = Pattern.compile("^I execute SELECT ([A-Za-z0-9_(), .*]*) FROM ([A-Za-z0-9_, ().='!]*?)" +
                                            "(?: (INNER JOIN|LEFT OUTER JOIN) ([A-Za-z0-9_, ]*) ON ([A-Za-z0-9_=.() ]*?))?" +
                                            "(?: WHERE ([%''A-Za-z0-9_=(). ]*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) ([%''A-Za-z0-9_=()&@. \\-!:]*?))?" +
                                            "(?: (AND|OR) ([A-Za-z0-9_]*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) ([%''A-Za-z0-9_=()&@. \\-!:]*?))?" +
                                            "(?: GROUP BY ([A-Za-z0-9_(), .]*?))?" +
                                            "(?: ORDER BY ([A-Za-z0-9_(), .]*?))?" +
                                            "(?: (ASC|DESC))? " +
                                            "and save (?:(\\d+)(?:st|nd|rd|th) )?result as ([A-Za-z0-9_]*) variable$");
      Matcher matcher = pattern.matcher(sentence);

      if (matcher.find()) {

        Integer rownum = null;
        if (matcher.group(16) != null) {
          rownum = Integer.parseInt(matcher.group(12));
        }

        selectAndSave(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5),
                      matcher.group(6), matcher.group(7), matcher.group(8)
            , matcher.group(9), matcher.group(10), matcher.group(11), matcher.group(12), matcher.group(13),
                      matcher.group(14), matcher.group(15), rownum, matcher.group(17));
      } else {
        throw new IllegalArgumentException("Illegal query syntax: " + sentence);
      }
    }
  }

  /**
   * Insert into.
   *
   * @param table   the table
   * @param columns the columns
   * @param values  the values
   * @throws Exception the exception
   */
/*
      Usage example(s):

      And I execute INSERT INTO hades_members COLUMNS member_id,member_name,member_surname,member_egn VALUES 17,'name17','surname17','2132132'

   */
  @And("^I execute INSERT INTO (.*) COLUMNS (.*) VALUES (.*)$")
  public void insertInto(String table, String columns,
      String values) throws Exception {

    table = VariablesTransformer.transformSingleValue(table);
    values = VariablesTransformer.transformSingleValue(values);

    int result = dbOperationsFactory.operationsFactory().insert(table, columns, values);

    assertEquals(result, 1, "Created records");
  }

  /**
   * Create records.
   *
   * @param tableName the table name
   * @param dataTable the data table
   * @throws Throwable the throwable
   */
/*
    Usage example(s):

    And I insert records into table hades_members:
      | member_id | member_name | member_surname | member_egn |
      | 11        | 'name11'    | 'surname11'    | '2132132'  |
      | 12        | 'name12'    | 'surname12'    | '3132132'  |
      | 13        | 'name13'    | 'surname13'    | '4132132'  |
      | 14        | 'name14'    | 'surname14'    | '5132132'  |
      | 15        | 'name15'    | 'surname15'    | '6132132'  |
      | 16        | 'name16'    | 'surname16'    | '7132132'  |

  */
  @And("^I insert records into table (.*):$")
  public void createRecords(String tableName, List<List<String>> dataTable) throws Throwable {

    dataTable = VariablesTransformer.transformTable(dataTable);

    String tableColumns = StringUtils.join(dataTable.get(0), ',');

    for (int j = 1; j < dataTable.size(); j++) {
      String values = "";
      for (int k = 0; k < dataTable.get(j).size(); k++) {
        values = values + dataTable.get(j).get(k) + ",";
      }
      values = StringUtils.removeEnd(values, ",");
      insertInto(tableName, tableColumns, values);
    }
  }

  /**
   * Execute general update query.
   *
   * @param query the query
   * @throws Throwable the throwable
   */
  @And("^I execute general update query: (.*)$")
  public void executeGeneralUpdateQuery(String query) throws Throwable {

    query = VariablesTransformer.transformSingleValue(query);

    dbOperationsFactory.operationsFactory().executeGeneralUpdate(query);
  }

  /**
   * Execute general update query for specific db platform.
   *
   * @param query the query
   * @param dbPlatform the database platform
   * @throws Throwable the throwable
   */
  @And("^I execute update query: (.*) if database.platform is (.*)$")
  public void executeGeneralUpdateQueryForDB(String query, String dbPlatform) throws Throwable {
    // execute only for the database
    if(!Variables.get("database.platform").equalsIgnoreCase(dbPlatform)) { // NOI18N
      return;
    }

    executeGeneralUpdateQuery(query);
  }

  /**
   * Create records using file.
   *
   * @param executingType  uses to specify if you are executing whole file or line by line
   * @param filePath the file path
   * @throws Throwable the throwable
   */
/*
    Usage example(s):

    And I update records using query from file with path: ${myVariable}/myfolder/myFile.ext

  */
  @And("^I update records executing( multi line)? query from file with path: (.*)$")
  public void createRecordsUsingFile(String executingType, String filePath) throws Throwable {

    String queryExecutionMessage = "Executing query from file: %s"; //NOI18N

    filePath = VariablesTransformer.transformSingleValue(filePath);
    filePath = FilenameUtils.separatorsToUnix(filePath);

    File queryFile = new File(filePath);

    if (StringUtils.isEmpty(executingType)) {
      List<String> queries = FileUtils.readLines(queryFile, StandardCharsets.UTF_8);
      for (String query : queries) {
        logger.info(String.format(queryExecutionMessage, query));
        executeGeneralUpdateQuery(query);
      }
    } else {
      String query = VariablesTransformer.transformSingleValue(
          FileUtils.readFileToString(queryFile, StandardCharsets.UTF_8));
      logger.info(String.format(queryExecutionMessage, query));
      executeGeneralUpdateQuery(query);
    }
  }

  /**
   * Heart beat for records.
   *
   * @param tableName  the table name
   * @param resultsNum the results num
   * @param timeOut    the time out
   * @throws Throwable the throwable
   */
/*
     Example usage:
         And I wait for table SENTMESSAGES having rowcount 1 for 10 seconds
  */
  @And("^I wait for table (.*) having rowcount (.*) for (\\d+) seconds$")
  public void heartBeatForRecords(String tableName, String resultsNumString, Integer timeOut) throws Throwable {
    long endTime = System.currentTimeMillis() + (timeOut * 1000);
    
    Integer resultsNum = Integer.parseInt(VariablesTransformer.transformSingleValue(resultsNumString));

    List<List<Object>> result = dbOperationsFactory.select("*", tableName, null, null, null, null, null, null, null,
                                                           null, null,
                                                           null, null, null, null, null);
    logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));
    Integer rowCount = result.size();

    while (rowCount < resultsNum && System.currentTimeMillis() <= endTime) {
      result = dbOperationsFactory.select("*", tableName, null, null, null, null, null, null, null, null, null,
                                          null, null, null, null, null);
      logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));
      rowCount = result.size();

      Thread.sleep(500);
    }
    assertEquals(resultsNum, rowCount,
                 "Expected row number " + resultsNum + " is equal to actual row number " + rowCount);
  }

  /**
   * Gets record count.
   *
   * @param columns                     the columns
   * @param table                       the table
   * @param typeOfJoin                  the type of join
   * @param joiningTable                the joining table
   * @param joinOnColumnCondition       the join on column condition
   * @param filterColumn                the filter column
   * @param operator                    the operator
   * @param filterColumnValue           the filter column value
   * @param additionalCondition         the additional condition
   * @param additionalConditionCol      the additional condition col
   * @param additionalConditionOperator the additional condition operator
   * @param additionalConditionVal      the additional condition val
   * @param groupByValues               the group by values
   * @param variable                    the variable
   * @throws Throwable the throwable
   */
/*
      Example usage:
           And I save record count to variable dbDeAllocationCount executing SELECT * FROM CASEUSERSLOG WHERE ACTION IN 'DEALLOCATE' query
   */
  @And("^I execute SELECT (.*) FROM (.*?)" +
      "(?: (INNER JOIN|LEFT OUTER JOIN) (.*) ON (.*?))?" +
      "(?: WHERE (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: (AND|OR) (.*?) (IN|NOT IN|LIKE|NOT LIKE|IS|IS NOT) (.*?))?" +
      "(?: GROUP BY (.*?))? " +
      "and save record count to variable (.*)$")
  public void getRecordCount(String columns, String table,
      String typeOfJoin, String joiningTable, String joinOnColumnCondition,
      String filterColumn, String operator, String filterColumnValue,
      String additionalCondition, String additionalConditionCol, String additionalConditionOperator,
      String additionalConditionVal,
     String groupByValues, String variable) throws Throwable {

    filterColumnValue = VariablesTransformer.transformSingleValue(filterColumnValue);
    additionalConditionVal = VariablesTransformer.transformSingleValue(additionalConditionVal);
    groupByValues = VariablesTransformer.transformSingleValue(groupByValues);

    List<List<Object>> result = dbOperationsFactory.select(columns, table, typeOfJoin, joiningTable,
                                                           joinOnColumnCondition,
                                                           filterColumn, operator, filterColumnValue,
                                                           additionalCondition,
                                                           additionalConditionCol, additionalConditionOperator,
                                                           additionalConditionVal, groupByValues, null, null, null);
    logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));
    Integer rowCount = result.size();
    logger.info(String.format(SET_STEP_DATA_MESSAGE, variable, rowCount));
    Variables.set(variable, rowCount.toString());
  }

  /**
   * Gets next sequence value.
   *
   * @param sequenceName   the sequence name
   * @param savingVariable the saving variable
   * @throws Throwable the throwable
   */
  /*
   * Usage Example(s):
   *      And I get next value from sequence ACCOUNTACTIVITIESSEQ and save result as nextValueId variable
   */
  @And("^I get next value from sequence (.*) and save result as (.*) variable$")
  public void getNextSequenceValue(String sequenceName, String savingVariable) throws Throwable {
    List<List<Object>> result;
    switch (Config.get("database.vendor").toUpperCase()) {
      case "MSSQL":
        result = dbOperationsFactory.operationsFactory().executeGeneralSelect(
            "SELECT NEXT VALUE FOR " + sequenceName);
        break;
      case "ORACLE":
        result = dbOperationsFactory.operationsFactory().executeGeneralSelect(
            "SELECT " + sequenceName + ".NEXTVAL FROM DUAL");
        break;
      default:
        throw new IllegalArgumentException("Database vendor: " + Config.get("database.vendor") + " is not supported");
    }

    logger.info(String.format(QUERY_RESULT_MESSAGE, result.toString()));
    String value = result.get(0).get(0).toString();

    Variables.set(savingVariable, value);
    logger.info(String.format(SET_STEP_DATA_MESSAGE, savingVariable, value));
  }

  /**
   * Save date to db format.
   *
   * @param inputVariable  the input variable
   * @param format         the format
   * @param outputVariable the output variable
   * @throws Throwable the throwable
   */
  /*
   * Usage Example(s):
   *      And I save formatted date variable ${today} having format ${dateFormatter} to database format as variable today
   */
  @And("^I save formatted date variable (.*) having format (.*) to database format as variable (.*)$")
  public void saveDateToDBFormat(String inputVariable,
      String format, String outputVariable) throws Throwable {

    inputVariable = VariablesTransformer.transformSingleValue(inputVariable);
    format = VariablesTransformer.transformSingleValue(format);

    format = dbOperationsFactory.operationsFactory().normalizeDateFormat(format);
    String date = dbOperationsFactory.operationsFactory().convertDateToDBDateFormat(inputVariable, format);

    Variables.set(outputVariable, date);
  }

  /**
   * Save date to db format.
   *
   * @param inputVariable    is the non-formatted date variable name.
   * @param language         is the language tag to define the locale and the date formatting.
   * @param outputVariable   is the formatted date variable.
   * @throws DBStepException an error that might occur on step execution.
   */
  /*
   * Usage Example(s):
   *      And I format date variable ${todayGermanFormat} by locale for language de and save it to database format as variable dbTodayGermanFormat
   * Example values:
   *      todayGermanFormat = a variable name referring to 06.12.21 (dd.MM.yy) date value.
   *      language          = de, en, bg, cs, etc.
   *      variableName      = a string value by choice.
   */
  @And("^I format date variable (.*) by locale for language (.*) and save it to database format as variable (.*)$")
  public void saveDateFormattedByLocaleToDBFormat(String inputVariable, String language, String outputVariable) throws DBStepException {
    Locale locale = Locale.forLanguageTag(language);
    String defaultDateFormat = "dd/MM/yyyy"; // NOI18N

    SimpleDateFormat formatFromLocale = (SimpleDateFormat) DateFormat.getDateInstance(SimpleDateFormat.SHORT, locale);

    if (language.equalsIgnoreCase("en")) { // NOI18N
      formatFromLocale = new SimpleDateFormat(defaultDateFormat);
    }

    inputVariable = VariablesTransformer.transformSingleValue(inputVariable);

    DBOperationsFactory operationsFactory;

    try {
      operationsFactory = dbOperationsFactory.operationsFactory();
    } catch (Exception exception) {
      throw new DBStepException("There was an error while instantiating the operations factory.", exception); // NOI18N
    }

    String dbFormat = operationsFactory.normalizeDateFormat(formatFromLocale.toPattern());

    String date = operationsFactory.convertDateToDBDateFormat(inputVariable, dbFormat);

    Variables.set(outputVariable, date);
  }

  /**
   * Convert db data type.
   *
   * @param column       the column
   * @param toDataType   the to data type
   * @param variableName the variable name
   * @throws Throwable the throwable
   */
/*
   * Usage Example(s):
   *      And I convert database column MESSAGE to data type VARCHAR and save result as variable MESSAGE_SORT
   *      And I execute SELECT MESSAGE FROM SENTMESSAGES ORDER BY ${MESSAGE_SORT} DESC and save results as:
            | index | variable       |
            | 1     | ExpAccMessage1 |
            | 2     | ExpAccMessage2 |
   */
  @And("^I convert database column (.*) to data type (NUMERIC|VARCHAR|BINARY|DATETIME STRING) and save result as variable (.*)$")
  public void convertDBDataType(String column, String toDataType,
      String variableName) throws Throwable {

    column = VariablesTransformer.transformSingleValue(column);

    if (toDataType.equals("NUMERIC")) {
      column = dbOperationsFactory.operationsFactory().convertColumnToNumeric(column);
    } else if (toDataType.equals("VARCHAR")) {
      column = dbOperationsFactory.operationsFactory().convertColumnToVarchar(column);
    } else if (toDataType.equals("BINARY")) {
      column = dbOperationsFactory.operationsFactory().convertColumnToBinary(column);
    } else if (toDataType.equals("DATETIME STRING")) {
      column = dbOperationsFactory.operationsFactory().convertDateTimeColumnToString(column);
    }

    Variables.set(variableName, column);
  }

  /**
   * Register connection.
   *
   * @param name       the name
   * @param properties the properties
   * @throws Throwable the throwable
   */
  /*
   * Usage Example(s):
   *    And I register new database connection with name newConn and properties:
   *      | driverClassName | content |
   *      | url             | content |
   *      | username        | content |
   *      | password        | content |
   *      | platform        | content |
   */
  @And("^I register new database connection with name (.*) and properties:$")
  public void registerConnection(String name,
      Map<String, String> properties) throws Throwable {

    name = VariablesTransformer.transformSingleValue(name);
    properties = VariablesTransformer.transformMap(properties);
    DBOperationsFactory instance = DBOperationsFactory.getDbOperationsFactoryInstance();

    if (name.equalsIgnoreCase("default")) {

      throw new Exception("Cannot overwrite default configurations");
    }

    instance.registerConnection(name, properties.get("driverClassName"), properties.get("url"),
                                properties.get("username"), properties.get("password"), properties.get("platform"));
  }

  /**
   * Switch connection.
   *
   * @param name the name
   * @throws Throwable the throwable
   */
  /*
   * Usage Example(s):
   *    And I switch to newConn database connection
   *    And I switch to default database connection
   */
  @And("^I switch to (.*) database connection$")
  public void switchConnection(String name) throws Throwable {

    name = VariablesTransformer.transformSingleValue(name);

    DBOperationsFactory instance = DBOperationsFactory.getDbOperationsFactoryInstance();
    if (name.equalsIgnoreCase("default")) {
      instance.switchToDefaultConnection();
    } else {
      instance.switchConnection(name);
    }
  }

  /**
   * Execute general select and verify.
   *
   * @param query     the query
   * @param path      the path
   * @param separator the separator
   * @throws Throwable the throwable
   */
  /*
   * Usage Example(s):
   *    And I execute general select query: SELECT * FROM TEAM and verify the result with csv file: ${features.path}/BusinessReports/data/export.csv with , separator
   */
  @And("^I execute general select query: (.*) and verify the result with csv file: (.*) with (.) separator$")
  public void executeGeneralSelectAndVerify(String query,
      String path,
      String separator) throws Throwable {

    query = VariablesTransformer.transformSingleValue(query);
    path = VariablesTransformer.transformSingleValue(path);

    List<List<String>> csvAsList;
    try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8), separator.charAt(0))) {

      csvAsList = new ArrayList<>();

      String[] line;
      while ((line = reader.readNext()) != null) {
        csvAsList.add(Arrays.asList(line));
      }
    }
    executeGeneralSelect(query, csvAsList);
  }


  /**
   * Verify a database query result table
   * @param queryResultTable The result table
   * @param expectedResultTable An expected table
   * @throws Exception
   */
  private void verifyQueryResultTable(List<List<Object>> queryResultTable, List<List<Object>> expectedResultTable)
      throws Exception {
    for (List<Object> expectedRow : expectedResultTable) {
      Boolean found = false;
      for (int i = 0; !found && i < queryResultTable.size(); i++) {
        List<Object> actualRow = queryResultTable.get(i);
        List<Object> formattedActualRow = new ArrayList<>(actualRow.size());
        for (Object element : actualRow) {
          Object cell = element;
          if (element instanceof String) {
            cell = dbOperationsFactory.operationsFactory().transformValue((String) element, element.getClass());
          } else if (element instanceof Timestamp) {
            cell = element.toString();
          }
          formattedActualRow.add(cell);
        }
        if (dbOperationsFactory.rowsCompare(formattedActualRow, expectedRow)) {
          queryResultTable.remove(actualRow);
          found = true;
        }
      }
      assertTrue(found, "Query result row " + expectedRow + " found");
    }
  }

  /**
   * Extract the Classes of each Column from a result table if the column contains Non Null values
   * @param queryResultTable A result table from a SELECT query to a database
   * @return List<Class>
   */
  private List<Class> extractDataTypesFromQueryResult(List<List<Object>> queryResultTable) {
    List<Class> dataTypes = new ArrayList<>();
    for (int columnIndex = 0; columnIndex < queryResultTable.get(0).size(); columnIndex++) {
      boolean notNullValueFound = false;
      for (int rowIndex = 0; !notNullValueFound && rowIndex < queryResultTable.size(); rowIndex++) {
        if (queryResultTable.get(rowIndex).get(columnIndex) != null) {
          dataTypes.add(queryResultTable.get(rowIndex).get(columnIndex).getClass());
          notNullValueFound = true;
        }
      }
      if (!notNullValueFound) {
        dataTypes.add(null);
      }
    }

    return dataTypes;
  }

  /**
   * Prepares SQL statement from text and saves it as variable
   *
   * @param var          Variable to store sql statement
   * @param sqlStatement The SQL statement which to execute
   */
  /*
   * Usage Example(s):
   *    And I save sql statement as variable select_from_employees:
   *    """
   *      SELECT employee_id, employee_name, employee_number, employee_address FROM employees
   *      WHERE employee_surname = '${employeeSurname}'
   *      ORDER BY employee_name ASC
   *    """
   */
  @And("^I save sql statement as variable (.*):$")
  public void prepareSqlFromText(String var, String sqlStatement) {
    sqlStatement = VariablesTransformer.transformSingleValue(sqlStatement);
    Variables.set(var, sqlStatement);
  }
}
