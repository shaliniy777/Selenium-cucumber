/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import com.mashape.unirest.http.utils.Base64Coder;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * The type Http operations.
 */
public class HTTPOperations {

  /**
   * The Url.
   */
  protected String url;

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Http operations.
   */
  public HTTPOperations() {
    //Blank Constructor
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Downloads a file from a url
   * <p>
   * Example: (http://artifactory-gsg-eu-west-1.experian.local/export.xml, C:/Temp/Lib/export.xml)
   *
   * @param url      - URL to the file to be downloaded
   * @param filePath - System file path where the file will be downloaded
   * @throws Exception the exception
   */
  public void downloadFile(String url, String filePath) throws Exception {

    URL downloadURL = new URL(url);
    File file = new File(filePath);

    int timeOutMilliseconds = 10 * 60 * 1000;
    FileUtils.copyURLToFile(downloadURL, file, timeOutMilliseconds, timeOutMilliseconds);
  }

  /**
   * Downloads a file from a url using basic authentication
   * <p>
   *
   * @param url          URL to the file to be downloaded
   * @param filePath     System file path where the file will be downloaded
   * @param username     username of the privileged user
   * @param password     password of the privileged password
   * @throws IOException the exception
   */
  public void downloadFileWithAuthentication(String url, String filePath, String username, String password)
      throws IOException {

    URL downloadURL = new URL(url);
    File file = new File(filePath);

    int timeOutMilliseconds = 10 * 60 * 1000;

    String authString = String.format("%s:%s", username, password); //NOI18N
    String authStringBase64 = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

    URLConnection connection = downloadURL.openConnection();
    connection.setRequestProperty("Authorization", "Basic " + authStringBase64); //NOI18N
    connection.setConnectTimeout(timeOutMilliseconds);
    connection.setReadTimeout(timeOutMilliseconds);

    FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
  }

  /**
   * Uploads a file to a url
   * <p>
   * Example: (PUT, http://artifactory-gsg-eu-west-1.experian.local/export.xml,C:/export.xml,user1,password1)
   *
   * @param method             - HTTP method to be used for uploading the file (POST|PUT)
   * @param url                - URL to the location where file will be uploaded
   * @param filePath           - Path to the file being uploaded
   * @param username           - Username used for basic authorization
   * @param password           - Password used for basic authorization
   * @param expectedStatusCode - The expected status code
   * @throws Exception the exception
   */
  public void uploadFile(String method, String url, String filePath, String username, String password,
      int expectedStatusCode) throws Exception {

    File file = new File(filePath);
    HttpEntity entity = new FileEntity(file);

    setUrl(url);
    HttpRequestBase request = createRequest(method, entity);
    setAuthentication(request, username, password);
    int responseStatusCode = executeRequest(request);

    if (responseStatusCode != expectedStatusCode) {
      logger.error("Upload of file: " + file.getName() + " to " + url + " failed");
    }
  }

  /**
   * Uploads a file using HTTP PUT request to a url expecting HTTP status 201(Created)
   * <p>
   * Example: (http://artifactory-gsg-eu-west-1.experian.local/export.xml,C:/export.xml,user1,password1)
   *
   * @param url      - URL to the location where file will be uploaded
   * @param filePath - Path to the file being uploaded
   * @param username - Username used for basic authorization
   * @param password - Password used for basic authorization
   * @throws Exception the exception
   */
  public void uploadFile(String url, String filePath, String username, String password)
      throws Exception {
    uploadFile("PUT", url, filePath, username, password, HttpStatus.SC_CREATED);
  }

  /**
   * Probes a URL
   * <p>
   * Example: (http://artifactory-gsg-eu-west-1.experian.local/export.xml)
   *
   * @param url                - URL string to be probed
   * @param expectedStatusCode - The expected status code
   * @return the boolean
   * @throws Exception the exception
   */
  public boolean probe(String url, int expectedStatusCode) throws Exception {
    setUrl(url);
    HttpRequestBase request = createRequest("HEAD", null);
    int responseStatusCode = executeRequest(request);
    return responseStatusCode == expectedStatusCode;
  }

  private HttpRequestBase createRequest(String method, HttpEntity entity) {

    HttpRequest httpRequest;
    switch (method) {
      case "PUT":
        httpRequest = new HttpPut(this.getUrl());
        ((HttpPut) httpRequest).setEntity(entity);
        break;
      case "GET":
        httpRequest = new HttpGet(this.getUrl());
        break;
      case "POST":
        httpRequest = new HttpPost(this.getUrl());
        ((HttpPost) httpRequest).setEntity(entity);
        break;
      case "HEAD":
        httpRequest = new HttpHead(this.getUrl());
        break;
      case "PATCH":
        httpRequest = new HttpPatch(this.getUrl());
        ((HttpPatch) httpRequest).setEntity(entity);
        break;
      case "DELETE":
        httpRequest = new HttpDelete(this.getUrl());
        break;
      default:
        throw new RuntimeException("Unsupported HTTP method: " + method);
    }
    return (HttpRequestBase) httpRequest;
  }

  private void setAuthentication(HttpRequestBase request, String userName, String password) {
    request.addHeader("Authorization", "Basic " + Base64Coder.encodeString(userName + ":" + password));
  }

  private int executeRequest(HttpRequestBase request) {
    int statusCode;
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      try (CloseableHttpResponse response = client.execute(request)) {
        statusCode = response.getStatusLine().getStatusCode();
      }
    } catch (IOException e) {
      logger.error("Unable to close http client" + e.getMessage());
      statusCode = -1;
    }
    return statusCode;
  }
}
