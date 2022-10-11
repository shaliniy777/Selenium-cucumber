/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.FSOperations;
import com.experian.automation.helpers.webservers.TomcatOperations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Web engine tomcat operations.
 */
public class WebEngineTomcatOperations extends TomcatOperations {

  /**
   * Instantiates a new Web engine tomcat operations.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public WebEngineTomcatOperations() throws IOException, ConfigurationException {
    super();
    startWorkDirPath = getStartWorkingDirPath();
    stopWorkDirPath = startWorkDirPath;
    startTimeout = 3 * 60 * 1000;

    pidFilePath = startWorkDirPath + "/catalina.pid";
    windowTitle = "WebEngine Tomcat";
    setEnvironmentVariables();
  }

  @Override
  public void stop() throws Exception {
    super.stop();

    if (SystemUtils.IS_OS_WINDOWS) {
      killTomcatByWindowTitle();
    }
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    ArrayList<Integer> ports = new ArrayList<Integer>();
    ports.add(Integer.parseInt(Config.get("webengine.http.port")));
    ports.add(Integer.parseInt(Config.get("webengine.shutdown.port")));
    return ports;
  }

  @Override
  protected String getStartWorkingDirPath() {
    return Config.get("webengine.home") + "/bin";
  }

  @Override
  protected List<String> getServiceFilesToBackup() throws Exception {
    List<String> filesToBackup = new ArrayList<>();
    filesToBackup.add("WEB-INF/we-httpHeaderResponseFilter.properties");
    filesToBackup.add("WEB-INF/web.xml");
    return filesToBackup;
  }

  public List<String> getServiceLogs() throws Exception {
    FSOperations fsOperations = new FSOperations();
    List<String> logs = new ArrayList<>();
    logs.add(Config.getAsUnixPath("webengine.home") + "/logs");
    logs.add(fsOperations.getHomePath() + "/PowerCurve/Web Engine/logs/webengine.log");
    logs.add(fsOperations.getHomePath() + "/PowerCurve/Web Engine/logs/consolidated.log");
    return logs;
  }

}





