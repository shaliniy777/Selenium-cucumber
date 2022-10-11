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
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.MultipartBody;
import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.testng.TestException;

/**
 * The type Api operations.
 */
public abstract class APIOperations {

  /**
   * The constant RESPONSE_COOKIE.
   */
  public final static String RESPONSE_COOKIE = "ResponseCookie";
  /**
   * The constant REQUEST_COOKIE.
   */
  public final static String REQUEST_COOKIE = "RequestCookie";
  /**
   * The constant RESPONSE_HEADERS.
   */
  public final static String RESPONSE_HEADERS = "ResponseHeaders";

  private final Logger logger = Logger.getLogger(this.getClass());

  //For testing purposes set  proxyHostName and proxyPort in order to sniff http requests
  private final String proxyHostName = "";
  private final Integer proxyPort = -1;

  private HttpClientBuilder httpClientBuilder;
  /**
   * The Url.
   */
  protected String url;

  /**
   * Instantiates a new Api operations.
   */
  public APIOperations() {

    initClient();
  }

  /**
   * Add url.
   *
   * @param url the url
   */
  public void addURL(String url) {
    this.url = normaliseUrl(url);
  }

  private void initClient() {
    if (StringUtils.isNotEmpty(proxyHostName) && proxyPort > 0) {
      Unirest.setProxy(new HttpHost(proxyHostName, proxyPort));
    }
    Unirest.clearDefaultHeaders();

    try {
      httpClientBuilder = HttpClients.custom()
          .setSSLContext(new SSLContextBuilder()
          .loadTrustMaterial(null, (x509Certificates, s) -> true).build())
          .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);

      Unirest.setHttpClient(httpClientBuilder.build());
    } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
      java.util.logging.Logger.getLogger(APIOperations.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void initClient(int connectionTimeout, int socketTimeout) {
    initClient();

    httpClientBuilder = httpClientBuilder.setDefaultRequestConfig(
        RequestConfig.custom().setConnectTimeout(connectionTimeout).setSocketTimeout(
            socketTimeout).build());

    Unirest.setHttpClient(httpClientBuilder.build());
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return normaliseUrl(url);
  }

  /**
   * Add new cookie to request.
   *
   * @param cookies the cookies
   */
  public void addNewCookieToRequest(Map<String, String> cookies) {
    cookies = VariablesTransformer.transformMap(cookies);

    for (String cookie : cookies.keySet()) {
      logger.info(String.format("Added cookie to request - %s = %s",cookie, cookies.get(cookie)));
      Variables.set(REQUEST_COOKIE + "." + cookie, cookies.get(cookie));
    }
  }

  /**
   * Add existing cookie to request.
   *
   * @param cookies the cookies
   */
  public void addExistingCookieToRequest(List<String> cookies) {
    for (String cookie : cookies) {
      logger.info(String.format("Added cookie to request - %s = %s", cookie, Variables.get(cookie)));
      Variables.set(REQUEST_COOKIE + "." + cookie, Variables.get(cookie));
    }
  }

  /**
   * Disable unirest cookie state management.
   */
  public void disableUnirestCookieStateManagement() {
    httpClientBuilder = httpClientBuilder.disableCookieManagement();
    Unirest.setHttpClient(httpClientBuilder.build());
  }

  /**
   * Add cookies to request.
   *
   * @param request the request
   */
  public void addCookiesToRequest(HttpRequest request) {

    List<String> cookiesNames = Variables.getAll().stringPropertyNames().stream()
        .filter((name) -> name.startsWith(APIOperations.REQUEST_COOKIE + "."))
        .collect(Collectors.toList());
    StringBuilder allCookies = new StringBuilder();
    for (String cookie : cookiesNames) {
      String cookieName = cookie.substring(APIOperations.REQUEST_COOKIE.length() + 1, cookie.length());
      String cookieValue = Variables.get(cookie);
      String appendText = String.format("%s=%s; ", cookieName, cookieValue);
      allCookies.append(appendText);
    }

    // If the cookie is empty we should not add it to the request headers
    if(StringUtils.isNotEmpty(allCookies.toString())) {
      request.header("Cookie", allCookies.toString());
    }
  }

  /**
   * Gets cookies for request.
   *
   * @return the cookies for request
   */
  public String getCookiesForRequest() {

    List<String> cookiesNames = Variables.getAll().stringPropertyNames().stream()
        .filter((name) -> name.startsWith(APIOperations.REQUEST_COOKIE + "."))
        .collect(Collectors.toList());
    StringBuilder allCookies = new StringBuilder();
    for (String cookie : cookiesNames) {
      String cookieName = cookie.substring(APIOperations.REQUEST_COOKIE.length() + 1, cookie.length());
      String cookieValue = Variables.get(cookie);
      String appendText = String.format("%s=%s; ", cookieName, cookieValue);
      allCookies.append(appendText);
    }
    return allCookies.toString();
  }

  /**
   * Save response cookies.
   *
   * @param response the response
   */
  public void saveResponseCookies(HttpResponse response) {
    Headers headers = response.getHeaders();

    List<String> cookieHeaders = headers.get("Set-Cookie");
    if (cookieHeaders != null) {
      for (String header : cookieHeaders) {
        List<HttpCookie> cookies = HttpCookie.parse(header);
        for (HttpCookie cookie : cookies) {
          Variables.set(RESPONSE_COOKIE + "." + cookie.getName(), cookie.getValue());
        }
      }
    }
  }

  /**
   * Clear cookies.
   */
  public void clearCookies() {
    List<String> cookies = Variables.getAll().stringPropertyNames().stream()
        .filter((name) -> name.startsWith(APIOperations.RESPONSE_COOKIE + ".") || name.startsWith(
            APIOperations.REQUEST_COOKIE + "."))
        .collect(Collectors.toList());
    for (String cookieName : cookies) {
      Variables.getAll().remove(cookieName);
    }
  }

  /**
   * Add headers to request.
   *
   * @param headers the headers
   */
  public void addHeadersToRequest(Map<String, String> headers) {
    headers = VariablesTransformer.transformMap(headers);
    Unirest.clearDefaultHeaders();

    for (String header : headers.keySet()) {
      Unirest.setDefaultHeader(header, headers.get(header));
    }
  }

  /**
   * Gets response cookie.
   *
   * @param cookieName the cookie name
   * @return the response cookie
   */
  public static String getResponseCookie(String cookieName) {
    return Variables.get(APIOperations.RESPONSE_COOKIE + "." + cookieName);
  }

  /**
   * Save response headers.
   *
   * @param response the response
   */
  public void saveResponseHeaders(HttpResponse response) {
    Headers headers = response.getHeaders();

    for (String header : headers.keySet()) {
      Variables.set(RESPONSE_HEADERS + "." + header, StringUtils.join(headers.get(header), ','));
    }
  }

  /**
   * Clear response headers.
   */
  public void clearResponseHeaders() {
    List<String> headers = Variables.getAll().stringPropertyNames().stream()
        .filter((name) -> name.startsWith(APIOperations.RESPONSE_HEADERS + "."))
        .collect(Collectors.toList());
    for (String header : headers) {
      Variables.getAll().remove(header);
    }
  }

  /**
   * Create request http request.
   *
   * @param method      the method
   * @param queryParams the query params
   * @param body        the body
   * @return the http request
   */
  public HttpRequest createRequest(HttpMethod method, Map<String, String> queryParams,
      String body) {

    HttpRequest request;

    switch (method) {
      case GET:
        request = Unirest.get(getUrl());
        break;
      case POST:
        request = Unirest.post(getUrl());
        ((HttpRequestWithBody) request).body(body);
        break;
      case PUT:
        request = Unirest.put(getUrl());
        ((HttpRequestWithBody) request).body(body);
        break;
      case PATCH:
        request = Unirest.patch(getUrl());
        ((HttpRequestWithBody) request).body(body);
        break;
      case DELETE:
        request = Unirest.delete(getUrl());
        break;
      case OPTIONS:
        request = Unirest.options(getUrl());
        break;
      default:
        throw new RuntimeException("Unsupported REST HTTP method: " + method);

    }
    request.queryString(Collections.<String, Object>unmodifiableMap(queryParams));

    return request;
  }

  /**
   * Create request fields multipart body.
   *
   * @param method      the method
   * @param headers     the headers
   * @param queryFields the query fields
   * @return the multipart body
   * @throws UnirestException when an error during the unirest call happen
   * @throws IOException the io exception
   */
  public MultipartBody createRequestFields(HttpMethod method, Map<String, String> headers,
      Map<String, Object> queryFields) throws IOException, UnirestException {

    MultipartBody request;
    switch (method) {
      case POST:
        request = Unirest.post(getUrl()).headers(headers).fields(null);
        break;
      case PUT:
        request = Unirest.put(getUrl()).headers(headers).fields(null);
        break;
      case PATCH:
        request = Unirest.patch(getUrl()).headers(headers).fields(null);
        break;
      default:
        throw new UnirestException("Unsupported REST HTTP method: " + method); //NOI18N
    }

    for (Map.Entry<String, Object> entry : queryFields.entrySet()) {
      JSONOperations jsonOperations = new JSONOperations();
      if (jsonOperations.isJSON(entry.getValue().toString())) {
        request = request.field(entry.getKey(), entry.getValue().toString(), "application/json"); //NOI18N
      } else if (entry.getValue() instanceof File) {
        File file = (File)entry.getValue();
        request = request.field(entry.getKey(), file);
      } else {
        request = request.field(entry.getKey(), entry.getValue(), false);
      }
    }

    Variables.set("Request", request.toString()); //NOI18N
    return request;
  }

  /**
   * Sets unirest timeouts.
   *
   * @param connectionTimeout the connection timeout
   * @param socketTimeout     the socket timeout
   */
  public void setUnirestTimeouts(long connectionTimeout, long socketTimeout) {
    initClient((int) connectionTimeout, (int) socketTimeout);
  }

  /**
   * Normalise URL
   *
   * @param originalUrl Url to be normalised.
   * @return normalised url
   */
  private String normaliseUrl(String originalUrl) {

    try {
      URI uri = new URI(originalUrl);
      return uri.normalize().toString();
    } catch (URISyntaxException e) {
      throw new TestException(e);
    }
  }
}
