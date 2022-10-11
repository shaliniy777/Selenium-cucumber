/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static java.lang.Integer.parseInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.experian.automation.helpers.APIOperations;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.JSONOperations;
import com.experian.automation.helpers.RESTOperations;
import com.experian.automation.helpers.RetryExecutor;
import com.experian.automation.helpers.SOAPOperations;
import com.experian.automation.helpers.Variables;
import com.experian.automation.helpers.XMLOperations;
import com.experian.automation.helpers.saas.TokenUtils;
import com.experian.automation.helpers.tools.jwt.JwtGenerator;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.cucumber.java.en.And;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.Assert;
import org.testng.TestException;
import org.testng.asserts.SoftAssert;

/**
 * The type Api steps.
 */
public class APISteps {

  /**
   * The constant RESPONSE_HEADERS.
   */
  public static final String RESPONSE_HEADERS = "ResponseHeaders"; //NOI18N

  private static final String API_REQUEST_BODY_VAR = "API_REQUEST_BODY"; //NOI18N
  private static final String API_REQUEST_TYPE_VAR = "API_REQUEST_TYPE"; //NOI18N
  private static final String API_USERNAME_VAR = "API_USERNAME"; //NOI18N
  private static final String API_PWORD_VAR = "API_PASSWORD"; //NOI18N
  private static final String API_JWT_TOKEN_VAR = "API_JWT_TOKEN"; //NOI18N
  private static final String RESPONSE_VAR = "RESPONSE"; //NOI18N

  private static final String UNSUPPORTED_REQUEST_MESSAGE = "Unsupported request type "; //NOI18N
  private static final String UNSUPPORTED_RESPONSE_MESSAGE = "Unsupported response type"; //NOI18N
  private static final String RESPONSE_TYPE_JSON = "JSON";  //NOI18N

  private static final String JSON_VALIDATION_FULL_MATCHED = "match"; //NOI18N
  private static final String JSON_VALIDATION_CONTAINS_FIELD = "contains"; //NOI18N
  private static final String JSON_VALIDATION_NOT_CONTAINS_FIELD = "does not contain"; //NOI18N
  public static final String XML = "XML"; //NOI18N
  public static final String JSON = "JSON"; //NOI18N
  public static final String REST = "REST"; //NOI18N
  public static final String SOAP = "SOAP"; //NOI18N


  private final Logger logger = Logger.getLogger(this.getClass());
  private SOAPOperations soapOperations;
  private RESTOperations restOperations;
  private String baseURL;
  private Map<String, String> queryParameters;
  private Map<String, Object> queryFields;
  private boolean useBasicAuthentication;

  /**
   * Instantiates a new Api steps.
   */
  public APISteps() {
    this.restOperations = new RESTOperations();
    this.soapOperations = new SOAPOperations();
    this.queryParameters = new HashMap();
    this.queryFields = new HashMap();
    this.useBasicAuthentication = false;
  }

  /**
   * Sets base url
   * <pre>Example:
   * When I set the base webservice url to ${base.webservices.url}</pre>
   *
   * @param url the url
   * @deprecated typo; use setBaseURL() instead
   */
  @Deprecated
  public void setBseURL(String url) {
    setBaseURL(url);
  }

  /**
   * Sets base url
   * <pre>Example:
   * When I set the base webservice url to ${base.webservices.url}</pre>
   *
   * @param url the url
   */
  @And("^I set the base webservice url to (.*)$")
  public void setBaseURL(String url) {
    url = VariablesTransformer.transformSingleValue(url);
    this.baseURL = url;
  }

  /**
   * Prepare request from text.
   * <pre>Example:
   *  And I prepare SOAP request body:
   *    """
   *    < ?xml version="1.0"?>
   *    < soap:Envelope
   *    xmlns:soap="http://www.w3.org/2003/05/soap-envelope/"
   *    soap:encodingStyle="http://www.w3.org/2003/05/soap-encoding">
   *    < soap:Body>
   *      < m:GetPrice xmlns:m="https://www.w3schools.com/prices">
   *        < m:Item>Apples< /m:Item>
   *      < /m:GetPrice>
   *    < /soap:Body>
   *    < /soap:Envelope>
   *    """</pre>
   *
   * @param requestType the request type
   * @param requestBody the request body
   */
  @And("^I prepare (REST|SOAP) request body:$")
  public void prepareRequestFromText(String requestType, String requestBody) {

    requestBody = VariablesTransformer.transformSingleValue(requestBody);

    Variables.set(API_REQUEST_BODY_VAR, requestBody);
    Variables.set(API_REQUEST_TYPE_VAR, requestType);
  }

  /**
   * Prepare request params.
   * <pre>Example:
   * And I prepare REST request params:
   *   | param1 | value1 |
   *   | param2 | value2 |</pre>
   *
   * @param requestType the request type
   * @param params      the params
   */
  @And("^I prepare (REST|SOAP) request params:$")
  public void prepareRequestParams(String requestType, Map<String, String> params) {
    for (String param : params.keySet()) {
      String value = VariablesTransformer.transformSingleValue(params.get(param));

      queryParameters.put(param, value);
    }
    Variables.set(API_REQUEST_TYPE_VAR, requestType);
  }

  /**
   * Prepare request fields.
   * <pre>Example:
   * And I prepare REST request Fields:
   *   | field1 | value1 |</pre>
   *
   * @param requestType the request type
   * @param params      the params
   */
  @And("^I prepare (REST|SOAP) request Fields:$")
  public void prepareRequestFields(String requestType, Map<String, String> params) {
    for (String param : params.keySet()) {
      String value = VariablesTransformer.transformSingleValue(params.get(param));
      queryFields.put(param, value);
    }
    Variables.set(API_REQUEST_TYPE_VAR, requestType);
  }

  /**
   * Prepare file upload.
   * <pre>Example:
   * And I prepare file to send REST request:
   *   | file | ${features.path}/ACF/data/evaluateCredit.txt|</pre>
   *
   * @param requestType the request type
   * @param params      the params
   */
  @And("^I prepare file to send (REST) request:$")
  public void prepareFileUpload(String requestType, Map<String, String> params) {
    for (String param : params.keySet()) {
      String value = VariablesTransformer.transformSingleValue(params.get(param));
      File fileObject = new File(value);
      queryFields.put(param, fileObject);
    }
    Variables.set(API_REQUEST_TYPE_VAR, requestType);
  }

  /**
   * Prepare request from file.
   * <pre>Example:
   * And I prepare REST request body from file ${features.path}/ACF/data/evaluateCredit_request.json</pre>
   *
   * @param requestType the request type
   * @param filePath    the file path
   * @throws IOException the exception
   */
  @And("^I prepare (REST|SOAP) request body from file (.*)$")
  public void prepareRequestFromFile(String requestType, String filePath) throws IOException {
    filePath = VariablesTransformer.transformSingleValue(filePath);

    filePath = FilenameUtils.separatorsToUnix(filePath);
    File file = new File(filePath);
    String requestBody = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    prepareRequestFromText(requestType, requestBody);
  }

  /**
   * Step that creates JSON from CSV header/line value map and prepares it as REST request body
   * <pre>Example:
   * Given I prepare REST request body from csv file ${features.path}/POC-CreditMatch/data/A001/A001-RequestData.csv line 3</pre>
   *
   * @param filePath   path to the CSV file. Header values are used as json keys.
   * @param lineNumber Optional parameter. Row number to be used for json values. If missing the whole CSV file is
   *                   converted to JSON object.
   * @throws IOException exception
   */
  @And("^I prepare REST request body from csv file (.*?)(?: line (\\d+))?$")
  public void prepareRequestFromCSV(String filePath, Integer lineNumber) throws IOException {
    FileOperationsSteps fileOperationsSteps = new FileOperationsSteps();
    fileOperationsSteps.convertCSVtoJSON(filePath, lineNumber, API_REQUEST_BODY_VAR);
    Variables.set(API_REQUEST_TYPE_VAR, REST);  //NOI18N
  }

