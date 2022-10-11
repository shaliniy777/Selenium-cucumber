/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Option;
import com.mashape.unirest.http.options.Options;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mashape.unirest.request.body.MultipartBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.w3c.dom.Document;

import javax.net.ssl.SSLHandshakeException;

/**
 * The type Rest operations.
 */
public class RESTOperations extends APIOperations {

  /**
   * The constant RESPONSE_VARIABLE.
   */
  public static final String RESPONSE_VARIABLE = "RESTResponse"; //NOI18N
  private static final long SLEEP_TIME_BETWEEN_CALLS_MILLISECONDS = 2000; //2 sec
  private List<Integer> returnCodeToRetry = new ArrayList<>();
  private int callRetryCount = 10;

  /**
   * The Additional headers.
   */
  public List<String> additionalHeaders = Arrays.asList();
  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Rest operations.
   */
  public RESTOperations() {
    Unirest.setDefaultHeader("Content-Type", "application/json"); //NOI18N
    Unirest.setDefaultHeader("Accept", "application/json;charset=utf-8"); //NOI18N
    returnCodeToRetry.add(503);
    returnCodeToRetry.add(504);
  }

  public List<Integer> getReturnCodeToRetry() {
    return this.returnCodeToRetry;
  }
  public void setReturnCodeToRetry(List<Integer> returnCodeToRetry) {
    this.returnCodeToRetry = returnCodeToRetry;
  }
  public int getCallRetryCount() {
    return this.callRetryCount;
  }
  public void setCallRetryCount(int callRetryCount) {
    this.callRetryCount = callRetryCount;
  }

  /**
   * Instantiates a new Rest operations.
   *
   * @param url the url
   */
  public RESTOperations(String url) {
    this();
    this.url = url;
  }

  /**
   * Instantiates a new Rest operations.
   *
   * @param baseUrl the base url
   * @param uri     the uri
   */
  public RESTOperations(String baseUrl, String uri) {
    this();
    url = VariablesTransformer.transformSingleValue(baseUrl + uri);
  }

  /**
   * Execute async.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param body               the body
   * @param queryParams        the query params
   * @param expectedStatusCode the expected status code
   * @throws Exception the exception
   */
  public void executeAsync(HttpMethod method, String userName, String password, Document body,
      Map<String, String> queryParams, int expectedStatusCode) throws Exception {

    HttpRequest request = createRequest(method, queryParams, XMLOperations.documentToString(body));

    setAuthentication(request, userName, password);

    request.asStringAsync(new Callback<String>() {

      public void failed(UnirestException e) {
        logger.warn("The asynchronous request has failed");
      }

      public void completed(HttpResponse<String> response) {
        logger.info("The asynchronous request has been executed successfully");
        checkResponse(response, expectedStatusCode);
        addHeaders(response);
      }

      public void cancelled() {
        logger.warn("The asynchronous request has been cancelled");
      }

    });
  }

