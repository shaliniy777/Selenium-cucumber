/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.helpers.mock.MockVariables;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;
import java.util.Map;

/**
 * The type Mock steps.
 */
public class MockSteps {

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Mock steps.
   */
  public MockSteps(){
    // Blank Constructor
  }

  /**
   * Configure mock.
   *
   * @param mockName   the mock name
   * @param properties the properties
   */
  @And("^I configure mock service with name (.*) and properties:$")
  public void configureMock(String mockName, Map<String, String> properties){
    properties = VariablesTransformer.transformMap(properties);

    for (String property : properties.keySet()) {
      logger.info(String.format("Added property - %s = %s", property, properties.get(property)));
      MockVariables.set(mockName, property, properties.get(property));
    }
  }

}
