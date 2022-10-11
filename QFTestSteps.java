/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.steps;

import static org.testng.Assert.assertEquals;

import com.experian.automation.cucumber.configuration.ConfigurationProperties;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSDesign;
import com.experian.automation.helpers.FSRuntime;
import com.experian.automation.helpers.ProcessExecutor;
import com.experian.automation.helpers.STAFExecutor;
import com.experian.automation.helpers.Variables;
import com.experian.automation.logger.Logger;
import com.experian.automation.transformers.VariablesTransformer;
import com.opencsv.CSVWriter;
import io.cucumber.java.en.And;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;

/**
 * The type Qf test steps.
 */
@ConfigurationProperties(
    properties = {
        "qftest.bin",
        "qftest.suite.dir",
        "qftest.suite.name",
        "qftest.tmp.dir",
        "design.host",
        "runtime.host"
    }
)
public class QFTestSteps {

  private static final String TMP_DIR_PROP = "temp.dir";
  private static final String QFTEST_TMP_DIR_PROP = "qftest.tmp.dir";
  private static final String QFTEST_SUITE_NAME_PROP = "${qftest.suite.name}";

  private static final String REPORT_DIR_LOC = "/report";

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Instantiates a new Qf test steps.
   */
  public QFTestSteps() {
    // Blank Constructor
  }

  /**
   * Execute qf test procedures.
   *
   * @param dataTable the data table
   * @throws Throwable the throwable
   */
  /*
  * Usage example(s):
  *   And I execute QFTest procedures:
  *   | PCC.DCA        | CreateDCA                            | { 'name': 'My DCA', 'internalUser': 'true', 'active': 'true', 'commisionDelay': '0', 'accountQuota': '0' }                 |
  *   | PCC.DCA        | CreatePlacementType                  | { 'name': 'My Placement Type' }                                                                                            |
  *   | PCC.DCA        | CreateDebtstream                     | { 'name': 'My Debtstream', 'owner': 'DefaultOwner', 'placementType': 'My Placement Type', 'displayName': 'My Debtstream' } |
  *   | PCC.DCA        | AddDCAToDebtstream                   | { 'dca': 'My DCA' }                                                                                                        |
  */
  @And("^I execute QFTest procedures:$")
  public void executeQFTestProcedures(List<List<String>> dataTable) throws Throwable {
    prepareCSVdata(dataTable);
    executeQFTestProcedures();
  }

  /**
   * Execute qf test procedures.
   *
   * @throws Throwable the throwable
   */
  @And("^I execute QFTest procedures$")
  public void executeQFTestProcedures() throws Throwable {

    String cucumberFile = "qftest-cucumber.properties";
    String csvFile = "qftest-data-table.csv";

    String localCVSFile = Config.getAsUnixPath(TMP_DIR_PROP) + "/" + csvFile;
    String remoteCVSFile = Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + "/" + csvFile;
    String localCucumberFile = Config.getAsUnixPath(TMP_DIR_PROP) + "/" + cucumberFile;
    String remoteCucumberFile = Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + "/" + cucumberFile;

    // Create Cucumber properties file
    Properties cucumberProperties = Config.getProperties();
    cucumberProperties.putAll(Variables.getAll());

    try (OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(localCucumberFile), StandardCharsets.UTF_8)) {
      cucumberProperties.store(output,null);
    }

    String qftestBinnary = FilenameUtils.separatorsToUnix(Config.get("qftest.bin"));

    String qftestCommandArgs =
        " -batch -compact " +
            " -runlog " + Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + "/report/+b.qzp" +
            " -report.xml " + Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + REPORT_DIR_LOC +
            " -variable CSVDATAFILE=" + remoteCVSFile +
            " -variable CUCUMBERDATAFILE=" + remoteCucumberFile +
            " " + Config.getAsUnixPath("qftest.suite.dir") + "/" + VariablesTransformer.transformSingleValue(
            QFTEST_SUITE_NAME_PROP);

    String command;
    int processRC;

    String studioHost = Config.get("design.host");

    logger.info("QFTest execution will be started. \nWaiting ...");

    // Copy local QFTest CSV file to remote machine
    new FSRuntime().copyFile(localCucumberFile, remoteCucumberFile);

    if (!studioHost.equals("localhost")) {
      command = String.format("START SHELL COMMAND \"\\\"%s\\\" %s \" WAIT 2h NEWCONSOLE", qftestBinnary,
                              qftestCommandArgs);
      processRC = new STAFExecutor().execute(studioHost, "PROCESS", command);
    } else {
      command = qftestBinnary + qftestCommandArgs;
      processRC = new ProcessExecutor().execute(command);
    }

    logger.info(String.format("%s\nQFTest execution completed!\n", command));

    // Clean report dir if QFTest execution is successful
    if (processRC == 0 || processRC == 1) {
      new FSRuntime().delete(Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + REPORT_DIR_LOC);
    } else {

      String localReportFile = Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + "/report/"
          + FilenameUtils.getBaseName(
          VariablesTransformer.transformSingleValue(QFTEST_SUITE_NAME_PROP)) + ".qzp";
      String remoteReportFile = Config.getAsUnixPath(TMP_DIR_PROP) + "/"
          + FilenameUtils.getBaseName(
          VariablesTransformer.transformSingleValue(QFTEST_SUITE_NAME_PROP)) + ".qzp";

      new FSDesign().copyFile(localReportFile, remoteReportFile);
    }

    new FSRuntime().delete(localCVSFile);

    // Exit code 0 - success, 1 - warnings occurred
    assertEquals(processRC == 0 || processRC == 1, true, "QFTest execution failed! Report location: \"" +
        Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + REPORT_DIR_LOC + "\". Exit code " + processRC);
  }

  /**
   * Prepare cs vdata.
   *
   * @param dataTable the data table
   * @throws Throwable the throwable
   */
  /*
  * Usage example(s):
  *   And I prepare CSV data file for QFTest execution:
  *   | PCC.DCA        | CreateDCA                            | { 'name': 'My DCA', 'internalUser': 'true', 'active': 'true', 'commisionDelay': '0', 'accountQuota': '0' }                 |
  *   | PCC.DCA        | CreatePlacementType                  | { 'name': 'My Placement Type' }                                                                                            |
  *   | PCC.DCA        | CreateDebtstream                     | { 'name': 'My Debtstream', 'owner': 'DefaultOwner', 'placementType': 'My Placement Type', 'displayName': 'My Debtstream' } |
  *   | PCC.DCA        | AddDCAToDebtstream                   | { 'dca': 'My DCA' }                                                                                                        |
  */
  @And("^I prepare CSV data file for QFTest execution:$")
  public void prepareCSVdata(List<List<String>> dataTable) throws Throwable {

    String csvFile = "qftest-data-table.csv";
    String localCVSFile = Config.getAsUnixPath(TMP_DIR_PROP) + "/" + csvFile;
    String remoteCVSFile = Config.getAsUnixPath(QFTEST_TMP_DIR_PROP) + "/" + csvFile;

    CSVWriter writer;
    if (!new FSRuntime().exists(localCVSFile)) {
      // Create CSV file
      writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(localCVSFile), StandardCharsets.UTF_8));
      // Add header
      String[] header = {"package", "action", "fParam"};
      writer.writeNext(header);
    } else {
      writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(localCVSFile, true), StandardCharsets.UTF_8));
    }
    for (int i = 0; i < dataTable.size(); i++) {
      String[] entries = dataTable.get(i).toArray(new String[dataTable.get(i).size()]);
      writer.writeNext(entries);
    }
    writer.close();

    new FSRuntime().copyFile(localCVSFile, remoteCVSFile);
  }
}
