/*
 * Copyright (c) Experian, 2021. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.ProxyOperations;
import com.experian.automation.helpers.saas.TokenUtils;
import io.cucumber.java.en.And;

/**
 * Steps related to the ProxyOperations class
 */
public class ProxySteps {

  /**
   * Starts the proxy server
   */
  @And("^I start the proxy server$")
  public void startProxyServer() {
    ProxyOperations.startProxyServer();
  }

  /**
   * Stops the proxy server
   */
  @And("^I stop the proxy server$")
  public void stopProxyServer() {
    ProxyOperations.stopProxyServer();
  }

  /**
   * Adds header with Authorization name and token as value
   * Uses the users set in config.properties file:
   * tactical.parameters.api.user and tactical.parameters.api.password
   */
  @And("^I add authorization token into proxy header$")
  public void addAuthorizationHeader() {
    ProxyOperations.addProxyHeader("Authorization", TokenUtils.getAuthorizationBearerToken()); // NOI18N
  }

  /**
   * Removes the Authorization header
   */
  @And("^I remove access token from header$")
  public void removeAuthorizationHeader() {
    ProxyOperations.removeHeader("Authorization");  // NOI18N
  }

  /**
   * Removes all headers added to the proxy server
   */
  @And("^I remove all headers in proxy server$")
  public void removeHeaders() {
    ProxyOperations.removeAllHeaders();
  }
}