  /**
   * Prepare authentication.
   * <pre>Example:
   * And I prepare REST authentication username user1 and password password1</pre>
   *
   * @param username the username
   * @param password the password
   */
  @And("^I prepare (?:REST|SOAP) (?:authentcation|authentication) username (.*) and password(?: ([^\\s]+))?$")
  public void prepareAuthentication(String username, String password) {
    Variables.set(API_USERNAME_VAR, VariablesTransformer.transformSingleValue(username));
    if (password == null) {
      Variables.set(API_PWORD_VAR, "");
    } else {
      Variables.set(API_PWORD_VAR, VariablesTransformer.transformSingleValue(password));
    }

    this.useBasicAuthentication = true;
  }

  /**
   * Send rest request with method.
   * <pre>Example:
   * And I send a REST POST request to /v1/applications/TENANT1/CreditEvaluation and receive status code HTTP 200</pre>
   *
   * @param method             the method
   * @param uri                the uri
   * @param expectedStatusCode the expected status code
   * @throws UnirestException the exception
   * @throws IOException      the exception
   */
  @And("^I send a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) and receive status code HTTP (\\d+)$")
  public void sendRestRequestWithMethod(String method, String uri, Integer expectedStatusCode)
      throws UnirestException, IOException {
    uri = VariablesTransformer.transformSingleValue(uri);

    restOperations.addURL(baseURL + uri);

    String response = "";
    String body = Variables.get(API_REQUEST_BODY_VAR);
    if (body == null) {
      body = "";
    }

    if (Variables.get(API_JWT_TOKEN_VAR) != null && !useBasicAuthentication) {
      response = restOperations.execute(HttpMethod.valueOf(method), queryFields, queryParameters,
                                        body, Variables.get(API_JWT_TOKEN_VAR), expectedStatusCode);
    } else {
      String username = Variables.get(API_USERNAME_VAR);
      String password = Variables.get(API_PWORD_VAR);
      response = restOperations.execute(HttpMethod.valueOf(method), username, password, body, expectedStatusCode);
      // reset to false as basic auth request has been made.
      this.useBasicAuthentication = false;
    }

    Variables.set(RESPONSE_VAR, response);
    queryParameters.clear();
    queryFields.clear();
    Variables.set(API_REQUEST_BODY_VAR, "");
  }

  /**
   * Send multiple times rest request with method.
   * <pre>Example:
   * And I send multiple REST POST request to /v1/applications/TENANT1/CreditEvaluation for 30 times and receive status code HTTP 200</pre>
   *
   * @param method             the method
   * @param uri                the uri
   * @param times              the x times
   * @param expectedStatusCode the expected status code
   * @throws UnirestException the exception
   * @throws IOException      the exception
   */
  @And("^I send multiple REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (\\d+) times and receive status code HTTP (\\d+)$")
  public void sendMultipleRestRequestWithMethod(String method, String uri, Integer times, Integer expectedStatusCode)
      throws UnirestException, IOException{

    String body = Variables.get(API_REQUEST_BODY_VAR);
    if (body == null) {
      body = "";
    }

    while(times != 0) {
      Variables.set(API_REQUEST_BODY_VAR, body);
      sendRestRequestWithMethod(method, uri, expectedStatusCode);
      times = times - 1;
    }
  }

  /**
   * Send rest request with method.
   * <pre>Example:
   * And I send a REST GET request to /v0/files/pcc-outbound/17bf718a-c865-4e05-b4fc-a2dcab204be5/file and receive status code HTTP 200 and save zip file in C:/distributor_handler_output/csv-actual-output/newFile.zip</pre>
   *
   * @param method             the method
   * @param uri                the uri
   * @param expectedStatusCode the expected status code
   * @param filePath           the path
   * @throws UnirestException the exception
   * @throws IOException      the exception
   */
  @And("^I send a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) and receive status code HTTP (\\d+) and save zip file in (.*)$")
  public void sendRestRequestWithMethodAndSaveAsFile(String method, String uri, Integer expectedStatusCode, String filePath)
                                                    throws UnirestException, IOException {

    uri = VariablesTransformer.transformSingleValue(uri);
    filePath = VariablesTransformer.transformSingleValue(filePath);

    restOperations.addURL(baseURL + uri);

    HttpResponse<String> response;
    String body = Variables.get(API_REQUEST_BODY_VAR);
    if (body == null) {
      body = "";
    }

    response = restOperations.execute(HttpMethod.valueOf(method), queryFields, queryParameters,
                                      body, Variables.get(API_JWT_TOKEN_VAR), expectedStatusCode, 10);


    InputStream is = response.getRawBody();
    File targetFile = new File(filePath);
    FileUtils.copyInputStreamToFile(is, targetFile);

    Variables.set(RESPONSE_VAR, response.getRawBody().toString());
    queryParameters.clear();
    queryFields.clear();
    Variables.set(API_REQUEST_BODY_VAR, "");
  }

  /**
   * Send rest request retry.
   * <pre>Example:
   * And I attempt to receive status code HTTP 200 for a REST POST request to /v0/files/strategy for 3 minutes</pre>
   * @param expectedStatusCode the expected status code
   * @param method             the method
   * @param uri                the uri
   * @param minutes            the minutes
   * @throws InterruptedException the exception
   */
  @And("^I attempt to receive status code HTTP (\\d+) for a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (.*) minute[s]?$")
  public void sendRestRequestRetry(Integer expectedStatusCode, String method, String uri, int minutes
  ) throws InterruptedException {
    long defaultSocketTimeout = 60000;

    long timeoutTime = System.currentTimeMillis() + minutes * defaultSocketTimeout;
    String stCode = null;
    do {
      try {
        sendRestRequestWithMethod(method, uri, expectedStatusCode);
        stCode = Variables.get(RESPONSE_VAR);
        //TODO This logic should be improved here AND TO BE TESTED
        if (stCode != null && !stCode.isEmpty()) {
          logger.info("Status is : " + stCode);
          break;
        }
      } catch (UnirestException | IOException | RuntimeException ex) {
        logger.debug("Exception was received!", ex); //NOI18N
      }
      Thread.sleep(10000);
    } while (System.currentTimeMillis() < timeoutTime);
    if (stCode == null) {
      throw new TestException(
          String.format("REST %s request to %s did not return HTTP status code %s in %s minute/s", method, uri, //NOI18N
                        expectedStatusCode, minutes));
    }
  }

  /**
   * Send rest request retry with interval of 10s.
   * <pre>Example:
   * And I attempt to receive status code HTTP 200 for a REST POST request to /v0/files/strategy for 3 minutes and response has fields:
   *   | $['status'] | COMPLETED |</pre>
   *
   * @param expectedStatusCode the expected status code
   * @param method             the HTTP method
   * @param uri                the REST endpoint uri
   * @param minutes            the number of minutes to keep retrying
   * @param paths              the response fields
   * @throws Exception from sendRestRequestRetryJSON
   */
  @And("^I attempt to receive status code HTTP (\\d+) for a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (.*) minute[s] and response has fields:$")
  public void sendRestRequestRetryJSON(Integer expectedStatusCode, String method, String uri, int minutes,
                                       List<List<String>> paths) throws Exception {
    sendRestRequestRetryJSON(expectedStatusCode, method, uri, minutes, 10000, paths);
  }

