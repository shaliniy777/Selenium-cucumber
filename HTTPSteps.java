/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.HTTPOperations;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;
import java.io.IOException;


/**
 * The type Http steps.
 */
public class HTTPSteps {

  private HTTPOperations httpOperations;

  /**
   * Instantiates a new Http steps.
   */
  public HTTPSteps() {
    httpOperations = new HTTPOperations();
  }

  /**
   * Download file.
   *
   * @param url      the url
   * @param filePath the file path
   * @throws Throwable the throwable
   */
  /*
   *  And I download file from http://artifactory/file.txt to C:\newFile.txt
   */
  @And("^I download file from (.*) to (.*)$")
  public void downloadFile(String url, String filePath) throws Throwable {

    url = VariablesTransformer.transformSingleValue(url);
    filePath = VariablesTransformer.transformSingleValue(filePath);

    httpOperations.downloadFile(url, filePath);
  }

  /**
   * Download file with authentication
   *
   * @param username     the username
   * @param password     the password
   * @param url          the url
   * @param filePath     the file path
   * @throws IOException the exception
   */
  /*
   *  I download file using username John and password pas$word from http://localhost:120/api/v1/entity/account/10/files/21c89e2e-fe0e-4e36-90a5-8779c8758a2c to ${temp.dir}/validPDFFile.pdf
   */
  @And("^I download file using username (.*) and password (.*) from (.*) to (.*)$")
  public void downloadFile(String username, String password, String url, String filePath) throws IOException {

    url = VariablesTransformer.transformSingleValue(url);
    filePath = VariablesTransformer.transformSingleValue(filePath);
    username = VariablesTransformer.transformSingleValue(username);
    password = VariablesTransformer.transformSingleValue(password);

    httpOperations.downloadFileWithAuthentication(url, filePath, username, password);
  }

  /**
   * Upload file.
   *
   * @param filePath the file path
   * @param url      the url
   * @param username the username
   * @param password the password
   * @throws Throwable the throwable
   */
  /*
   *  And I upload file from ${fileLocation} to ${artifactoryLocation} using username user and password pass
   */
  @And("^I upload file from (.*) to (.*) using username (.*) and password (.*)$")
  public void uploadFile(String filePath, String url, String username, String password) throws Throwable {

    filePath = VariablesTransformer.transformSingleValue(filePath);
    url = VariablesTransformer.transformSingleValue(url);
    username = VariablesTransformer.transformSingleValue(username);
    password = VariablesTransformer.transformSingleValue(password);

    httpOperations.uploadFile(url, filePath, username, password);
  }
}
