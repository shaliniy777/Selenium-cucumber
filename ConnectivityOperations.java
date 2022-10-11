/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.ArchiversOperations;
import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.ServiceOperations;
import com.experian.automation.helpers.TextFileOperations;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;


/**
 * The type Connectivity operations.
 */
public class ConnectivityOperations extends ServiceOperations {

  private String connectivitySolutionPath;
  private static final String TEMP_DIR = Config.get("temp.dir"); // NOI18N
  protected String unZipDir;

  /**
   * Instantiates a new Connectivity operations.
   */
  public ConnectivityOperations() {
    connectivitySolutionPath = Config.get("connectivity.solution.path"); // NOI18N
    unZipDir = TEMP_DIR;

    pidFilePath = Config.getAsUnixPath("temp.dir") + "/connectivity.pid";
    startExecutable = (SystemUtils.IS_OS_WINDOWS) ? "run_connectivity.bat" : "./run_connectivity.sh"; // NOI18N
    startWorkDirPath = Config.get("connectivity.path"); // NOI18N
    startTimeout = 120 * 1000;
    stopTimeout = 120 * 1000;
  }

  /**
   * Instantiates a Custom Connectivity operations.
   *
   * @param connectivityPath custom connectivity path
   */
  public ConnectivityOperations(String connectivityPath) {
    this();
    connectivitySolutionPath = connectivityPath + "/sample"; // NOI18N
    unZipDir = TEMP_DIR + "/CX_inbound/"; // NOI18N

    startWorkDirPath = connectivityPath + "/bin"; // NOI18N
  }

  @Override
  public void waitToStart() throws Exception {
    super.waitToStart();
    final String regex = "Connectivity started up successfully"; // NOI18N
    int fileLookupTimeout = 1200000;

    String systemLog = connectivitySolutionPath + "/logs/System.log"; // NOI18N
    boolean systemLogExist = new FSOperations().waitForFile(FilenameUtils.separatorsToUnix(systemLog),
                                                            fileLookupTimeout);

    if (systemLogExist) {
      new TextFileOperations().filterByRegex(FilenameUtils.separatorsToUnix(systemLog),
                                             regex, 72, 2000L);
    } else {
      throw new IllegalArgumentException("System.log file not available"); // NOI18N
    }
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    FSOperations fsOperations = new FSOperations();
    ArrayList<Integer> ports = new ArrayList<>();
    String propFilePath = FilenameUtils.separatorsToUnix(
        connectivitySolutionPath + "/conf/system/system.properties"); // NOI18N
    Path myPath = Paths.get(propFilePath);

    if (!myPath.toFile().exists()) {
      return Collections.emptyList();
    }
    String connectivityURL = fsOperations.getValueFromPropertiesFile(propFilePath, "health.url"); // NOI18N
    ports.add(new URL(connectivityURL).getPort());
    return ports;
  }

  @Override
  public List<String> getServiceLogs() {
    List<String> logs = new ArrayList<>();
    logs.add(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/logs/System.log")); // NOI18N
    logs.add(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/logs/Transactions.log")); // NOI18N
    logs.add(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/logs/Trace.log")); // NOI18N
    logs.add(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/logs/Others.log")); // NOI18N
    return logs;
  }

  /**
   * Deploy.
   *
   * @param filePath the file path
   * @throws Exception the exception
   */
  public void deploy(String filePath) throws Exception {
    ArchiversOperations ao = new ArchiversOperations();
    String fileName = new File(filePath).getName();
    String solutionUnzipPath = FilenameUtils.separatorsToUnix(
        unZipDir + fileName.substring(0, fileName.lastIndexOf('.'))); // NOI18N
    ao.unzip(filePath, solutionUnzipPath);
    FSOperations fs = new FSOperations();
    fs.copyDirectory(solutionUnzipPath, FilenameUtils.separatorsToUnix(connectivitySolutionPath));
    fs.delete(solutionUnzipPath);
  }

  /**
   * Clean deployed files.
   *
   * @throws Exception the exception
   */
  public void cleanDeployedFiles() throws Exception {
    FSOperations fs = new FSOperations();
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/logs")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/processflows/startup")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/mapping")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/files")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/utilities/script")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/utilities/schema")); // NOI18N
    fs.delete(FilenameUtils.separatorsToUnix(connectivitySolutionPath + "/utilities/template")); // NOI18N
  }
}
