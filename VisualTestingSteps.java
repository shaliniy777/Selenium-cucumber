/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import com.experian.automation.harnesses.WebHarness;
import com.experian.automation.harnesses.VisualTestingHarness;
import io.cucumber.java.en.And;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.testng.TestException;
import com.experian.automation.logger.Logger;

/**
 * The type Visual testing steps.
 */
public class VisualTestingSteps {

  private final WebHarness webHarness;
  private final VisualTestingHarness visualTestingHarness;
  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Visual testing steps.
   *
   * @param webHarness           the web harness
   * @param visualTestingHarness the visual testing harness
   * @throws ConfigurationException the configuration exception
   */
  public VisualTestingSteps(WebHarness webHarness, VisualTestingHarness visualTestingHarness)
      throws ConfigurationException {
    this.webHarness = webHarness;
    this.visualTestingHarness = visualTestingHarness;
  }

  /**
   * Change the match level for the current runner. One of EXACT, STRICT, CONTENT, LAYOUT, LAYOUT2 or NONE
   * @param matchLevel the match level
   * <pre>Examples
   * And I change visual testing match level to NONE<br>
   * And I change visual testing match level to LAYOUT<br>
   * And I change visual testing match level to LAYOUT2<br>
   * And I change visual testing match level to CONTENT<br>
   * And I change visual testing match level to STRICT<br>
   * And I change visual testing match level to EXACT</pre>
   */
  @And("^I change visual testing match level to (EXACT|STRICT|CONTENT|LAYOUT|LAYOUT2|NONE)$")
  public void changeMatchLevel(String matchLevel) {
    visualTestingHarness.setMatchLevel(matchLevel);
  }

  /**
   *  Set wait time before screenshots
   *  This step is meant to add wait time before screenshots are taken, so that all web components can appear
   *  on the screen
   *  Recommended usage is right before the step when you need additional wait time and when you do not needed it anymore
   *  to reset the wait time. Reset of the the wait can be done by setting 0 for wait time.
   *  Example:
       Given I start the browser
       And I change visual testing match level to EXACT
       And I start visual testing
       And I set 4500 milliseconds wait before screenshot
       And I go to login page
       And I verify viewport Login Page against baseline
       And I set 0 milliseconds wait before screenshot
   *
   * @param waitTime  the time to wait in milliseconds before taking a screenshots
   */
  @And("^I set (\\d+) milliseconds wait before screenshot$")
  public void setWaitBefore(int waitTime) { visualTestingHarness.setWaitTime(waitTime);  }

  /**
   * Start visual testing with Classic Runner.
   *
   * @param scenarioName the scenario name
   */
  @And("^I start visual testing(?: for scenario (.*))?$")
  public void startVisualTesting(String scenarioName) {
    visualTestingHarness.setupEyeObject(Boolean.FALSE);
    visualTestingHarness.start(webHarness, StringUtils.isEmpty(scenarioName)? CucumberSteps.currentScenario.getName():scenarioName, new ArrayList<>());
  }

