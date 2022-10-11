/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.webservers.WeblogicOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The type OpenAM weblogic operations.
 */
public class OpenAMWeblogicOperations extends WeblogicOperations {

  /**
   * Instantiates a new OpenAM weblogic operations.
   */
  public OpenAMWeblogicOperations() {
    super();
  }

  @Override
  public List<Integer> getServicePorts() {
      List<Integer> openAMWeblogicPorts = new ArrayList<>();
      openAMWeblogicPorts.add(Integer.valueOf(Objects.requireNonNull(Config.get("openam.http.port"))));
      return openAMWeblogicPorts;
  }
}