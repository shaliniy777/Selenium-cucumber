/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.ServiceOperations;
import com.experian.automation.helpers.webservers.WebSphereOperations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * The type Web engine factory.
 */
public class WebEngineFactory {

  private ServiceOperations webengineOperations;

  /**
   * Instantiates a new Web engine factory.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public WebEngineFactory() throws IOException, ConfigurationException {
    String container = Config.get("webengine.container");
    switch (container) {
      case "tomcat":
        webengineOperations = new WebEngineTomcatOperations();
        break;
      case "websphere":
        List<String> webApps = new ArrayList<String>();
        webApps.add(Config.get("webapp.webengine"));
        webengineOperations = new WebSphereOperations(webApps);
        break;
      case "weblogic":
        webengineOperations = new WebEngineWeblogicOperations();
        break;
      default:
        webengineOperations = new WebEngineTomcatOperations();
    }
  }

  /**
   * Factory service operations.
   *
   * @return the service operations
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public ServiceOperations factory() throws IOException, ConfigurationException {
    return webengineOperations;
  }
}