/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.ServiceOperations;
import com.experian.automation.helpers.mock.OpenAMMockOperations;
import com.experian.automation.helpers.webservers.WebSphereOperations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * The type Open am factory.
 */
public class OpenAMFactory {

  /**
   * The Config.
   */
  public Config config;
  private ServiceOperations openAMOperations;
  private String container;

  /**
   * Instantiates a new Open am factory.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public OpenAMFactory() throws IOException, ConfigurationException {
    container = Config.get("openam.container");
    List<String> ssoWebapp = new ArrayList<String>();
    ssoWebapp.add(Config.get("webapp.openam"));

    switch (container) {
      case "websphere":
        openAMOperations = new WebSphereOperations(ssoWebapp);
        break;
      case "mockAM":
        openAMOperations = new OpenAMMockOperations();
        break;
      case "weblogic":
        openAMOperations = new OpenAMWeblogicOperations();
        break;
      default:
        openAMOperations = new OpenAMTomcatOperations();
    }
  }

  /**
   * Factory service operations.
   *
   * @return the service operations
   */
  public ServiceOperations factory() {
    return openAMOperations;
  }

}
