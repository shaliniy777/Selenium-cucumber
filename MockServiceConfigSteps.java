/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.mock.MockServiceConfigOperations;
import com.experian.automation.transformers.VariablesTransformer;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.And;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.mockserver.model.Header;

/**
 * The type Mock service config steps.
 */
public class MockServiceConfigSteps {

  private final MockServiceConfigOperations mockServiceConfigOperations;

  /**
   * Instantiates a new Mock service config steps.
   *
   * @param mockServiceConfigOperations the mock service config operations
   */
  public MockServiceConfigSteps(MockServiceConfigOperations mockServiceConfigOperations) {
    this.mockServiceConfigOperations = mockServiceConfigOperations;
  }

  /**
   * Start mock server.
   *
   * @param port the port
   * @throws Throwable the throwable
   */
  /* Example call:
   *    And I start Mock Server on port 10080
   */
  @And("^I start Mock Server on port (.*)$")
  public void startMockServer(String port) throws Throwable {

    port = VariablesTransformer.transformSingleValue(port);

    mockServiceConfigOperations.start(Integer.valueOf(port));
  }

  /**
   * Stop mock server.
   *
   * @param port the port
   * @throws Throwable the throwable
   */
  /* Example call:
   *    And I stop Mock Server on port 10080
   */
  @And("^I stop Mock Server on port (.*)$")
  public void stopMockServer(String port) throws Throwable {

    port = VariablesTransformer.transformSingleValue(port);

    mockServiceConfigOperations.stop(Integer.valueOf(port));
  }

  /**
   * Load expectation.
   *
   * @param endpoint the endpoint
   * @param filePath the file path
   * @param port     the port
   * @throws Throwable the throwable
   */
/* Example call:
   *    And I load response for endpoint /api/spa/layout from file ${some.path} for Mock Server on port 10080
   *
   * Json file structure example:
      [
        {
          "when": {
            "headers": {
              "X-Correlation-Id" : "75cca6f9-a3c2-42af-b7ce-1897cf276752"
            },
            "body": {
              "numbersOnly": "123",
              "abc": "abcvalue"
            },
            "queryParameters": {}
          },
          "headers": {},
          "body": 1,
          "code": 200
        }
      ]
      * Regex body validation example:
        [
          {
            "when": {
              "headers": {
                "X-Correlation-Id" : "75cca6f9-a3c2-42af-b7ce-1897cf276752"
              },
              "body": ".*\"numbersOnly\": \"\\d*\",\"abc\": \"abcvalue\",\"label\": \"label\".*",
              "queryParameters": {}
            },
            "headers": {},
            "body": 1,
            "code": 200
          }
        ]
   */
  @And("^I load response for endpoint (.*) from file (.*) for Mock Server on port (.*)$")
  public void loadExpectation(String endpoint, String filePath, String port) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);
    port = VariablesTransformer.transformSingleValue(port);

    String method = FilenameUtils.getBaseName(filePath);
    JSONArray mockExpectations = new JSONArray(FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8));

    for (Object mockExpectation : mockExpectations) {

      DocumentContext documentContext = JsonPath.parse(mockExpectation);

      String requestBody = documentContext.read("$.when.body").toString();
      Map<String, String> requestHeaders = documentContext.read("$.when.headers");

      String responseBody = documentContext.read("$.body").toString();
      Map<String, String> responseHeaders = documentContext.read("$.headers");
      int responseStatusCode = Integer.parseInt(documentContext.read("$.code").toString());

      List<Header> expectedRequestHeaders = new ArrayList<>();
      requestHeaders.forEach((key, value) -> expectedRequestHeaders.add(new Header(key, value)));
      List<Header> expectedResponseHeaders = new ArrayList<>();
      responseHeaders.forEach((key, value) -> expectedResponseHeaders.add(new Header(key, value)));

      mockServiceConfigOperations.createExpectation(method, endpoint, requestBody, expectedRequestHeaders,
                                                    responseBody, expectedResponseHeaders, responseStatusCode,
                                                    Integer.valueOf(port));
    }
  }

  /**
   * Load expectations.
   *
   * @param mockResponsesPath the mock responses path
   * @param port              the port
   * @throws Throwable the throwable
   */
  /* Example call:
   *    And I load responses from directory ${features.path}/ApiMock/data/mockresponses for Mock Server on port 10080
   */
  @And("^I load responses from directory (.*) for Mock Server on port (.*)$")
  public void loadExpectations(String mockResponsesPath, String port) throws Throwable {

    mockResponsesPath = VariablesTransformer.transformSingleValue(mockResponsesPath);
    port = VariablesTransformer.transformSingleValue(port);

    File mockResponseDir = new File(mockResponsesPath);
    Collection<File> apiMockResponses = FileUtils.listFiles(mockResponseDir, null, true);

    for (File file : apiMockResponses) {

      String basePath = StringUtils.substringAfter(FilenameUtils.separatorsToUnix(file.getPath()), mockResponsesPath);
      String endpoint = FilenameUtils.getFullPathNoEndSeparator(basePath);
      endpoint = endpoint.endsWith("index") ? StringUtils.substringBefore(endpoint, "index") : endpoint;
      loadExpectation(endpoint, file.getPath(), port);
    }
  }
}
