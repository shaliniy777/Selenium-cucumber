/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.NetworkOperations;
import com.experian.automation.helpers.Variables;
import io.cucumber.java.en.And;
import java.net.UnknownHostException;

/**
 * The type Network steps.
 */
public class NetworkSteps {

  /**
   * Instantiates a new Network steps.
   */
  public NetworkSteps() {
    // Blank Constructor
  }

  /**
   * Gets local machine ip.
   *
   * @param varName the var name
   * @throws UnknownHostException the unknown host exception
   */
  @And("^I get local machine IP address and save it to variable (.*)$")
  public void getLocalMachineIP(String varName) throws UnknownHostException {
    NetworkOperations networkOperations = new NetworkOperations();
    Variables.set(varName, networkOperations.getLocalMachineIP());
  }


  /**
   * Get FQDN of local machine and save as variable.
   *
   * @param varName variable name
   * @throws UnknownHostException from getCanonicalHostName()
   */
  @And("^I save local machine FQDN to variable (.*)$")
  public void saveFqdnAsVariable(String varName) throws UnknownHostException {
    NetworkOperations networkOperations = new NetworkOperations();
    Variables.set(varName, networkOperations.getCanonicalHostName());
  }

}