  /**
   * Retry method depending on unwanted return code list rcToRetry (503 by default), representing mainly service unavailable or network issues
   *
   * @param request the http request to process
   * @param status the status we expecting
   * @param maxRetry to set the retry number
   * @param rcToRetry list of return code if encountered, we retry the call
   * @return the http response for the request
   * @throws UnirestException if there is an error in the request processing
   */
  private HttpResponse<String> executeCallRetry(BaseRequest request, int status, int maxRetry, List<Integer> rcToRetry ) throws UnirestException {
    HttpResponse<String> response = null;
    int i=1;
    String url = request.getHttpRequest().getUrl();
    //Calls to request.asString() is adding all Unirest default headers to the request POJO. In a retry context this does not work
    //because same headers get added over & over. Hence need to manually clear and set POJO specific headers.
    Map<String, List<String>> requestSpecificHeaders = new HashMap<>(request.getHttpRequest().getHeaders());
    String bodyMessage;
    int returnCode;
    String returnMessage="Unknown Message"; //NOI18N
    do {
      returnCode = -1;
      try {
        response = request.asString();
        returnCode = response.getStatus();
        returnMessage = response.getStatusText();
        bodyMessage = response.getBody();
        if ((status == returnCode) || !rcToRetry.contains(returnCode)) {
          break;
        }
      } catch (UnirestException e) {
        if ((e.getCause() instanceof SSLHandshakeException) && (i <= maxRetry)) {
          //Ignore error
          bodyMessage = e.getMessage();
        } else { throw e; }
      }
      //In case of retry, need to clear default headers that was added by Unirest and restore those added programmatically
      request.getHttpRequest().getHeaders().clear();
      request.getHttpRequest().getHeaders().putAll(requestSpecificHeaders);
      logger.debug("Try " + i + " is not successful, return code \"" + returnCode + "\" is different from expected \"" + status + "\"\nCall url :\n" + url); //NOI18N
      waitBeforeRetry();

    } while (i++ <= maxRetry);

    if (status != returnCode) {
      logger.debug(String.format("Unexpected status received when calling %s: %s - %s",url,returnCode, returnMessage)); //NOI18N
      logger.debug("Request Headers : " + request.getHttpRequest().getHeaders().toString()); //NOI18N
      logger.debug(String.format("Default Headers: %s", Options.getOption(Option.DEFAULT_HEADERS))); //NOI18N
      logger.debug("Response body : " + bodyMessage); //NOI18N
    }
    return response;
  }

  /**
   * waiter between each retry
   */
  private void waitBeforeRetry() {
    try {
      TimeUnit.MILLISECONDS.sleep(SLEEP_TIME_BETWEEN_CALLS_MILLISECONDS);
    } catch (InterruptedException e) {
      logger.debug("Sleep have been interrupted"); //NOI18N
      // Restore interrupted state...
      Thread.currentThread().interrupt();
    }
  }

