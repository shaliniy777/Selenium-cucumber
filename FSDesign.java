/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import java.io.IOException;

/**
 * The type Fs design.
 */
public class FSDesign extends FSOperations {

  /**
   * Instantiates a new Fs design.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public FSDesign() throws IOException, ConfigurationException {
    Config config = new Config();

    srcHost = config.get("design.host");
    dstHost = config.get("runtime.host");
  }

}
