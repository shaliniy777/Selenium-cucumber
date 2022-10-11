/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.databases.MSSQLDBOperations;
import com.experian.automation.transformers.VariablesTransformer;
import io.cucumber.java.en.And;

/**
 * The type Mssql admin steps.
 */
@ConfigurationProperties(
    properties = {
        "database.adminUser",
        "database.adminUserPassword"
    }
)
public class MSSQLAdminSteps {

  private MSSQLDBOperations mssqlDBOperations;

  /**
   * Instantiates a new Mssql admin steps.
   *
   * @throws Exception the exception
   */
  public MSSQLAdminSteps() throws Exception {
    mssqlDBOperations = new MSSQLDBOperations();
  }

  /**
   * Create database.
   *
   * @param databaseName the database name
   * @throws Throwable the throwable
   */
  @And("^I create mssql database with name (.*)$")
  public void createDatabase(String databaseName)
      throws Throwable {

    databaseName = VariablesTransformer.transformSingleValue(databaseName);

    mssqlDBOperations.createDatabase(databaseName);
  }

  /**
   * Drop database.
   *
   * @param databaseName the database name
   * @throws Throwable the throwable
   */
  @And("^I drop mssql database with name (.*)$")
  public void dropDatabase(String databaseName) throws Throwable {

    databaseName = VariablesTransformer.transformSingleValue(databaseName);

    mssqlDBOperations.dropDatabase(databaseName);
  }

  /**
   * Create login.
   *
   * @param user     the user
   * @param password the password
   * @throws Throwable the throwable
   */
  @And("^I create mssql database login with name (.*) and password (.*)$")
  public void createLogin(String user,
      String password) throws Throwable {

    user = VariablesTransformer.transformSingleValue(user);
    password = VariablesTransformer.transformSingleValue(password);

    mssqlDBOperations.createLogin(user, password);
  }

  /**
   * Create user.
   *
   * @param username   the username
   * @param schemaName the schema name
   * @param login      the login
   * @throws Throwable the throwable
   */
  @And("^I create mssql database user with username (.*) for database (.*) and login (.*)$")
  public void createUser(String username,
      String schemaName,
      String login) throws Throwable {

    username = VariablesTransformer.transformSingleValue(username);
    schemaName = VariablesTransformer.transformSingleValue(schemaName);
    login = VariablesTransformer.transformSingleValue(login);

    mssqlDBOperations.createUserForLogin(schemaName, login, username);
  }
}