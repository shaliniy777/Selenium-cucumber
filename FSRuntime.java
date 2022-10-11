/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import java.io.IOException;

/**
 * The type Fs runtime.
 */
public class FSRuntime extends FSOperations {

  /**
   * Instantiates a new Fs runtime.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public FSRuntime() throws IOException, ConfigurationException {
    Config config = new Config();

    srcHost = config.get("runtime.host");
    dstHost = config.get("design.host");
  }

}