  /**
   * Start visual testing with UltraFastGrid runner.
   *
   * @param scenarioName   the scenario name
   * @param data           List of List with strings from the data table configuration
   * @param concurrentRuns the number of concurrent runs for crossbrowser testing
   * <p>The data section represents the following table:</p>
   * <table>
   *   <tr>
   *     <th>Type of device - browser or device</th>
   *     <th>Browser version or device model</th>
   *     <th>Browser width or device orientation</th>
   *     <th>Browser height or nothing if device selected</th>
   *   </tr>
   * </table>
   * @see <a href="https://applitools.com/docs/api/eyes-sdk/enums-gen/enum-global-browsertype-selenium-java.html">Supported brosers</a>
   * @see <a href="https://applitools.com/docs/api/eyes-sdk/enums-gen/enum-global-devicename-selenium-java.html">Supported devices</a>
   * @see <a href="https://applitools.com/docs/api/eyes-sdk/enums-gen/enum-global-screenorientation-selenium-java.html">Types of device orientation</a>
   * <pre>Example:<br>
   * And I start cross browser visual testing with 20 concurrent runs:<br>
   * And I start cross browser visual testing with 20 concurrent runs for scenario UltraFastGrid Runner:<br>
   *       | browser | CHROME_TWO_VERSIONS_BACK  | 800        | 600 |<br>
   *       | browser | CHROME                    | 1200       | 600 |<br>
   *       | browser | CHROME                    | 1400       | 600 |<br>
   *       | browser | FIREFOX_ONE_VERSION_BACK  | 800        | 600 |<br>
   *       | browser | FIREFOX                   | 1000       | 600 |<br>
   *       | browser | FIREFOX                   | 1200       | 600 |<br>
   *       | browser | FIREFOX                   | 1400       | 600 |<br>
   *       | browser | EDGE_CHROMIUM             | 1400       | 600 |<br>
   *       | device  | Pixel_2                   | PORTRAIT   |     |<br>
   *       | device  | iPad_Pro                  | LANDSCAPE  |     |
   * </pre>
   */
  @And("^I start cross browser visual testing with (\\d+) concurrent runs(?: for scenario (.*))?:$")
  public void startCrossBrowserTesting(int concurrentRuns, String scenarioName, List<List<String>> data) {
    visualTestingHarness.setUltraFastGridConcurrentRuns((concurrentRuns>0)? concurrentRuns:1);
    visualTestingHarness.setupEyeObject(Boolean.TRUE);
    visualTestingHarness.start(webHarness, StringUtils.isEmpty(scenarioName)? CucumberSteps.currentScenario.getName():scenarioName, data);
  }

  /**
   * And I verify viewport Decision Page - Main Form against baseline And I verify viewport Decision Page - Co-applicant
   * container against baseline
   *
   * @param viewPort the view port
   */
  @And("^I verify viewport (.*) against baseline$")
  public void checkViewPort(String viewPort) {
    visualTestingHarness.checkViewPort(viewPort);
  }

  /**
   * Locating an element on the page and performing visual comparison against the baseline
   *
   * <pre>Examples
   * And I verify element with id authorized-parties-widget on viewport Authorized Parties against baseline<br>
   * And I verify element with xpath //*[@id="activities-list"]/section/collections-menu-item-group/div/collections-menu-item[10] on viewport Generic Activity User Updates against baseline<br>
   * </pre>
   *
   * @param selector Selector using which to find the button
   * @param selectorValue Value of the selector
   * @param viewPort the view port
   */
  @And("^I verify element with (id|class|name|xpath) (.*) on viewport (.*) against baseline$")
  public void checkElement(String selector, String selectorValue, String viewPort){
    visualTestingHarness.checkElementOnViewPortBySelector(selector, selectorValue, viewPort);
  }

  /**
   * Click on Applitools locator.
   *
   * @param locator the locator to click on
   * @param count single or double click
   */
  @And("^I click on (.*) locator( twice)?$")
  public void clickOnLocator(String locator, String count) {
    visualTestingHarness.clickLocator(locator, count != null);
  }

  /**
   * Right-click on Applitools locator.
   *
   * @param locator the locator to right-click
   */
  @And("^I right-click on (.*) locator$")
  public void rightClickLocator(String locator) {
    visualTestingHarness.rightClickLocator(locator);
  }

  /**
   * Click on menu item locator from Applitools locator.
   *
   * @param menuItemLocator the menu item locator to select
   * @param locator         the locator to right-click on for menu opening
   */
  @And("^I select (.*) menu item locator from (.*) locator")
  public void selectMenuLocator(String menuItemLocator, String locator) {
    visualTestingHarness.rightClickLocator(locator);
    visualTestingHarness.clickLocator(menuItemLocator,false);
  }

  /**
   * Click on a locator and enter text.
   *
   * @param text    the text to type
   * @param locator the locator to click before entering the text
   */
  @And("^I type text (.*) on (.*) locator")
  public void typeTextInLocator(String text, String locator) {
    visualTestingHarness.typeTextInLocator(locator, text);
  }

