/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.databases.PostgreDBOperations;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The type Postgres admin steps.
 */
@ConfigurationProperties(
    properties = {
        "database.adminUser",
        "database.adminUserPassword"
    }
)
public class PostgresAdminSteps {

  private PostgreDBOperations postgresDBOperations;

  /**
   * Instantiates a new Postgres admin steps.
   *
   * @throws SQLException the sql exception
   */
  public PostgresAdminSteps() throws SQLException {
    postgresDBOperations = new PostgreDBOperations();
  }

  /**
   * Create schema.
   *
   * @param schemaName the schema name
   * @throws SQLException the sql exception
   * @throws IOException  the io exception
   */
  @And("^I create postgres schema with name (.*)$")
  public void createSchema(String schemaName) throws SQLException, IOException {

    schemaName = VariablesTransformer.transformSingleValue(schemaName);

    postgresDBOperations.createDatabase(schemaName);
  }

  /**
   * Drop schema.
   *
   * @param schemaName the schema name
   * @throws SQLException the sql exception
   * @throws IOException  the io exception
   */
  @And("^I drop postgres schema with name (.*)$")
  public void dropSchema(String schemaName) throws SQLException, IOException {

    schemaName = VariablesTransformer.transformSingleValue(schemaName);

    postgresDBOperations.dropDatabase(schemaName);
  }

  /**
   * Create login.
   *
   * @param user     the user
   * @param password the password
   * @throws SQLException the sql exception
   * @throws IOException  the io exception
   */
  @And("^I create postgres database login with name (.*) and password (.*)$")
  public void createLogin(String user,
      String password) throws SQLException, IOException {

    user = VariablesTransformer.transformSingleValue(user);
    password = VariablesTransformer.transformSingleValue(password);

    postgresDBOperations.createLogin(user, password);
  }
}