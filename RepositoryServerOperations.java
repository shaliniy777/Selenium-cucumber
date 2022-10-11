/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.ServiceOperations;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Repository server operations.
 */
public class RepositoryServerOperations extends ServiceOperations {

  private static final String REPO_PATH_PROP = "repository.path";

  /**
   * Instantiates a new Repository server operations.
   *
   * @throws Exception the exception
   */
  public RepositoryServerOperations() throws Exception {

    Config config = new Config();

    startExecutable = (SystemUtils.IS_OS_WINDOWS) ? "run.bat" : "./run.sh";
    stopExecutable = (SystemUtils.IS_OS_WINDOWS) ? "stop.bat" : "./stop.sh";
    startWorkDirPath = config.getAsUnixPath(REPO_PATH_PROP);
    stopWorkDirPath = config.getAsUnixPath(REPO_PATH_PROP);
    startTimeout = 7 * 60 * 1000;
    stopTimeout = 60 * 1000;
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {

    List<Integer> servicePorts = new ArrayList<Integer>();

    String configFilePath = this.startWorkDirPath + "/conf/org.ops4j.pax.web.cfg";
    servicePorts.add(getPortFromConfigPropertiesFile(configFilePath, "org.osgi.service.http.port"));

    return servicePorts;
  }

  @Override
  public List<String> getServiceLogs() throws Exception {
    List<String> logs = new ArrayList<>();
    logs.add(Config.getAsUnixPath(REPO_PATH_PROP) + "/logs/consolidated_logs.log");
    logs.add(Config.getAsUnixPath(REPO_PATH_PROP) + "/logs/derby.log");
    return logs;
  }
}
