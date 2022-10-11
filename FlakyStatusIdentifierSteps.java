/*
 * Copyright (c) Experian, 2022. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.Config;
import com.experian.automation.logger.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Instantiates a new Json output result file into Temp directory and gather all test result.
 * The Json file will be reused to determine Flaky status once the same test-id scenario is being rerun from the same test executor.
 */
public class FlakyStatusIdentifierSteps {

  private final Logger logger = Logger.getLogger(this.getClass());

  private HashSet<String> cucumberOutputMessage = new LinkedHashSet<>();
  private static final String FLAKY = "Flaky"; // NOI18N
  private static final String RESULTKEY = "Result:"; // NOI18N
  private static final String OUTLINERESULTSKEY = "OutlineResults:"; // NOI18N
  private static final String RETRIEDSTATUSKEY = "TestRetriedStatus:"; // NOI18N
  private static final String RETRIEDNUMBERKEY = "TestRetried:"; // NOI18N
  private static final String FLAKYSTATUSKEY = "FlakyStatus:"; // NOI18N
  private static final String LINENUMBERKEY = "LineNumber:"; // NOI18N
  private static final String RETRIEDCOUNTKEY = "RetriedCount:"; // NOI18N
  private static final String DEFAULTVALUE = "-";  // NOI18N

  /**
   * Generate Json Result output file
   *
   * @param scenarioId Scenario Full Id
   * @param scenarioUri Scenario feature file path
   * @param sourceTagNames Scenario Tag Names (Collection)
   * @param scenarioStatusName Scenario Status
   * @param testClassPath Scenario auto-generated feature file path
   * @throws IOException - read/write file exception
   * @throws ParseException - json file parser exception
   */
  public void generateJsonOutput(String scenarioId, String scenarioUri, Collection<String> sourceTagNames,
    String scenarioStatusName, String testClassPath) throws IOException, ParseException {

    File tempDirectory = new File(Config.get("temp.dir") == null ? System.getProperty("user.home") : Config.get("temp.dir"));
    String filepath =
        tempDirectory + File.separator + getSimplifiedId(scenarioUri) + ".json"; // NOI18N
    File logFile = new File(filepath);

    if (!tempDirectory.exists()) {
      tempDirectory.mkdirs();
    }
    if (logFile.createNewFile()) {
      fileWriter(new JSONObject(), filepath);
      logger.info("Create New JSON File...");
    }

    int lineNumber = Integer.parseInt(scenarioId.substring(scenarioId.lastIndexOf(':') + 1));
    boolean isScenarioOutline = isCurrentScenarioOutline(testClassPath, lineNumber, scenarioUri);

    Pattern testIdPattern = Pattern.compile("@test-id-.*"); // NOI18N
    List<String> matches = sourceTagNames
        .stream().filter(testIdPattern.asPredicate()).collect(Collectors.toList());

    if (!matches.isEmpty()) {
      JSONParser jsonParser = new JSONParser();
      FileReader fileReader = new FileReader(filepath);
      JSONObject jsonBody = (JSONObject) jsonParser.parse(fileReader);
      fileReader.close();

      for (String match : matches) {
        if (jsonBody.containsKey(match)) {
          JSONObject arrayValue = updateJsonArrayBody((JSONObject) jsonBody.get(match), scenarioStatusName,
                                                      isScenarioOutline, lineNumber);
          jsonBody.put(match, arrayValue);
        } else {
          jsonBody.put(match, createJsonArrayBody(scenarioStatusName, isScenarioOutline, lineNumber));
        }
      }
      fileWriter(jsonBody, filepath);
    }

    if (!cucumberOutputMessage.isEmpty()) {
      cucumberOutputMessage.forEach(logger::info);
    }
  }

  /**
   * Create Json Body if test-id not found in existing Json file.
   */
  private JSONObject createJsonArrayBody(String currentResult, boolean isScenarioOutline, int lineNumber) {

    int defaultCounter = 0;
    JSONObject outputResult = new JSONObject();

    if (isScenarioOutline) {
      JSONArray resultsArray  = new JSONArray();
      JSONObject resultObject = createOutlineResultsBody(currentResult, lineNumber, defaultCounter, DEFAULTVALUE);
      resultsArray.add(resultObject);
      outputResult.put(OUTLINERESULTSKEY, resultsArray);
    } else {
      outputResult.put(RESULTKEY, currentResult);
      outputResult.put(FLAKYSTATUSKEY, DEFAULTVALUE);
    }

    outputResult.put(RETRIEDNUMBERKEY, defaultCounter);
    outputResult.put(RETRIEDSTATUSKEY, DEFAULTVALUE);

    return outputResult;
  }

  /**
   * Update Json Body if test-id is found in existing Json file.
   */
  private JSONObject updateJsonArrayBody(JSONObject results, String currentResult, boolean isScenarioOutline,
      int lineNumber) {

    return isScenarioOutline ? updateScenarioOutline(results, currentResult, lineNumber)
        : updateScenario(results, currentResult);
  }