  /**
   * Send rest request retry with interval.
   *
   * <pre>Example:
   * And I attempt to receive status code HTTP 200 for a REST GET request to /pc-userjourney/files/${fileId}/deployables for 30 minutes with interval of 10000 milliseconds and response has fields:
   *   | $.[0].type | UNKNOWN                      |
   *   | $.[0].name | Business Process Composition |</pre>
   *
   * @param expectedStatusCode     the expected status code
   * @param method                 the HTTP method
   * @param uri                    the REST endpoint uri
   * @param minutes                the number of minutes to keep retrying
   * @param intervalInMilliseconds The interval between retries in milliseconds
   * @param paths                  the response fields
   * @throws Exception from sendRestRequestRetryJSONWithValidation
   */
  @And("^I attempt to receive status code HTTP (\\d+) for a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (.*) minute[s] with interval of (\\d+) milliseconds and response has fields:$")
  public void sendRestRequestRetryJSON(Integer expectedStatusCode, String method, String uri, int minutes,
                                       int intervalInMilliseconds,
                                       List<List<String>> paths) throws Exception {
    int retries = (minutes * 60 * 1000) / intervalInMilliseconds;
    sendRestRequestRetryJSONWithValidation(JSON_VALIDATION_FULL_MATCHED, expectedStatusCode, method, uri,
                                           intervalInMilliseconds, retries, paths);
  }

  /**
   * Retry request for x seconds with interval of 1s to ensure the HTTP status code and JSON fields exist.
   * <pre>Example:
   * And I attempt to receive status code HTTP 200 with response contains JSON fields listed below for a REST GET request to /pc-userjourney/files/${fileId}/deployables for 10 seconds:
   *   | $.[0].type |
   *   | $.[0].name |</pre>
   *
   * @param expectedStatusCode Expected HTTP response code.
   * @param method             REST HTTP Method to call
   * @param uri                REST HTTP URI to call
   * @param seconds            Retry for x seconds
   * @param expectedJsonpaths  List of jsonpath to be validated
   * @throws Exception from sendRestRequestRetryJSONWithValidation
   */
  @And("^I attempt to receive status code HTTP (\\d+) with response contains JSON fields listed below for a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (.*) seconds:$")
  public void sendRestRequestRetryJSONWithContainsFieldValidation(Integer expectedStatusCode, String method, String uri,
                                                                  int seconds, List<String> expectedJsonpaths) throws Exception {
    sendRestRequestRetryJSONWithContainsFieldValidation(expectedStatusCode, method, uri, seconds, 1000, expectedJsonpaths);
  }

  /**
   * Retry request for x seconds with interval of y milliseconds to ensure the HTTP status code and JSON fields exist.
   * <pre>Example:
   * And I attempt to receive status code HTTP 200 with response contains JSON fields listed below for a REST GET request to /pc-userjourney/files/${fileId}/deployables for 10 seconds with interval of 1000 milliseconds:
   *   | $.[0].type |
   *   | $.[0].name |</pre>
   *
   * @param expectedStatusCode     Expected HTTP response code.
   * @param method                 REST HTTP Method to call
   * @param uri                    REST HTTP URI to call
   * @param seconds                Retry for x seconds
   * @param intervalInMilliseconds The interval between retries in milliseconds
   * @param expectedJsonpaths      List of jsonpath to be validated
   * @throws Exception from sendRestRequestRetryJSONWithValidation
   */
  @And("^I attempt to receive status code HTTP (\\d+) with response contains JSON fields listed below for a REST (GET|POST|PUT|PATCH|DELETE) request to (.*) for (.*) seconds with interval of (\\d+) milliseconds:$")
  public void sendRestRequestRetryJSONWithContainsFieldValidation(Integer expectedStatusCode, String method, String uri,
                                                          int seconds, int intervalInMilliseconds, List<String> expectedJsonpaths) throws Exception {
    int retries = (seconds * 1000) / intervalInMilliseconds;
    sendRestRequestRetryJSONWithValidation(JSON_VALIDATION_CONTAINS_FIELD, expectedStatusCode, method, uri,
                                           intervalInMilliseconds, retries, expectedJsonpaths);
  }

  /**
   * Send REST HTTP that expect JSON response and perform validation against the validation method and criteria given.
   *
   * @param validationMethod   Validation method against JSON response. (match|contains)
   * @param expectedStatusCode Expected HTTP response code.
   * @param method             REST HTTP Method to call
   * @param uri                REST HTTP URI to call
   * @param retries            Number of retries
   * @param jsonCriteria       Validation criteria against JsonPath (match: table| contains: list)
   * @throws Exception from RetryExecutor
   */
  private void sendRestRequestRetryJSONWithValidation(String validationMethod, Integer expectedStatusCode,
                                                      String method, String uri, int intervalInMilliseconds,
                                                      int retries, Object jsonCriteria) throws Exception {

    // define list of valid validation methods here
    List<String> validValidationMethods = new ArrayList<>();
    validValidationMethods.add(JSON_VALIDATION_FULL_MATCHED);
    validValidationMethods.add(JSON_VALIDATION_CONTAINS_FIELD);
    String errMsgForInvalidValidationMethods = String.format("Validation method not supported. Expected: %s. Got: %s", //NOI18N
                                                             validValidationMethods, validationMethod);

    // Note: need to validate this before entering RetryExecutor, else it will considered as retry failing and keep looping
    if (!validValidationMethods.contains(validationMethod)) {
      throw new IllegalArgumentException(errMsgForInvalidValidationMethods);
    }

    // Note: retries+1 so that first attempt doesn't count as retry
    new RetryExecutor().delay(intervalInMilliseconds).retry(retries + 1).execute(() -> {

      try {
        String response = "";
        sendRestRequestWithMethod(method, uri, expectedStatusCode);
        response = Variables.get(RESPONSE_VAR);
        switch (validationMethod) {
          case JSON_VALIDATION_FULL_MATCHED:
            verifyResponseFieldsValues(response, RESPONSE_TYPE_JSON, (List<List<String>>) jsonCriteria);
            break;
          case JSON_VALIDATION_CONTAINS_FIELD:
            verifyResponseContainsFields(RESPONSE_TYPE_JSON, "", (List<String>) jsonCriteria);
            break;
          default:
            logger.error(errMsgForInvalidValidationMethods);
            break;
        }
        logger.info(String.format("Response %s criteria given. Response: %s", validationMethod, response)); //NOI18N
        return;
      } catch (UnirestException | IOException | RuntimeException | AssertionError e) {
        logger.warn(String.format(
            "REST %s request to %s did not return HTTP status code %s or criteria given %s. Got response: %s", //NOI18N
            method, uri, expectedStatusCode, jsonCriteria, Variables.get(RESPONSE_VAR)));
        throw new TestException(e);
      }

    });

  }

  /**
   * switch to another micro client jwt token.
   * <pre>Example:
   * And I prepare JWT token for client with id 121215545415</pre>
   *
   * @param clientIdArg      client id to use token
   */
  @And("^I prepare JWT token for client with id (.*)")
  public void clientJWTTokenSwitch(String clientIdArg) {
    String clientIdToUse = VariablesTransformer.transformSingleValue(clientIdArg);
    TokenUtils.useJwtTokenForClientId(clientIdToUse);
  }

  /**
   * switch to another micro client jwt token.
   * <pre>Example:
   * And I use JWT token of Micro client 1</pre>
   *
   * @param clientNb      client number from know list to use token
   */
  @And("^I use JWT token of Micro client (.*)")
  public void clientNbJWTTokenSwitch(String clientNb) {
    int clientNbToUse = Integer.parseInt(VariablesTransformer.transformSingleValue(clientNb));
    TokenUtils.useMicroClientJwtTokenByIndex(clientNbToUse-1);
  }

