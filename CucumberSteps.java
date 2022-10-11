/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.helpers.Config;
import com.experian.automation.logger.Logger;
import io.cucumber.core.api.Scenario;
import io.cucumber.java.Before;
import java.io.File;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * The type Cucumber steps.
 */
@ConfigurationProperties(
    name = "Cucumber common framework steps",
    properties = {
        "execution.tags",
        "features.path",
        "temp.dir",
        "reports.dir"
    }
)
public class CucumberSteps {

  private final WebHarness webHarness;

  private final Logger logger = Logger.getLogger(this.getClass());
  /**
   * The constant currentScenario.
   */
  public static Scenario currentScenario;

  /**
   * Instantiates a new Cucumber steps.
   *
   * @param webHarness the web harness
   */
  public CucumberSteps(WebHarness webHarness) {
    this.webHarness = webHarness;
  }

  /**
   * Default before.
   */
  @Before
  public void defaultBefore() {
    // Default before
  }

  /**
   * Before scenario.
   *
   * @param scenario the scenario
   * @throws Throwable the throwable
   */
  public void beforeScenario(Scenario scenario) throws Throwable {
    setCurrentScenario(scenario);
  }

  private static void setCurrentScenario(Scenario scenario) {
    currentScenario = scenario;
  }

  /**
   * After scenario.
   *
   * @param scenario the scenario
   * @throws Throwable the throwable
   */
  public void afterScenario(Scenario scenario) throws Throwable {
    if (!scenario.getStatus().name().equalsIgnoreCase("PASSED")) {
      if (webHarness.driver != null && ((RemoteWebDriver) webHarness.driver).getSessionId() != null) {
        try {

          try {
            //Handle unexpected alert.
            Alert alert = webHarness.driver.switchTo().alert();
            logger.error("Unhandled alert with message: " + alert.getText());
            alert.dismiss();
          } catch (NoAlertPresentException ignored) {
          }

          logger.error("Url on failure: " + webHarness.driver.getCurrentUrl());
          getScreenshot(scenario);

        } catch (WebDriverException wde) {
          logger.error(wde.getMessage());
        } catch (ClassCastException cce) {
          cce.printStackTrace();
        }
      }

      //Quit all started web drivers
      quitStartedWebDrivers();
    }
  }

  /**
   * @param scenario cucumber scenario
   */
  private void getScreenshot(Scenario scenario) {
    ArrayList<String> windowHandles = new ArrayList<>(webHarness.driver.getWindowHandles());
    String initialWindowHandle = webHarness.driver.getWindowHandle();
    for (String winHandle : windowHandles) {
      try {
        String imageFileSuffix = "window" + "-" + String.valueOf(windowHandles.indexOf(winHandle));
        String imageFilePrefix = scenario.getId().substring(scenario.getId().lastIndexOf('/') + 1).replaceAll(
            "[/:.]", "-");

        webHarness.driver.switchTo().window(winHandle);
        if (initialWindowHandle.equals(winHandle)) {
          imageFileSuffix = "window-main";
          //Switch to default content in order to get full screenshot of the main window
          webHarness.driver.switchTo().defaultContent();
        }
        File ImageFile = ((TakesScreenshot) webHarness.driver).getScreenshotAs(OutputType.FILE);

        String imageName = String.format("%s-%s.png", imageFilePrefix, imageFileSuffix);
        String imageLocation = String.format("%s/data-%s", Config.getAsUnixPath("reports.dir"), imageName);

        File reportImageFile = new File(imageLocation);
        FileUtils.copyFile(ImageFile, reportImageFile);
        logger.embedFileToReport(reportImageFile, "image/png");
      } catch (Exception e) {
        logger.error("Cannot get screenshot window handle");
      }
    }
  }

  /**
   * quits all drivers
   */
  private void quitStartedWebDrivers() {
    for (WebDriver driver : webHarness.driversMap.values()) {
      driver.quit();
    }
    webHarness.driversMap.clear();
  }
}