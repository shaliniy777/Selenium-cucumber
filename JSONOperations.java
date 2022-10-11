/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

/**
 * The type Json operations.
 */
public class JSONOperations {
  private final Logger logger = Logger.getLogger(this.getClass());


  /**
   * Validate json boolean.
   *
   * @param jsonString   the json string
   * @param schemaString the schema string
   * @return the boolean
   * @throws IOException         the io exception
   * @throws ProcessingException the processing exception
   */
  public boolean validateJSON(String jsonString, String schemaString) throws IOException, ProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonFileNode = mapper.readTree(jsonString);
    JsonNode jsonSchemaNode = mapper.readTree(schemaString);

    JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonSchema schema = factory.getJsonSchema(jsonSchemaNode);

    return schema.validInstance(jsonFileNode);
  }

  /**
   * Validate json boolean.
   *
   * @param jsonString the json string
   * @param schemaFile the schema file
   * @return the boolean
   * @throws IOException         the io exception
   * @throws ProcessingException the processing exception
   */
  public boolean validateJSON(String jsonString, File schemaFile) throws IOException, ProcessingException {
    return validateJSON(jsonString, FileUtils.readFileToString(schemaFile, StandardCharsets.UTF_8));
  }

  /**
   * Compare boolean.
   *
   * @param jsonString  the json string
   * @param jsonString2 the json string 2
   * @return the boolean
   * @throws IOException the io exception
   */
  public boolean compare(String jsonString, String jsonString2) throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode1 = mapper.readTree(jsonString);
    JsonNode jsonNode2 = mapper.readTree(jsonString2);

    return jsonNode1.equals(jsonNode2);
  }

  /**
   * Is json boolean.
   *
   * @param stringToTest the string to test
   * @return the boolean
   */
  public boolean isJSON(String stringToTest) {
    try {
      new JSONObject(stringToTest);
      return true;
    } catch (JSONException e) {
      return false;
    }
  }

  /**
   * Compare boolean.
   *
   * @param jsonString the json string
   * @param jsonFile   the json file
   * @return the boolean
   * @throws IOException the io exception
   */
  public boolean compare(String jsonString, File jsonFile) throws IOException {
    return compare(jsonString, FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8));
  }

  /**
   * Evaluate json path string.
   *
   * @param jsonString the json string
   * @param jsonPath   the json path
   * @return the string
   */
  public String evaluateJSONPath(String jsonString, String jsonPath) {
    try {
      DocumentContext docCtx = JsonPath.parse(jsonString);
      return docCtx.read(jsonPath) == null ? "null" : docCtx.read(jsonPath).toString();
    } catch (PathNotFoundException e) {
      logger.debug(String.format("Path not found. Path: %s. JSON: %s", jsonPath, jsonString)); //NOI18N
      throw new PathNotFoundException(e);
    }
  }

  /**
   * Gets value from json file.
   *
   * @param filePath the file path
   * @param jsonPath the json path
   * @return the value from json file
   * @throws Exception the exception
   */
  public String getValueFromJsonFile(String filePath, String jsonPath) throws Exception {
    String jsonString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
    return evaluateJSONPath(jsonString, jsonPath);
  }

  /**
   * Evaluate json path string to get multiple values
   *
   * @param jsonString the json string
   * @param jsonPath   the json path
   * @return list of string values
   */
  public List<String> evaluateJSONPathForMultipleValues(String jsonString, String jsonPath) {
    try {
      DocumentContext docCtx = JsonPath.parse(jsonString);
      return docCtx.read(jsonPath) == null ? Collections.emptyList() : docCtx.read(jsonPath);
    } catch (PathNotFoundException e) {
      logger.debug(String.format("Path not found. Path: %s. JSON: %s", jsonPath, jsonString)); //NOI18N
      throw new PathNotFoundException(e);
    }
  }

  /**
   * Get list of values from json file.
   *
   * @param filePath the file path
   * @param jsonPath the json path
   * @return list of values from json file
   * @throws IOException the io exception
   */
  public List<String> getMultipleValuesFromJsonFile(String filePath, String jsonPath) throws IOException {
    String jsonString = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
    return evaluateJSONPathForMultipleValues(jsonString, jsonPath);
  }

  /**
   * Modify json file.
   *
   * @param targetPath the target path
   * @param properties the properties
   * @throws IOException the io exception
   */
  public void modifyJsonFile(String targetPath, Map<String, String> properties)
      throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    JsonNode tree = mapper.readTree(new File(targetPath));

    Configuration configuration = Configuration.builder()
        .jsonProvider(new JacksonJsonNodeJsonProvider())
        .mappingProvider(new JacksonMappingProvider())
        .build();

    // Parse the tree
    DocumentContext docContext = JsonPath.using(configuration).parse(tree);

    // Update all values
    for (String key : properties.keySet()) {
      try {
        docContext.read(key);
        // Update if found
        docContext.set(key, defineJSONValueType(properties.get(key)));
      } catch (PathNotFoundException e) {
        // Create if parent exists
        String parentNodeJsonPath = key.substring(0, key.lastIndexOf('['));
        if (docContext.read(parentNodeJsonPath) != null) {
          String parentNodeName = key.substring(key.lastIndexOf('[') + 2, key.lastIndexOf(']') - 1);

          docContext.put(parentNodeJsonPath, parentNodeName, defineJSONValueType(properties.get(key)));
        }
      }
    }

    // Save updated JSON
    docContext.json();
    ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
    writer.writeValue(new File(targetPath), tree);
  }

  /**
   * Compare values boolean.
   *
   * @param firstStr  the first str
   * @param secondStr the second str
   * @return the boolean
   */
  public boolean compareValues(String firstStr, String secondStr) {
    if(secondStr.matches("(^\\[RGX].*$)")) {
      return firstStr.matches(secondStr.replace("[RGX]", ""));
    }

    if (firstStr.matches("(^\\[.*]$)") && secondStr.matches("(^\\[.*]$)")) {

      JSONArray jsonArray = new JSONArray(firstStr);
      JSONArray jsonArray2 = new JSONArray(secondStr);

      if (jsonArray.length() == jsonArray2.length()) {
        List<String> firstList = new ArrayList<String>();
        List<String> secondList = new ArrayList<String>();

        for (int i = 0; i < jsonArray.length(); i++) {
          firstList.add(jsonArray.get(i).toString().trim());
          secondList.add(jsonArray2.get(i).toString().trim());
        }

        // Compare values
        return firstList.containsAll(secondList) && secondList.containsAll(firstList);
      } else {
        return false;
      }
    }

    return firstStr.equals(secondStr);
  }

  /**
   * Define json value type object.
   *
   * @param value the value
   * @return the object
   * @throws IOException the io exception
   */
  public Object defineJSONValueType(String value) throws IOException {
    if (StringUtils.isBlank(value)) {
      return value;
    } else if (StringUtils.isNumeric(value)) {
      return Long.valueOf(value);
    } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
      return Boolean.valueOf(value);
    } else if (value.startsWith("{") && value.endsWith("}")) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(value);
    }
    return value;
  }

  /**
   * Compare customized json json compare result.
   *
   * @param expectedJsonObject the expected json object
   * @param actualJsonObject   the actual json object
   * @param compareMode        the compare mode
   * @param customizations     the customizations
   * @return the json compare result
   * @throws JSONException the json exception
   */