  /**
   * Gets jw token.
   * <pre>Example:
   * And I prepare JWT token with user (.*) and password (.*) from service (.*)</pre>
   *
   * @param user       the user
   * @param password   the password
   * @param serviceURL the service url
   * @throws UnirestException the exception
   */
  @And("^I prepare JWT token with user (.*) and password (.*) from service (.*)")
  public void getJWTtoken(String user, String password, String serviceURL) throws UnirestException {
    user = VariablesTransformer.transformSingleValue(user);
    password = VariablesTransformer.transformSingleValue(password);
    serviceURL = VariablesTransformer.transformSingleValue(serviceURL);

    restOperations.addURL(serviceURL);
    logger.debug("Token service url : " + serviceURL); //NOI18N
    logger.debug("Token user : " + user); //NOI18N
    String[] userDomain = user.split("@"); //NOI18N

    HashMap<String, String> headers = new HashMap<>();
    headers.put("Content-Type", MediaType.APPLICATION_JSON); //NOI18N
    headers.put("Accept", MediaType.APPLICATION_JSON); //NOI18N
    headers.put("X-Correlation-Id", String.valueOf(UUID.randomUUID())); //NOI18N
    headers.put("X-User-Domain", userDomain[1]); //NOI18N

    restOperations.addHeadersToRequest(headers);
    String response = null;
    int counter = 0;
    boolean tokenSuccess = false;

    String retry = Config.getOrDefault("token.service.retry", "10"); //NOI18N
    int maxTry = parseInt(retry);
    while ((counter != maxTry) && !tokenSuccess) {
      logger.debug("Asking for JWT Token ... try " + counter + " of " + maxTry + "."); //NOI18N
      try {
        response = restOperations.execute(HttpMethod.POST, user, password, 200); //NOI18N
        tokenSuccess = true;
      } catch (RuntimeException | UnirestException e) {
        counter++;
        if (counter == maxTry) {
          throw new TestException("Failed to get JWT token. " + e); //NOI18N
        }
      }
    }
    List<String> tokens = JsonPath.read(response, "$.*.access_token"); //NOI18N
    String token = tokens.get(0);
    Variables.set(API_JWT_TOKEN_VAR, token);
  }

  /**
   * Obtain token by providing the data in the body
   *
   * @param serviceURL  the URL of the token service
   * @param requestBody the body needed in the request
   * @throws UnirestException the UnirestException
   * @throws IOException      the IOException
   */
  @And("^I prepare JWT token from service (.*) with body:$")
  public void getJWTtokenWithBody(String serviceURL, String requestBody) throws UnirestException, IOException {

    serviceURL = VariablesTransformer.transformSingleValue(serviceURL);
    requestBody = VariablesTransformer.transformSingleValue(requestBody);

    Variables.set(API_REQUEST_BODY_VAR, requestBody);

    restOperations.addURL(serviceURL);
    logger.debug("Token service url : " + serviceURL); //NOI18N

    HashMap<String, String> headers = new HashMap<>();
    headers.put("Content-Type", MediaType.APPLICATION_JSON); //NOI18N
    headers.put("Accept", MediaType.APPLICATION_JSON); //NOI18N
    headers.put("X-Correlation-Id", String.valueOf(UUID.randomUUID())); //NOI18N
    headers.put("X-User-Domain", Config.get("cms.client.email.domain")); //NOI18N

    restOperations.addHeadersToRequest(headers);

    String response = restOperations.execute(HttpMethod.POST, queryFields, queryParameters, requestBody, null, 200); //NOI18N
    String token = JsonPath.read(response, "$.access_token"); //NOI18N

    Variables.set(API_JWT_TOKEN_VAR, token);
    Variables.set(API_REQUEST_BODY_VAR, "");
  }

  /**
   * Get JWT token for ExperianOne platform. (Support both apigee and non-apigee environment based on baseURL)
   *
   * @param user  Username
   * @param password  Password
   */
  @And("^I prepare JWT token with user (.*) and password (.*) for ExperianOne")
  public void getJwtTokenForExperianOne(String user, String password) {
    Boolean isApigee = false;
    // check whether we are calling via apigee
    String apigeeBaseUrl = Config.get("apigee.base.url");
    if (StringUtils.isNotEmpty(apigeeBaseUrl) && this.baseURL.contains(apigeeBaseUrl)) {
      isApigee = true;
    }
    String token = TokenUtils.getJwtToken(isApigee, user, password);
    Variables.set(API_JWT_TOKEN_VAR, token);
  }

  /**
   * Send soap request with method.
   * <pre>Example:
   * And I send a SOAP request to /globalweather.asmx and receive status code HTTP 200</pre>
   *
   * @param uri                the uri
   * @param expectedStatusCode the expected status code
   * @throws SOAPException    the throwable
   * @throws UnirestException the throwable
   * @throws IOException      the throwable
   */
  @And("^I send a SOAP request to (.*) and receive status code HTTP (\\d+)$")
  public void sendSoapRequestWithMethod(String uri, int expectedStatusCode)
      throws SOAPException, UnirestException, IOException {

    uri = VariablesTransformer.transformSingleValue(uri);

    soapOperations.addURL(baseURL + uri);
    String username = Variables.get(API_USERNAME_VAR);
    String password = Variables.get(API_PWORD_VAR);
    String body = Variables.get(API_REQUEST_BODY_VAR);
    if (body == null) {
      body = "";
    }

    String response;
    if (username == null && password == null) {
      response = soapOperations.execute(body, expectedStatusCode);
    } else {
      response = soapOperations.execute(username, password, body, expectedStatusCode);
    }

    Variables.set(RESPONSE_VAR, response);
    Variables.set(API_REQUEST_BODY_VAR, "");
  }

