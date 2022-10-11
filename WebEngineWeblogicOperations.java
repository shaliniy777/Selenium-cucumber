/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.webservers.WeblogicOperations;

/**
 * The type Web engine weblogic operations.
 */
public class WebEngineWeblogicOperations extends WeblogicOperations {

  /**
   * Instantiates a new Web engine weblogic operations.
   */
  public WebEngineWeblogicOperations() {
    super();
  }

  @Override
  public void start() throws Exception {
    if (isStopped()) {
      super.start();
    }
  }
}