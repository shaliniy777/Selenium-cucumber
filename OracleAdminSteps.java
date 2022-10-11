/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.databases.OracleDBOperations;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;

/**
 * The type Oracle admin steps.
 */
@ConfigurationProperties(
    properties = {
        "database.adminUser",
        "database.adminUserPassword"
    }
)
public class OracleAdminSteps {

  private OracleDBOperations oracleDBOperations;

  /**
   * Instantiates a new Oracle admin steps.
   *
   * @throws Exception the exception
   */
  public OracleAdminSteps() throws Exception {
    oracleDBOperations = new OracleDBOperations();
  }

  /**
   * Create schema.
   *
   * @param schemaName the schema name
   * @param password the password
   * @throws Throwable the throwable
   */
  @And("^I create oracle schema with name (.*) and password (.*)$")
  public void createSchema(String schemaName, String password) throws Throwable {

    schemaName = VariablesTransformer.transformSingleValue(schemaName);

    oracleDBOperations.createSchema(schemaName, password);
  }

  /**
   * Drop schema.
   *
   * @param schemaName the schema name
   * @throws Throwable the throwable
   */
  @And("^I drop oracle schema with name (.*)$")
  public void dropSchema(String schemaName) throws Throwable {
    schemaName = VariablesTransformer.transformSingleValue(schemaName);
    oracleDBOperations.dropSchema(schemaName);
  }
}