  /**
   * Drag and drop an locator into another locator.
   *
   * @param locatorToDrag   The locator which to drag and drop
   * @param locatorToDropAt The locator at which to drop locatorToDrag
   */
  @And("^I drag and drop locator (.*) at (.*) locator")
  public void dndLocator(String locatorToDrag, String locatorToDropAt) {
    visualTestingHarness.dndLocator(locatorToDrag, locatorToDropAt);
  }

  /**
   * Click on percentage of Applitools locator
   * In this step image locator is divided on 100% percent and could be clicked on any percent of it.
   * 1% is the max left and 100% is the max right of the image locator
   *
   * @param locator    the locator to be clicked on
   * @param percentage percentage of the image locator to be clicked
   */
  @And("^I click on (.*) locator at (\\d+) percent$")
  public void clickOnLocatorAt(String locator, int percentage) {
    visualTestingHarness.clickLocatorPercentage(locator, percentage, 50);
  }

  /**
   * Click on specific point of Applitools locator
   * In this step image locator is divided on 100% percent vertically and horizontally and could be clicked anywhere inside it.
   *
   * <pre>Example:
   * I click on button-browse locator at 50 percent width and 50 percent height</pre>
   * as a result the click will be made in the center of the locator
   *
   * @param locator          the locator to be clicked on
   * @param percentageWidth  percentage of the image locator to be clicked
   * @param percentageHeight percentage of the image locator to be clicked
   */
  @And("^I click on (.*) locator at (\\d+) percent width and (\\d+) percent height")
  public void clickOnLocatorAtSpecificPoint(String locator, int percentageWidth, int percentageHeight) {
    visualTestingHarness.clickLocatorPercentage(locator, percentageWidth, percentageHeight);
  }

  /**
   * Click on percentage of Applitools locator and enter text
   * In this step image locator is divided on 100% percent and could be clicked on any percent of it.
   * 1% is the max left and 100% is the max right of the image locator
   *
   * @param locator The locator which has to be clicked
   * @param text    Text which will be entered
   * @param percentage    Percentage of the locator where text will be entered
   */
  @And("^I type text (.*) on (.*) locator at (\\d+) percent$")
  public void typeTextOnLocatorPercentage(String text,String locator, int percentage) {
    visualTestingHarness.typeTextInLocatorPercentage(locator, text, percentage);
  }

  /**
   * Click on Applitools locator if locator is displayed on viewport.
   *
   * @param locator the locator to click on
   */
  @And("^I click on (.*) locator if it is visible$")
  public void clickOnLocatorIfVisible(String locator) {
    if (visualTestingHarness.isLocatorVisible(locator)) {
      visualTestingHarness.clickLocator(locator,false);
    }
  }

  /**
   * Wait for locator to be shown
   *
   * @param seconds time to wait for locator to be shown
   * @param locator locator which is waiting to be shown
   */
  @And("^I wait (\\d+) seconds locator (.*) to be shown")
  public void waitForLocator(int seconds, String locator){

    for (int second = 0; second < seconds; second++) {

      if (visualTestingHarness.isLocatorVisible(locator)) {
        break;
      }else if(seconds - 1 == second){
        throw new TestException(String.format("Locator %s was not shown in the viewport", locator)); //NOI18N
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.info(String.format("Interrupted! %s", e)); //NOI18N
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Check that the selected locator is not presented in the current viewport
   *
   * @param locator locator which is waiting to be shown
   */
  @And("^I check that locator (.*) is not present in the viewport")
  public void checkForPresence(String locator){
      if (visualTestingHarness.isLocatorVisible(locator))
      {
        throw new TestException(locator+" is still visible in the viewport"); // NOI18N
      }
  }

  /**
   * Stop visual testing.
   *
   */
  @And("^I stop visual testing$")
  public void stopVisualTesting() {
    visualTestingHarness.stop();
  }

}
