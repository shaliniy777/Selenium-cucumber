/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation;

import com.experian.automation.cucumber.CustomTestNGCucumberRunner;
import com.experian.automation.cucumber.CustomTestNGCucumberRunnerFactory;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.Variables;
import com.experian.automation.runner.BackgroundGenerator;
import io.cucumber.testng.CucumberFeatureWrapper;
import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.PickleEventWrapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * The type Run test.
 */
@CucumberOptions(
    plugin = {"pretty", "com.experian.automation.cucumber.configuration.ConfigurationCheckFormatter", //NOI18N
        "com.experian.automation.cucumber.logger.CustomCucumberLogger"} //NOI18N
)
public class RunTest {

  private CustomTestNGCucumberRunner testNGCucumberRunner;

  /**
   * Sets up class.
   *
   * @param scenario the scenario
   * @throws Throwable the throwable
   */
  @BeforeClass(
      alwaysRun = true
  )
  @Parameters({"scenario"})
  public void setUpClass(@Optional("") String scenario) throws Throwable {

    synchronized (CustomTestNGCucumberRunnerFactory.class) {

      CustomTestNGCucumberRunnerFactory customTestNGCucumberRunnerFactory = new CustomTestNGCucumberRunnerFactory(
          RunTest.class);

      if (scenario.isEmpty()) {
        BackgroundGenerator.generate();
        customTestNGCucumberRunnerFactory.addCucumberOption("tags", Config.get("execution.tags").split(",\\s+"));  //NOI18N
      } else {
        // Reset cucumber.options
        System.setProperty("cucumber.options", ""); //NOI18N

        customTestNGCucumberRunnerFactory.setCucumberOption("features", scenario); //NOI18N
        customTestNGCucumberRunnerFactory.
            addCucumberOption("plugin", "json:target/cucumber" + scenario.replaceAll("[:|/]", "-") + ".json"); //NOI18N
        customTestNGCucumberRunnerFactory.
            addCucumberOption("plugin", "junit:target/cucumber" + scenario.replaceAll("[:|/]", "-") //NOI18N
                + ".cucumber_junit.xml"); //NOI18N
      }

      this.testNGCucumberRunner = customTestNGCucumberRunnerFactory.create();
    }

  }

  /**
   * Run scenario.
   *
   * @param pickleWrapper  the pickle wrapper
   * @param featureWrapper the feature wrapper
   * @throws Throwable the throwable
   */
  @Test(
      groups = {"cucumber"}, //NOI18N
      description = "Runs Cucumber Scenarios", //NOI18N
      dataProvider = "scenarios" //NOI18N
  )
  public void runScenario(PickleEventWrapper pickleWrapper, CucumberFeatureWrapper featureWrapper) throws Throwable {
    Variables.clearAll();
    this.testNGCucumberRunner.runScenario(pickleWrapper.getPickleEvent());
  }

  /**
   * Scenarios object [ ] [ ].
   *
   * @return the object [ ] [ ]
   */
  @DataProvider
  public Object[][] scenarios() {
    return this.testNGCucumberRunner.provideScenarios();
  }

  /**
   * Tear down class.
   *
   * @throws Exception the exception
   */
  @AfterClass(
      alwaysRun = true
  )
  public void tearDownClass() throws Exception {
    this.testNGCucumberRunner.finish();
  }

}