  /**
   * Add header to request.
   * <pre>Example:
   * And I add the following headers to the REST request:
   *   | Content-Type | application/json |
   *   | Accept       | application/json |</pre>
   * @param requestType the request type
   * @param headers     the headers
   */
  @And("^I add the following headers to the (REST|SOAP) request:$")
  public void addHeaderToRequest(String requestType, Map<String, String> headers) {
    switch (requestType) {
      case REST:
        restOperations.addHeadersToRequest(headers);
        break;
      case SOAP:
        soapOperations.addHeadersToRequest(headers);
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Add new cookies to request.
   * <pre>Example:
   * And I add the following cookies to the REST|SOAP request:
   *   | newcookie1 | newvalue1 |
   *   | newcookie2 | newvalue2 |</pre>
   * @param requestType the request type
   * @param cookies     the cookies
   */
  @And("^I add the following cookies to the (REST|SOAP) request:$")
  public void addNewCookiesToRequest(String requestType, Map<String, String> cookies) {
    switch (requestType) {
      case REST:
        restOperations.addNewCookieToRequest(cookies);
        break;
      case SOAP:
        soapOperations.addNewCookieToRequest(cookies);
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Include existing cookies to request.
   * <pre>Example:
   * And I include following existing cookies to the REST|SOAP request:
   *   | oldcookie1 |
   *   | oldcookie2 |</pre>
   * @param requestType the request type
   * @param cookies     the cookies
   */
  @And("^I add the following existing cookies to the (REST|SOAP) request:$")
  public void includeExistingCookiesToRequest(String requestType, List<String> cookies) {
    switch (requestType) {
      case REST:
        restOperations.addExistingCookieToRequest(cookies);
        break;
      case SOAP:
        soapOperations.addExistingCookieToRequest(cookies);
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Disable cookie state management.
   * <pre>Example:
   * And I disable cookie state management for REST requests</pre>
   * @param requestType the request type
   */
  @And("^I disable cookie state management for (REST|SOAP) requests$")
  public void disableCookieStateManagement(String requestType) {
    switch (requestType) {
      case REST:
        restOperations.disableUnirestCookieStateManagement();
        break;
      case SOAP:
        soapOperations.disableUnirestCookieStateManagement();
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Sets hmac signature.
   * <pre>Example:
   * And I set HMAC signature to the request with key 1234-abcd-5678-efgj</pre>
   * @param hmacKey the hmac key
   * @throws InvalidKeyException      the invalid key exception
   * @throws NoSuchAlgorithmException the no such algorithm exception
   */
  @And("^I set HMAC signature to the request with key (.*)$")
  public void setHMACSignature(String hmacKey) throws InvalidKeyException, NoSuchAlgorithmException {
    hmacKey = VariablesTransformer.transformSingleValue(hmacKey);

    String body = Variables.get(API_REQUEST_BODY_VAR);

    Mac sha256HMAC = Mac.getInstance("HmacSHA256"); //NOI18N
    SecretKeySpec secretKey = new SecretKeySpec(hmacKey.getBytes(), "HmacSHA256"); //NOI18N
    sha256HMAC.init(secretKey);

    String hash = Base64.encodeBase64String(sha256HMAC.doFinal(body.getBytes()));
    Map<String, String> headers = new HashMap<>();
    headers.put("Hmac-signature", hash); //NOI18N
    restOperations.addHeadersToRequest(headers);
  }

  /**
   * Verify response with schema.
   * <pre>Example:
   * And I verify that the XML response follows the schema:
   *   """
   *   < ?xml version="1.0"?>
   *   < xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
   *   < xs:element name="note">
   *     < xs:complexType>
   *       < xs:sequence>
   *         < xs:element name="to" type="xs:string"/>
   *         < xs:element name="from" type="xs:string"/>
   *         < xs:element name="heading" type="xs:string"/>
   *         < xs:element name="body" type="xs:string"/>
   *       < /xs:sequence>
   *     < /xs:complexType>
   *   < /xs:element>
   *   < /xs:schema>
   *   """</pre>
   * @param responseType the response type
   * @param schema       the schema
   * @throws IOException         the exception
   * @throws ProcessingException the exception
   */
  /*
    And I verify that the XML response follows the schema:
  """
  <?xml version="1.0"?>
  <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="note">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="to" type="xs:string"/>
        <xs:element name="from" type="xs:string"/>
        <xs:element name="heading" type="xs:string"/>
        <xs:element name="body" type="xs:string"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
  </xs:schema>
  """
  */
  @And("^I verify that the (JSON|XML) response follows the schema:$")
  public void verifyResponseWithSchema(String responseType, String schema) throws IOException, ProcessingException {
    String response = Variables.get(RESPONSE_VAR);
    switch (responseType) {
      case JSON:
        assertTrue(new JSONOperations().validateJSON(response, schema));
        break;
      case XML:
        assertTrue(new XMLOperations().validateXML(response, schema));
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_RESPONSE_MESSAGE);

    }
  }

  /**
   * Verify response with schema file.
   * <pre>Example:
   * And I verify that the JSON response follows the schema file ${features.path}/ACF/data/schema.json</pre>
   * @param responseType   the response type
   * @param schemaFilePath the schema file path
   * @throws IOException         the exception
   * @throws ProcessingException the exception
   */
  @And("^I verify that the (JSON|XML) response follows the schema file (.*)$")
  public void verifyResponseWithSchemaFile(String responseType, String schemaFilePath)
      throws IOException, ProcessingException {

    schemaFilePath = VariablesTransformer.transformSingleValue(schemaFilePath);

    schemaFilePath = FilenameUtils.separatorsToUnix(
        schemaFilePath);
    File schemaFile = new File(schemaFilePath);
    verifyResponseWithSchema(responseType, FileUtils.readFileToString(schemaFile, StandardCharsets.UTF_8));
  }

  /**
   * Verifies SOAP response values
   *
   * @param rootElementPath the path to the root element to be evaluated
   * @param paths           the xPath that will be checked
   */
  @And("^I verify that the SOAP response(?: starting from root element (.*))? has fields:$")
  public void verifySOAPResponseFieldsValues(String rootElementPath, List<List<String>> paths) {
    String response = Variables.get(RESPONSE_VAR);

    if (StringUtils.isNotEmpty(rootElementPath)) {
      response = new XMLOperations().evaluateXPath(response, rootElementPath);
    }

    verifyResponseFieldsValues(response, XML, paths);
  }

  /**
   * Verify response fields values.
   * <pre>Examples:
   * And I verify that the JSON response has fields:
   *   | $.data.['Results-DV.RSLT.Pst-B-Policy-Decision-Text'] | Accept |
   *
   * And I verify that the JSON response has fields:
   *   | $.[0].['permissions'] | ["CREATE","UPDATE"] |</pre>
   * @param responseType the response type
   * @param paths        the paths
   * @throws Throwable the throwable
   */
  @And("^I verify that the (JSON|XML) response has fields:$")
  public void verifyJSONXMLResponseFieldsValues(String responseType, List<List<String>> paths) throws Throwable {
    String response = Variables.get(RESPONSE_VAR);
    verifyResponseFieldsValues(response, responseType, paths);
  }

  /**
   * Verify response fields values using either a total or partial comparison against a given JSON String.
   * <p>
   * NOTE: In Lenient mode, comparison will expect the same number of objects in a response array; these can be left undefined (see example).
   * <pre>Example:
   * And I verify that the JSON response contains the following JSON string:
   *   """
   *   {
   *    "data":{
   *      "Applicant":{
   *         "Forename":"Deannie",
   *         "Surname":"Lim",
   *         "Date of birth":"19800909",
   *         "Address Details":[
   *           {
   *             "Country":"Malaysia",
   *             "DateMoved":"20011212"
   *           },
   *           {
   *           },
   *           {
   *           },
   *           {
   *           }
   *         ]
   *       }
   *     }
   *   }
   *   """</pre>
   *
   * @param comparisonType whether to expect an exact match or be lenient (equal or more json in the response)
   * @param expectedStr    the expected JSON response, as a String
   */
  @And("^I verify that the JSON response (contains|is exactly) the following JSON string:$")
  public void verifyJSONResponseFieldsValuesAgainstString(String comparisonType, String expectedStr) {
    String response = Variables.get(RESPONSE_VAR);
    expectedStr = VariablesTransformer.transformSingleValue(expectedStr);
    boolean strictCompare = "is exactly".equals(comparisonType); //NOI18N
    JSONAssert.assertEquals(expectedStr, response, strictCompare);
  }

  /**
   * Verify JSON response contains or does not contain the given list of values.
   * <pre>Example:
   * Then I verify that the JSON response message contains:
   *   | Row [0] is invalid. Reason: - Row 0 , Column ApplicationDataView.Applicant.Annual Salary exceeds the expected decimal length(30) |
   *   | Row 0, Column ApplicationDataView.Applicant.Date of birth is an invalid date                                                     |
   *   | Row 0 , Column ApplicationDataView.Applicant.Other Income exceeds the expected float length(15)                                  |</pre>
   *
   * @param validation action to validate the json etc contains or does not contain
   * @param message the list of values to compare against
   */
  @And("^I verify that the JSON response message (contains|does not contain):$")
  public void verifyResponseMessageContains(String validation, List<String> message) {
    String response = Variables.get(RESPONSE_VAR);
    // define list of valid validation methods here
    List<String> validValidationMethods = new ArrayList<>();
    validValidationMethods.add(JSON_VALIDATION_CONTAINS_FIELD);
    validValidationMethods.add(JSON_VALIDATION_NOT_CONTAINS_FIELD);
    switch (validation) {
      case JSON_VALIDATION_CONTAINS_FIELD:
        for (String msg : message) {
          msg = VariablesTransformer.transformSingleValue(msg);
          Assert.assertTrue(response.contains(msg), "Expected response contains: " + msg + " but got: " + response);
        }
        break;
      case JSON_VALIDATION_NOT_CONTAINS_FIELD:
        for (String msg : message) {
          msg = VariablesTransformer.transformSingleValue(msg);
          Assert.assertFalse(response.contains(msg), "Expected response does not contain: " + msg + " but got: " + response);
        }
        break;
      default:
        logger.error(String.format("Validation method not supported. Expected: %s. Got: %s", //NOI18N
                validValidationMethods, validation));
        break;
    }
  }

  /**
   * Step that verifies the JSON response against a golden file with parameter to ignore some JSON paths, e.g. IDs,
   * submission/execution date, etc.
   * <pre>Example:
   * Then I compare the JSON response with file ${features.path}/POC-CreditMatch/data/A001/<ResponseFile> excluding:
   *   | $.data.['In Tran Data.APSTranID'] |  |  | $.serviceContextId |  |</pre>
   *
   * @param filePath   path to the golden file
   * @param properties data table with json paths to be ignored
   * @throws Throwable exception
   */
  @And("^I compare the JSON response with file (.*) excluding:$")
  public void verifyResponseMatchesFile(String filePath, Map<String, String> properties) throws Throwable {
    FileComparisonSteps fileComparisonSteps = new FileComparisonSteps();
    fileComparisonSteps.compareJSONFileExcludingFields(filePath, Variables.get(RESPONSE_VAR), properties);
  }

  /**
   * Compare JSON file using compare mode with exclusion of the dynamic data where its JSONPath defined in the datatable.
   * Dynamic data defined in JSONPath will be match against the regex.
   * <pre>Example :
   * And I compare the JSON response with file ${features.path}/client-gateway/data/client-gateway-log-level.json with compare mode LENIENT and dynamic data matches regex:
   *   | JsonPath   | Regex Pattern                                |
   *   | deployment | \{("${deployment.pod.regex}":"DEBUG"(,*))+\} |
   *   | bpe        | \{("${bps.pod.regex}":"DEBUG"(,*))+\}        |
   *   | da         | \{("${bps.pod.regex}":"25"(,*))+\}           |
   *   | webEngine  | \{\}                                         |
   *   | compiler   | \{\}                                         |</pre>
   *
   * @param goldenFile  JSON file to be compared against
   * @param compareMode compare mode (STRICT|LENIENT|NON_EXTENSIBLE|STRICT_ORDER)
   * @param dataTable   JSON Path vs. Regex Pattern to compare against
   * @throws Throwable from compareJSONFileWithRegex
   */
  @And("^I compare the JSON response with file (.*) with compare mode (STRICT|LENIENT|NON_EXTENSIBLE|STRICT_ORDER) and dynamic data matches regex:$")
  public void verifyResponseMatchesRegex(String goldenFile, JSONCompareMode compareMode, List<List<String>> dataTable) throws Throwable {
    FileComparisonSteps fileComparisonSteps = new FileComparisonSteps();
    fileComparisonSteps.compareJSONFileWithRegex(goldenFile, Variables.get(RESPONSE_VAR), compareMode, dataTable);
  }

  /**
   * Verify response contains fields.
   * <pre>Example:
   * And I verify that the XML response does not have fields:
   *   |$.data.['Results-DV.RSLT.Pre-B-Policy-Decision-Text']|</pre>
   *
   * @param responseType  the response type
   * @param containsField the contains field
   * @param paths         the paths
   */
  @And("^I verify that the (JSON|XML) response(:? does not)? have fields:$")
  public void verifyResponseContainsFields(String responseType, String containsField, List<String> paths) {
    String response = Variables.get(RESPONSE_VAR);
    SoftAssert softAssert = new SoftAssert();

    for (String path : paths) {
      boolean keyExists = true;
      try {
        switch (responseType) {
          case JSON:
            new JSONOperations().evaluateJSONPath(response, path);
            break;
          case XML:
            if ((new XMLOperations().evaluateXPath(response, path)).equals("")) {
              throw new XPathExpressionException("Cannot find node with path " + path); //NOI18N
            }
            break;
          default:
            throw new IllegalArgumentException(UNSUPPORTED_RESPONSE_MESSAGE);
        }
      } catch (PathNotFoundException | XPathExpressionException e) {
        logger.debug("Path not found or path expression exception was received!", e); //NOI18N
        keyExists = false;
      } finally {
        if (StringUtils.isEmpty(containsField)) {
          softAssert.assertTrue(keyExists, "Expression " + path + " returns " + responseType + " key");
        } else {
          softAssert.assertFalse(keyExists, "Expression " + path + " returns " + responseType + " key");
        }
      }
    }
    softAssert.assertAll();
  }

  /**
   * Verify response header values.
   * <pre>Example:
   * And I verify that the REST response header has values:
   *   | Allow | [GET,HEAD] |</pre>
   *
   * @param requestType the request type
   * @param headers     the headers
   */
  @And("^I verify that the (REST|SOAP) response header has values:$")
  public void verifyResponseHeaderValues(String requestType, Map<String, String> headers) {
    String headerName;
    headers = VariablesTransformer.transformMap(headers);

    for (Map.Entry<String, String> header : headers.entrySet()) {
      headerName = Variables.get(RESPONSE_HEADERS + "." + header.getKey()); //NOI18N

      // header might return as small letters, in fact it's case in-sensitive
      //  if expected capitalize not returned as expected, look for similar one in small letters
      if(headerName == null) {
        headerName = Variables.get(RESPONSE_HEADERS + "." + header.getKey().toLowerCase()); //NOI18N
      }

      assertNotNull(headerName, requestType + " response header " + header.getKey());
      assertEquals(headerName, header.getValue());
    }
  }

  /**
   * Verify response header existence.
   * <pre>Example:
   * And I check the existence of the following response headers:
   *   | Pragma          | exists         |
   *   | X-Frame-Options | Does not exist |</pre>
   *
   * @param headers the headers
   */
  @And("^I check the existence of the following response headers:$")
  public void verifyResponseHeaderExistence(Map<String, String> headers) {
    headers = VariablesTransformer.transformMap(headers);

    for (Map.Entry<String, String> header : headers.entrySet()) {
      if (header.getValue().equalsIgnoreCase("exists")) { //NOI18N
        assertNotNull(Variables.get(RESPONSE_HEADERS + "." + header.getKey()));
      } else if (header.getValue().equalsIgnoreCase("does not exist")) { //NOI18N
        assertNull(Variables.get(RESPONSE_HEADERS + "." + header.getKey()));
      } else {
        throw new IllegalArgumentException(
            "Unrecognized header state: [" + header.getValue() + "] for header [" + header.getKey() + "]"); //NOI18N
      }
    }
  }

  /**
   * Verify response cookie values.
   * <pre>Example:
   * And I verify REST|SOAP response cookie values:
   *   | mycookie | asdf |</pre>
   *
   * @param responseType the response type
   * @param cookies      the cookies
   */
  @And("^I verify (REST|SOAP) response cookie values:$")
  public void verifyResponseCookieValues(String responseType, Map<String, String> cookies) {
    for (Entry<String, String> entry : cookies.entrySet()) {
      String cookieValue = APIOperations.getResponseCookie(entry.getKey());
      Assert.assertEquals(cookieValue, entry.getValue());
    }
  }

  /**
   * Verify response contains cookies.
   * <pre>Example:
   * And I verify REST|SOAP response contains cookies:
   *   | token |</pre>
   * @param responseType the response type
   * @param cookies      the cookies
   */
  @And("^I verify (REST|SOAP) response contains cookies:$")
  public void verifyResponseContainsCookies(String responseType, List<String> cookies) {
    for (String cookie : cookies) {
      assertNotNull(APIOperations.getResponseCookie(cookie), responseType + " response header " + cookie);
    }
  }

  /**
   * Save SOAP response value with CDATA in variables
   * <pre>Example(s):
   *SOAP response:
   *  < soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   *    < soap:Body>
   *      < ns2:ougWSResponse xmlns:ns2="http://ougwebcomponent.components.oug.osgi.scorex.com/">
   *        < data_flux>
   *          < item>bps.output< /item>
   *            < item><![CDATA[< MainOutput>
   *              < ApplicationDetails>
   *                < PowerCurveID>0000000000010891< /PowerCurveID>
   *              < /ApplicationDetails>
   *              < DecisionDetails>
   *                < DecisionText>Accept< /DecisionText>
   *              < /DecisionDetails>
   *            < /MainOutput>]]>< /item>
   *        < /data_flux>
   *      < /ns2:ougWSResponse>
   *    < /soap:Body>
   *  < /soap:Envelope>
   *
   *To save a SOAP response field without a CDATA in variable :
   *  And I save XML response matching expression //data_flux/item[preceding-sibling::item[contains(text(),'return code')]] as variable returnCodeVar3
   *
   *  To save a SOAP response field wrapped in CDATA in variable :
   *  In the first row pass the path from the root element to the CDATA block
   *  In the second row pass the path to the element that you want to save
   *  In the third row pass the name of the variable
   *  Example:
   *   And I save SOAP CDATA responses as variables:
   *     | Envelope/Body/ougWSResponse/data_flux[item='bps.output']/item[2] | MainOutput/DecisionDetails/Decision | decision |
   *
   *   You can save one or more variables. Example:
   *   And I save SOAP CDATA responses as variables:
   *     | Envelope/Body/ougWSResponse/data_flux[item='bps.output']/item[2] | MainOutput/ApplicationDetails/PowerCurveID | powerCurveID |
   *     | Envelope/Body/ougWSResponse/data_flux[item='bps.output']/item[2] | MainOutput/DecisionDetails/Decision        | decision     |
   *     | Envelope/Body/ougWSResponse/data_flux[item='bps.output']/item[2] | MainOutput/DecisionDetails/DecisionText    | decisionText |</pre>
   *
   * @param data data table information
   */
  @And("^I save SOAP CDATA response as variables:$")
  public void saveSoapResponsesInVariables(List<List<String>> data) {
    for (int i = 0; i < data.size(); i++) {
      String response = Variables.get(RESPONSE_VAR);
      String rootElementPath = data.get(i).get(0);
      Map<String, String> pathsMap = new HashMap<>();
      pathsMap.put(data.get(i).get(1), data.get(i).get(2));
      response = new XMLOperations().evaluateXPath(response, rootElementPath);
      saveResponseInVariables(response, XML, pathsMap);
    }
  }

  /**
   * Save response in variable.
   * <pre>Example:
   * And I save XML response matching expression //data_flux/item[preceding-sibling::item[contains(text(),'return code')]] as variable returnCodeVar3</pre>
   *
   * @param responseType   the response type
   * @param findExpression the find expression
   * @param variable       the variable
   */
  @And("^I save (JSON|XML) response matching expression (.*) as variable (.*)$")
  public void saveResponseInVariable(String responseType, String findExpression, String variable) {
    String response = Variables.get(RESPONSE_VAR);
    switch (responseType) {
      case JSON:
        Variables.set(variable, new JSONOperations().evaluateJSONPath(response, findExpression));
        break;
      case XML:
        Variables.set(variable, new XMLOperations().evaluateXPath(response, findExpression));
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_RESPONSE_MESSAGE);
    }
  }

  /**
   * Save response in multiple variables.
   * <pre>Example(s):
   * And I save JSON response as variables:
   *   | $.[0].client_guid | clientId      |
   *   | $.[1].version     | clientVersion |
   *
   * And I save XML response as variables:
   *   | //data_flux/item[preceding-sibling::item[contains(text(),'return code')]]   | returnCodeVar   |
   *   | //data_flux/item[preceding-sibling::item[contains(text(),'workflow_name')]] | workFlowNameVar |</pre>
   *
   * @param responseType the response type
   * @param paths        the paths
   */
  @And("^I save (JSON|XML) response as variables:$")
  public void saveResponseInMultipleVariables(String responseType, Map<String, String> paths) {
    String response = Variables.get(RESPONSE_VAR);
    paths = VariablesTransformer.transformMap(paths);
    saveResponseInVariables(response, responseType, paths);
  }

  /**
   * Sets request connection timeout.
   * <pre>Example:
   * And I set connection timeout to 1000 for REST requests</pre>
   *
   * @param connectionTimeout the connection timeout
   * @param requestType       the request type
   */
  @And("^I set connection timeout to (\\d+) for (REST|SOAP) requests$")
  public void setRequestConnectionTimeout(long connectionTimeout, String requestType) {
    long defaultSocketTimeout = 60000;

    switch (requestType) {
      case REST:
        restOperations.setUnirestTimeouts(connectionTimeout, defaultSocketTimeout);
        break;
      case SOAP:
        soapOperations.setUnirestTimeouts(connectionTimeout, defaultSocketTimeout);
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Sets request socket timeout.
   * <pre>Example:
   * And I set socket timeout to 100000 for REST requests</pre>
   *
   * @param socketTimeout the socket timeout
   * @param requestType   the request type
   */
  @And("^I set socket timeout to (\\d+) for (REST|SOAP) requests$")
  public void setRequestSocketTimeout(long socketTimeout, String requestType) {
    long defaultConnectionTimeout = 10000;

    switch (requestType) {
      case REST:
        restOperations.setUnirestTimeouts(defaultConnectionTimeout, socketTimeout);
        break;
      case SOAP:
        soapOperations.setUnirestTimeouts(defaultConnectionTimeout, socketTimeout);
        break;
      default:
        throw new IllegalArgumentException(UNSUPPORTED_REQUEST_MESSAGE + requestType);
    }
  }

  /**
   * Generate jwt via tool.
   * <pre>Example(s):
   * And I generate JWT via a tool with properties:
   *   | jwt.algName         | RS256                                   |
   *   | jwt.permissions     | Print-DataViewer                        |
   *   | jwt.roles           | Administrators                          |
   *   | jwt.userId          | admin                                   |
   *   | jwt.claim.tenantId  | TENANT1                                 |
   *   | jwt.claim.iss       | rhea                                    |
   *   | jwt.claim.aud       | saas                                    |
   *   | jwt.validityMin     | 60                                      |
   *
   * And I generate JWT via a tool with keypair from base64 with properties:
   *   | jwt.algName         | es256                                   |
   *   | jwt.privateKey      | ${PRIVATE KEY}                          |
   *   | jwt.publicKey       | ${PUBLIC KEY}                           |
   *   | jwt.permissions     | Get-ScheduledJobs,Put-Strategy          |
   *   | jwt.roles           | CLIENT_SYSTEM_ADMIN,Risk Analyst        |
   *   | jwt.client          | admin                                   |
   *   | jwt.claim.tenantId  | TENANT1                                 |
   *   | jwt.claim.iss       | rhea                                    |
   *   | jwt.claim.aud       | saas                                    |
   *   | jwt.validityMin     | 60                                      |
   *
   * And I generate JWT via a tool with keypair from keystore with properties:
   *   | jwt.algName         | es256                                   |
   *   | jwt.header.kid      | es256.jwt.sign.20170401.1               |
   *   | jwt.ksType          | JKS                                     |
   *   | jwt.ksPassword      | secret123                               |
   *   | jwt.ksKeysPath      | ${path}/jwt-keys.jks                    |
   *   | jwt.permissions     | NONE                                    |
   *   | jwt.roles           | Administrators                          |
   *   | jwt.userId          | admin                                   |
   *   | jwt.claim.tenantId  | TENANT1                                 |
   *   | jwt.claim.iss       | rhea                                    |
   *   | jwt.claim.aud       | saas                                    |
   *   | jwt.validityMin     | 60                                      |
   *
   * And I generate invalid JWT via a tool with keypair from base64 with properties:
   *   | jwt.algName        | es256                                                                           |
   *   | jwt.privateKey     | ${mock.jwt.privateKey}                                                          |
   *   | jwt.publicKey      | ${mock.jwt.publicKey}                                                           |
   *   | jwt.permissions    | Upload-StrategyFile,Download-StrategyFile,View-StrategyFile,Delete-StrategyFile |
   *   | jwt.roles          | non_existing_role                                                               |
   *   | jwt.claim.iss      | token-service                                                                   |
   *   | jwt.claim.aud      | saas                                                                            |
   *   | jwt.validityMin    | 2                                                                               |</pre>
   * @param invalidToken  the invalid token
   * @param keyPairSource the key pair source
   * @param properties    the properties
   */
  @And("^I generate( invalid)? JWT via a tool(?: with keypair from (base64|keystore))? with properties:$")
  public void generateJwtViaTool(String invalidToken, final String keyPairSource, Map<String, String> properties) {
    properties = VariablesTransformer.transformMap(properties);

    if (keyPairSource != null) {
      properties.put("jwt.keyPair", keyPairSource); //NOI18N
    }

    JwtGenerator jwtGenerator = new JwtGenerator();

    Variables.set(API_JWT_TOKEN_VAR, jwtGenerator.generateJwtToken(properties, StringUtils.isEmpty(invalidToken)));
    Variables.set("API_JWT_PUBLIC_KEY", jwtGenerator.getPublicKey()); //NOI18N
  }

  /**
   * Save header of the HTTP response as variable.
   *
   * @param header   the response header
   * @param variable the variable
   */
  @And("^I save HTTP response header (.*) as variable (.*)$")
  public void saveHTTPResponseHeader(String header, String variable) {
    String responseHeader = Variables.get(RESPONSE_HEADERS + "." + header); //NOI18N
    Variables.set(variable, responseHeader);
  }

  /**
   * Save response fields values in variables.
   *
   * @param response     received response that we have
   * @param responseType the response type
   * @param paths        Map that contains the path and the name of the variable
   */
  private void saveResponseInVariables(String response, String responseType, Map<String, String> paths) {
    for (Map.Entry<String, String> path : paths.entrySet()) {
      switch (responseType) {
        case JSON:
          Variables.set(path.getValue(), new JSONOperations().evaluateJSONPath(response, path.getKey()));
          break;
        case XML:
          Variables.set(path.getValue(), new XMLOperations().evaluateXPath(response, path.getKey()));
          break;
        default:
          throw new IllegalArgumentException(UNSUPPORTED_RESPONSE_MESSAGE);
      }
    }
  }

  /**
   * To save JSON response into a file.
   *
   * @param filePath the file path
   * @throws IOException the exception
   *
   * Usage example:
   * And I save JSON response into c:/temp/abc.json file
   */
  @And("^I save JSON response into (.*) file$")
  public void saveJsonResponseToFile(String filePath) throws IOException {
    filePath = VariablesTransformer.transformSingleValue(filePath);
    File targetFile = new File(filePath);

    if (targetFile.exists()) {
      PrintWriter writer = new PrintWriter(filePath);
      writer.print("");
      writer.close();
    }

    String content = Variables.get(RESPONSE_VAR);
    logger.info("Saving JSON response into " + filePath);
    FileUtils.writeByteArrayToFile(targetFile, content.trim().getBytes());
    assertFalse(FileUtils.readFileToString(targetFile, Charset.defaultCharset()).isEmpty(), filePath + " is empty: JSON response did not save successfully.");
  }

  /**
   * Verify response fields values.
   *
   * @param response     the response message
   * @param responseType the response type
   * @param paths        the paths
   */
  public void verifyResponseFieldsValues(String response, String responseType, List<List<String>> paths) {
    paths = VariablesTransformer.transformTable(paths);
    SoftAssert softAssert = new SoftAssert();

    for (List<String> path : paths) {
      if (path.size() == 1) {
        verifyResponseContainsFields(responseType, null, path);
      } else {
        switch (responseType) {
          case JSON:
            JSONOperations jsonOperations = new JSONOperations();
            String jsonPath = path.get(0);
            String actual = jsonOperations.evaluateJSONPath(response, jsonPath).trim();
            String expected = path.get(1);
            softAssert.assertTrue(
                jsonOperations.compareValues(actual, expected), String.format(
                    "Expected response value is %s but got %s. Jsonpath: %s.", expected, actual, jsonPath)); // NOI18N
            break;
          case XML:
            softAssert.assertEquals(new XMLOperations().evaluateXPath(response, path.get(0)).trim(), path.get(1));
            break;
          default:
            throw new IllegalArgumentException(UNSUPPORTED_RESPONSE_MESSAGE);
        }
      }
    }

    try {
      softAssert.assertAll();
    } catch (AssertionError err) {
      logger.debug(String.format("%s%nFull %s response: %s.", err.getMessage(), responseType, response)); //NOI18N
      throw err;
    }
  }

  /**
   * Encode the special characters for a url path.
   * <pre>Example:
   * And I encode the special characters for url ${urlPathParameter} twice as variable documentKey</pre>
   * @param urlPath       url pathParameter that we have
   * @param timesToEncode number of times to encode (once|twice)
   * @param variable      the variable
   * @throws UnsupportedEncodingException the exception
   */
  @And("^I encode the special characters for url (.*) (once|twice) as variable (.*)")
  public void encodeTheUrl(String urlPath, String timesToEncode, String variable) throws UnsupportedEncodingException {
    urlPath = VariablesTransformer.transformSingleValue(urlPath);
    String encodedUrl = URLEncoder.encode(urlPath, StandardCharsets.UTF_8.toString());
    if ("twice".equals(timesToEncode)) { //NOI18N
      encodedUrl = URLEncoder.encode(encodedUrl, StandardCharsets.UTF_8.toString());
    }
    Variables.set(variable, encodedUrl);
  }

  /**
   * Normalise url given.
   * @param url Url to be normalised
   * @param variable  Variable to save Url that is being normalised
   * @return Normalised url
   * @throws URISyntaxException from normalize()
   */
  @And("^I normalise url (.*) and save as variable (.*)")
  public String normaliseUrl(String url, String variable) throws URISyntaxException {
    url = VariablesTransformer.transformSingleValue(url);
    URI uri = new URI(url);
    url = uri.normalize().toString();
    Variables.set(variable, url);
    return url;
  }

  /**
   * Adding port to existing url.
   * @param port Port to be added
   * @param url   Url without port
   * @param variable  Variable to save the url with port
   * @return  Url with port
   * @throws URISyntaxException from URI
   */
  @And("^I add port (\\S*) to url (\\S*) and save as variable (.*)")
  public String addPortToUrl(String port, String url, String variable) throws URISyntaxException {
    port = VariablesTransformer.transformSingleValue(port);
    url = VariablesTransformer.transformSingleValue(url);
    URI uri = new URI(url);
    uri = UriBuilder.fromUri(uri).port(Integer.parseInt(port)).build();
    url = uri.normalize().toString();
    Variables.set(variable, url);
    return url;
  }

  /**
   * Get host from given URL.
   * @param url   target URL
   * @param variable  Variable to save host
   * @return Host of URL
   * @throws URISyntaxException from URI
   */
  @And("^I get host from url (\\S*) and save as variable (.*)")
  public String getHostFromUrl(String url, String variable) throws URISyntaxException {
    url = VariablesTransformer.transformSingleValue(url);
    URI uri = new URI(url);
    String host = uri.normalize().getHost();
    Variables.set(variable, host);
    return host;
  }

  /**
   * Getting the XSRF-TOKEN from JWT Token and save it as variable
   * <pre>Example:
   * And I save XSRF-TOKEN as xsrfToken variable</pre>
   *
   * @param variableName  name of the variable that saving the XSRF token
   */
  @And("^I save XSRF-TOKEN as (.*) variable$")
  public void saveXSRF(String variableName){
    String jwtToken = Variables.get(API_JWT_TOKEN_VAR);
    Variables.set(variableName, TokenUtils.getXsrfToken(jwtToken));
  }
}