  /**
   * the method is a wrapper to makes retry on return code list rcToRetry [503], representing mainly service unavailable or network issues
   *
   * @param method             the method
   * @param queryFields        the query fields
   * @param queryParams        the query params
   * @param body               the body
   * @param jwtToken           the jwt token
   * @param expectedStatusCode the expected status code
   * @param retryCount to set the retry number
   * @return the http response for the request
   * @throws UnirestException the exception
   * @throws IOException the exception
   */
  public HttpResponse<String> execute (
          HttpMethod method, Map<String, Object> queryFields, Map<String, String> queryParams,
          String body, String jwtToken, int expectedStatusCode, int retryCount )
          throws UnirestException, IOException {
    HttpResponse<String> response;
    if (!queryFields.isEmpty()){
      HashMap<String, String> requestHeaders = new HashMap<>();
      if (jwtToken != null){requestHeaders.put("Authorization", "Bearer " + jwtToken);} //NOI18N
      requestHeaders.put("Cookies", getCookiesForRequest()); //NOI18N
      MultipartBody request = createRequestFields(method, requestHeaders, queryFields);
      clearCookies();
      response = executeCallRetry(request, expectedStatusCode, retryCount, returnCodeToRetry);
    }else{
      HttpRequest request = createRequest(method, queryParams, body);
      if (jwtToken != null){setJwtAuthentication(request, jwtToken);}
      addCookiesToRequest(request);
      clearCookies();
      response = executeCallRetry(request, expectedStatusCode, retryCount, returnCodeToRetry);
    }

    addHeaders(response);

    saveResponseCookies(response);
    saveResponseHeaders(response);

    return response;
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param queryFields        the query fields
   * @param queryParams        the query params
   * @param body               the body
   * @param jwtToken           the jwt token
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   * @throws IOException the exception
   */
  public String execute(HttpMethod method, Map<String, Object> queryFields, Map<String, String> queryParams,
      String body, String jwtToken,
      int expectedStatusCode) throws UnirestException, IOException {

    HttpResponse<String> response = execute(method, queryFields, queryParams, body, jwtToken, expectedStatusCode, callRetryCount);
    checkResponse(response, expectedStatusCode);

    return Variables.get(RESPONSE_VARIABLE);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param queryParams        the query params
   * @param body               the body
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, Map<String, String> queryParams,
      String body, int expectedStatusCode) throws UnirestException  {

    HttpRequest request = createRequest(method, queryParams, body);

    setAuthentication(request, userName, password);

    addCookiesToRequest(request);

    clearCookies();

    clearResponseHeaders();

    HttpResponse<String> response = executeCallRetry(request, expectedStatusCode, callRetryCount, returnCodeToRetry);

    checkResponse(response, expectedStatusCode);

    addHeaders(response);

    saveResponseCookies(response);

    saveResponseHeaders(response);

    return Variables.get(RESPONSE_VARIABLE);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param queryParams        the query params
   * @param body               the body
   * @param JWTToken           the jwt token
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   * @throws IOException the exception
   */
  public String execute(HttpMethod method, Map<String, String> queryParams, String body, String JWTToken,
      int expectedStatusCode) throws IOException, UnirestException {
    return execute(method, new HashMap<String, Object>(), queryParams, body, JWTToken, expectedStatusCode);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param queryParams        the query params
   * @param body               the body
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, Map<String, String> queryParams,
      Document body, int expectedStatusCode) throws UnirestException {
    return execute(method, userName, password, queryParams, XMLOperations.documentToString(body), expectedStatusCode);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param body               the body
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, Document body, int expectedStatusCode)
      throws UnirestException {
    return execute(method, userName, password, new HashMap<String, String>(), XMLOperations.documentToString(body),
                   expectedStatusCode);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, int expectedStatusCode) throws UnirestException {
    return execute(method, userName, password, new HashMap<String, String>(), "", expectedStatusCode);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param body               the body
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, String body, int expectedStatusCode)
      throws UnirestException {
    return execute(method, userName, password, new HashMap<String, String>(), body, expectedStatusCode);
  }

  /**
   * Execute string.
   *
   * @param method             the method
   * @param userName           the user name
   * @param password           the password
   * @param queryParams        the query params
   * @param expectedStatusCode the expected status code
   * @return the string
   * @throws UnirestException the exception
   */
  public String execute(HttpMethod method, String userName, String password, Map<String, String> queryParams,
      int expectedStatusCode) throws UnirestException {
    return execute(method, userName, password, queryParams, "", expectedStatusCode);
  }

  /**
   * Sets authentication.
   *
   * @param request  the request
   * @param userName the user name
   * @param password the password
   */
  public void setAuthentication(HttpRequest request, String userName, String password) {
    if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
      request.basicAuth(userName, password);
    }
  }

  /**
   * Sets jwt authentication.
   *
   * @param request the request
   * @param token   the token
   */
  public void setJwtAuthentication(HttpRequest request, String token) {
    if (!token.isEmpty()) {
      request.header("Authorization", "Bearer " + token);
    } else {
      logger.warn("JWT token not available. Authorization header not set!"); // NOI18N
    }
  }

  private void checkResponse(HttpResponse<String> response, int expectedStatusCode) {
    Integer responseStatusCode = response.getStatus();
    String responseBody = response.getBody();

    if (responseStatusCode != expectedStatusCode) {
      throw new RuntimeException(
          "Actual response status code " + responseStatusCode + " is not equal to expected " + expectedStatusCode
              + ". Body: " + responseBody);
    }

    //Check for body existence
    if (expectedStatusCode != HttpStatus.SC_NO_CONTENT && expectedStatusCode != HttpStatus.SC_NOT_MODIFIED) {
      if (responseBody == null) {
        throw new RuntimeException("Response body is null, but it should not be");
      }
      Variables.set(RESPONSE_VARIABLE, XMLOperations.removeXmlNamespaces(responseBody));
    } else {
      if (responseBody != null) {
        throw new RuntimeException("Response body is not null, but it should be");
      }
      Variables.set(RESPONSE_VARIABLE, "");
    }
  }

  private void addHeaders(HttpResponse response) {

    // Remove all headers from map
    for (String additionalHeader : additionalHeaders) {
      if (Variables.has(additionalHeader)) {
        Variables.set(additionalHeader, "");
      }
    }

    // Add current headers to map
    Headers headers = response.getHeaders();

    for (String header : headers.keySet()) {
      if (additionalHeaders.contains(header)) {
        Variables.set(header, StringUtils.join(headers.get(header), ','));
      }
    }
  }
}
