/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.webservers.TomcatOperations;
import com.experian.automation.logger.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang.StringUtils;

/**
 * The type Open am tomcat operations.
 */
public class OpenAMTomcatOperations extends TomcatOperations {

  private static final Logger logger = Logger.getLogger(OpenAMTomcatOperations.class);
  private static final String OPENAM_TOMCAT_HOME = "openam.tomcat.home";  //NOI18N
  private static final String OPENAM_STOP_PROTECTION = "openam.stop.protection"; //NOI18N

  /**
   * Instantiates a new Open am tomcat operations.
   *
   * @throws IOException            the io exception
   * @throws ConfigurationException the configuration exception
   */
  public OpenAMTomcatOperations() throws IOException, ConfigurationException {
    super();
    startWorkDirPath = getStartWorkingDirPath();
    stopWorkDirPath = startWorkDirPath;
    startTimeout = 3 * 60 * 1000;

    pidFilePath = startWorkDirPath + "/catalina.pid";  //NOI18N
    windowTitle = "Tomcat";  //NOI18N
    setEnvironmentVariables();

    serviceName = Config.get("openam.service.name");   //NOI18N

    String runAsServiceConfig = Config.get("openam.as.service");   //NOI18N
    if(runAsServiceConfig != null){
      runAsService = runAsServiceConfig.equals("true");   //NOI18N
    }

    if (runAsService && StringUtils.isEmpty(serviceName)) {
      logger.info("OpenAM is configured to run as service, but openam.service.name is not set");    //NOI18N
      throw new ConfigurationException();
    }

  }

  @Override
  public List<String> getServiceLogs() throws Exception {
    List<String> logs = new ArrayList<>();
    logs.add(Config.getAsUnixPath(OPENAM_TOMCAT_HOME) + "/logs");   //NOI18N
    logs.add(Config.getAsUnixPath(OPENAM_TOMCAT_HOME) + "/webapps/sso/sso/debug");   //NOI18N
    return logs;
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    ArrayList<Integer> ports = new ArrayList<>();
    ports.add(Integer.parseInt(Config.get("openam.http.port")));   //NOI18N
    ports.add(Integer.parseInt(Config.get("openam.shutdown.port")));   //NOI18N
    return ports;
  }

  @Override
  protected List<String> getServiceFilesToBackup() throws Exception {
    List<String> filesToBackup = new ArrayList<>();
    filesToBackup.add("WEB-INF/openam-httpHeaderResponseFilter.properties");   //NOI18N
    filesToBackup.add("../../conf/catalina.properties");   //NOI18N
    return filesToBackup;
  }

  @Override
  protected String getStartWorkingDirPath() throws IOException, ConfigurationException {
    return Config.get(OPENAM_TOMCAT_HOME) + "/bin";   //NOI18N
  }

  @Override
  public void stop() throws Exception {
    if ("true".equalsIgnoreCase(Config.get(OPENAM_STOP_PROTECTION))) { //NOI18N
      logger.info("OpenAM is configured to run with 'stop protection'. The stop function will be ignored"); //NOI18N
      return;
    }
    super.stop();

    if (!isStopped()) {
      killTomcatByWindowTitle();
    }
  }

  @Override
  public void waitToStop() throws Exception {
    if ("true".equalsIgnoreCase(Config.get(OPENAM_STOP_PROTECTION))) { //NOI18N
      logger.info("OpenAM is configured to run with 'stop protection'. The wait for stop function will be ignored"); //NOI18N
      return;
    }
    super.waitToStop();
  }
}