  /**
   * Update Json Body if test scenario is scenario outline.
   */
  private JSONObject updateScenarioOutline(JSONObject results, String currentResult, long lineNumber) {

    int index = -1;
    JSONObject newResult;

    if (results.containsKey(OUTLINERESULTSKEY)) {
      JSONArray outlineArrayResults = (JSONArray) results.get(OUTLINERESULTSKEY);

      for (int i = 0; i < outlineArrayResults.size(); i++) {
        JSONObject jsonObject = (JSONObject) outlineArrayResults.get(i);

        if (jsonObject.get(LINENUMBERKEY).equals(lineNumber)) {
          index = i;
          break;
        }
      }
      newResult = updateOutlineResultsBody(index, lineNumber, currentResult, outlineArrayResults);
    } else {
      return createJsonArrayBody(currentResult, true, (int) lineNumber);
    }

    return newResult;
  }

  /**
   * Update Json Body if test scenario is normal scenario (Not scenario outline).
   */
  private JSONObject updateScenario(JSONObject results, String currentResult) {

    String flakyValue = "";
    JSONObject newResult = new JSONObject();

    if (!results.containsKey(OUTLINERESULTSKEY)) {

      newResult.put(RETRIEDNUMBERKEY, Integer.parseInt(results.get(RETRIEDNUMBERKEY).toString()) + 1);
      newResult.put(RETRIEDSTATUSKEY, currentResult);
      newResult.put(RESULTKEY, currentResult);
      if (!results.get(RESULTKEY).toString().equals(currentResult)) {
        flakyValue = FLAKY;
      }

      if (results.get(FLAKYSTATUSKEY).toString().equals(FLAKY) || flakyValue.equals(FLAKY)) {
        newResult.put(FLAKYSTATUSKEY, FLAKY);
        flakyMessageOutput(newResult);
      } else {
        newResult.put(FLAKYSTATUSKEY, results.get(FLAKYSTATUSKEY));
      }

    } else {
      return createJsonArrayBody(currentResult, false, 0);
    }
    return newResult;
  }

  /**
   * Update scenario outline Json Body base on the previous execution result.
   */
  private JSONObject updateOutlineResultsBody(int index, long lineNumber, String currentResult, JSONArray outlineArrayResults) {

    JSONObject newResult = new JSONObject();
    JSONObject resultObject;
    String flakyValue = "-";  // NOI18N
    int retriedCount = 0;

    if (index >= 0) {
      JSONObject jsonResult = (JSONObject) outlineArrayResults.get(index);
      retriedCount = Integer.parseInt(jsonResult.get(RETRIEDCOUNTKEY).toString()) + 1 ;
      if (!jsonResult.get(RESULTKEY).toString().equals(currentResult) || jsonResult.get(FLAKYSTATUSKEY).toString().equals(FLAKY)) {
        flakyValue = FLAKY;
      }
      resultObject = createOutlineResultsBody(currentResult, lineNumber, retriedCount, flakyValue);
      outlineArrayResults.set(index, resultObject);

    } else {
      resultObject = createOutlineResultsBody(currentResult, lineNumber, retriedCount, flakyValue);
      outlineArrayResults.add(resultObject);
    }
    newResult.put(OUTLINERESULTSKEY, outlineArrayResults);
    newResult.put(RETRIEDNUMBERKEY, retriedCount);
    newResult.put(RETRIEDSTATUSKEY, currentResult);

    if (resultObject.get(FLAKYSTATUSKEY).equals(FLAKY)) {
      flakyMessageOutput(newResult);
    }

    return newResult;
  }

  /**
   * Create scenario outline Json Body.
   */
  private JSONObject createOutlineResultsBody(String result, long lineNumber, int retriedCount, String flakyStatus ) {
    JSONObject resultObject = new JSONObject();
    resultObject.put(LINENUMBERKEY, lineNumber);
    resultObject.put(RESULTKEY, result);
    resultObject.put(RETRIEDCOUNTKEY, retriedCount);
    resultObject.put(FLAKYSTATUSKEY, flakyStatus);
    return resultObject;
  }

  /**
   * Output Flaky messages into cucumber.json file
   */
  private void flakyMessageOutput(JSONObject results) {
    cucumberOutputMessage.add(RETRIEDSTATUSKEY + results.get(RETRIEDSTATUSKEY));
    cucumberOutputMessage.add(RETRIEDNUMBERKEY + results.get(RETRIEDNUMBERKEY));
  }

  /**
   * Create Json file
   */
  private void fileWriter(JSONObject jsonObject, String filepath) throws IOException {
    try (FileWriter file = new FileWriter(filepath)) {
      file.write(jsonObject.toJSONString());
    }
  }

  /**
   * Determine current scenario is a scenario outline scenario or not an outline scenario.
   */
  private boolean isCurrentScenarioOutline(String testClassPath, int lineNumber, String scenarioUri) throws IOException {
    String testFilePath = testClassPath + scenarioUri.substring(scenarioUri.lastIndexOf(':') + 1);

    String line = Files.readAllLines(Paths.get(testFilePath)).get(lineNumber - 1);
    String name = "Scenario:"; // NOI18N
    String regex = "(^%s)(.*)"; // NOI18N
    Pattern testResultPattern = Pattern.compile(String.format(regex, name));
    Matcher testResultMatcher = testResultPattern.matcher(line.trim());
    return !testResultMatcher.find();
  }

  private String getSimplifiedId(String uri) {
    return new File(uri).getName().replace(".feature", ""); // NOI18N
  }
}