/*
    Compare two json objects with customized JSON compare mode and Customized Regex comparator.
    "JSONCompareMode" should use following values: STRICT, LENIENT, NON_EXTENSIBLE, STRICT_ORDER
    "customizations" is to enable user to compare JsonObject using Regex Pattern base on specific JsonPath.
    Refer to "compareJsonFileObjects" step definition from automation.steps.FileComparisonSteps.
   */
  public JSONCompareResult compareCustomizedJSON(JSONObject expectedJsonObject, JSONObject actualJsonObject,
      JSONCompareMode compareMode, @Nullable Customization[] customizations) throws JSONException {
    CustomComparator regExComparator;
    if (customizations == null) {
      regExComparator = new CustomComparator(compareMode);
    } else {
      regExComparator = new CustomComparator(compareMode, customizations);
    }
    return JSONCompare.compareJSON(expectedJsonObject, actualJsonObject, regExComparator);
  }

  /**
   * Convert file to json object json object.
   *
   * @param filePath the file path
   * @param charset  the charset
   * @return the json object
   * @throws Exception the exception
   */
/*
    To convert file to org.json.JSONObject object.
   */
  public JSONObject convertFileToJsonObject(String filePath, Charset charset) throws Exception {
    JSONTokener tokener = new JSONTokener(new String(Files.readAllBytes(Paths.get(filePath)), charset));
    return new JSONObject(tokener);
  }

  /**
   * Delete from json file.
   *
   * @param targetPath the target path
   * @param jsonPathsToDelete the jsonPathsToDelete
   * @throws IOException the io exception
   */
  public void removeFromJsonFile(String targetPath, List<String> jsonPathsToDelete) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode tree = mapper.readTree(new File(targetPath));
    Configuration configuration = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .build();
    // Parse the tree
    DocumentContext docContext = JsonPath.using(configuration).parse(tree);

    for (String jsonPathToDelete : jsonPathsToDelete) {
      docContext.delete(jsonPathToDelete);
    }
    // Save updated JSON
    docContext.json();
    ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
    writer.writeValue(new File(targetPath), tree);
  }
}