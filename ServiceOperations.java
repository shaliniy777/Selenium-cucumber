/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Service operations.
 */
public abstract class ServiceOperations {

  private static final String WAIT_FOR_PORTS_MESSAGE = "waitForPorts - port: <";

  /**
   * The Service name.
   */
  protected String serviceName;
  /**
   * The Service path.
   */
  protected String servicePath;
  /**
   * The Run as service.
   */
  protected Boolean runAsService = false;
  /**
   * The Start executable.
   */
  protected String startExecutable;
  /**
   * The Stop executable.
   */
  protected String stopExecutable;
  /**
   * The Start work dir path.
   */
  protected String startWorkDirPath;
  /**
   * The Stop work dir path.
   */
  protected String stopWorkDirPath;
  /**
   * The Start timeout.
   */
  protected int startTimeout = 60 * 1000;
  /**
   * The Stop timeout.
   */
  protected int stopTimeout = 60 * 1000;
  /**
   * The Pid file path.
   */
  protected String pidFilePath;
  /**
   * The Environment variables.
   */
  protected HashMap<String, String> environmentVariables = new HashMap<>();

  private final Logger logger = Logger.getLogger(this.getClass());

  /**
   * Start service
   *
   * @throws Exception the exception
   */
  public void start() throws Exception {
    CommandLineExecutor cle = getStartCommand();
    cle.execute();

    if (pidFilePath != null) {
      logger.info(String.format("ServiceOperations - start PID=<%s>", cle.getPID().toString()));
      FileUtils.writeStringToFile(new File(pidFilePath), cle.getPID().toString(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Stop service. Service process will be killed if soft stop is not working or configured
   *
   * @throws Exception the exception
   */
  public void stop() throws Exception {
    stop(true);
  }

  /**
   * Stop service. Service process will be killed if soft stop is not configured or not working and forceStop=true
   *
   * @param forceStop - whether to stop the service forcefully if soft stop does not work
   * @throws Exception the exception
   */
  public void stop(boolean forceStop) throws Exception {

    Boolean killService = false;

    if (!StringUtils.isEmpty(stopExecutable) || runAsService) {
      getStopCommand().execute();
    } else {
      killService = true;
    }

    if (killService || (!isStopped() && forceStop)) {
      forceStop();
    }
  }

  /**
   * Kill service process forcefully
   *
   * @throws Exception the exception
   */
  public void forceStop() throws Exception {
    killServiceByPid();
    killServiceByPorts();
  }

  /**
   * Wait for service to start
   *
   * @throws Exception the exception
   */
  public void waitToStart() throws Exception {
    if (!waitForPorts(startTimeout, true)) {
      throw new Exception(String.format("Failed to start the service after: <%d> milliseconds", startTimeout));
    }
  }

  /**
   * Wait for service to stop
   *
   * @throws Exception the exception
   */
  public void waitToStop() throws Exception {
    if (!waitForPorts(stopTimeout, false)) {
      throw new Exception(String.format("Failed to stop the service after: <%d> milliseconds", stopTimeout));
    }
  }

  /**
   * Check if the service is started
   *
   * @return true - started, false - stopped
   * @throws Exception the exception
   */
  public Boolean isStarted() throws Exception {
    return waitForPorts(0, true);
  }

  /**
   * Check if the service is stopped
   *
   * @return true - stopped, false - started
   * @throws Exception the exception
   */
  public Boolean isStopped() throws Exception {
    return waitForPorts(0, false);
  }

  /**
   * Return list of service port
   *
   * @return the service ports
   * @throws Exception the exception
   */
  protected abstract List<Integer> getServicePorts() throws Exception;

  /**
   * Gets service logs.
   *
   * @return the service logs
   * @throws Exception the exception
   */
  /*
   * Return list of paths to logs
   */
  protected List<String> getServiceLogs() throws Exception {
    return new ArrayList<>();
  }

  /**
   * Gets service files to backup.
   *
   * @return the service files to backup
   * @throws Exception the exception
   */
  /*
   * Return list of relative paths to files for backup
   */
  protected List<String> getServiceFilesToBackup() throws Exception {
    return new ArrayList<>();
  }

  /**
   * Get service path string.
   *
   * @return the string
   */
  /*
   * Return absolute path to the service
   */
  public String getServicePath(){
    return servicePath;
  }

  /**
   * Get service command
   *
   * @param action the action
   * @return String service command
   */
  protected String getServiceCommand(String action) {
    if (SystemUtils.IS_OS_WINDOWS) {
      return String.format("net %s %s", action, serviceName);
    } else {
      return String.format("systemctl %s %s", action, serviceName);
    }
  }

  /**
   * Get start command
   *
   * @return CommandLineExecutor start command
   */
  protected CommandLineExecutor getStartCommand() {
    CommandLineExecutor cle = new CommandLineExecutor();
    cle.setCommand(runAsService ? getServiceCommand("start") : startExecutable);
    cle.setWorkingDir(Objects.toString(startWorkDirPath, ""));
    cle.setWaitFor(false);

    if (!environmentVariables.isEmpty()) {
      cle.setEnvironmentVariables(environmentVariables);
    }

    return cle;
  }

  /**
   * Get stop command
   *
   * @return CommandLineExecutor stop command
   */
  protected CommandLineExecutor getStopCommand() {
    CommandLineExecutor cle = new CommandLineExecutor();
    cle.setCommand(runAsService ? getServiceCommand("stop") : stopExecutable);
    cle.setWorkingDir(Objects.toString(stopWorkDirPath, ""));
    cle.setWaitFor(true);

    if (!environmentVariables.isEmpty()) {
      cle.setEnvironmentVariables(environmentVariables);
    }

    return cle;
  }

  /**
   * Kill service by ports
   *
   * @throws Exception the exception
   */
  protected void killServiceByPorts() throws Exception {
    TaskKill taskKill = new TaskKill();
    for (Integer port : getServicePorts()) {
      taskKill.killConnectionsOnAllInterfaces(port);
    }
  }

  /**
   * Kill service by process id
   *
   * @throws Exception the exception
   */
  protected void killServiceByPid() throws Exception {
    if (pidFilePath != null) {
      File pidFile = new File(pidFilePath);
      if (pidFile.exists()) {
        Integer pid = Integer.valueOf(FileUtils.readFileToString(new File(pidFilePath), StandardCharsets.UTF_8).trim());
        logger.info(String.format("killServiceByPid - PID=<%s>", pid));

        TaskKill taskKill = new TaskKill();
        taskKill.killByPid(pid);

        FileUtils.forceDelete(new File(pidFilePath));
      }
    }
  }

  /**
   * Wait for port to become un/available
   *
   * @param timeout - Timeout in milliseconds
   * @param status  - false - service is not listening, true - service is listening
   * @return true - expected status is matched, false - expected status is matched,
   * @throws Exception the exception
   */
  protected Boolean waitForPorts(Integer timeout, Boolean status) throws Exception {
    NetworkOperations netOps = new NetworkOperations();
    for (Integer port : getServicePorts()) {
      logger.info(String.format("%s%s>", WAIT_FOR_PORTS_MESSAGE, port));
      netOps.setPort(port);
      if (netOps.checkPortAvailability(timeout, status) == false) {
        logger.info(String.format("%s%s > failed", WAIT_FOR_PORTS_MESSAGE, port));
        return false;
      }
      logger.info(String.format("%s%s > success", WAIT_FOR_PORTS_MESSAGE, port ));
    }

    return true;
  }

  /**
   * Read port from property configuration file
   * <p>
   * Example: (file, "myservice.port")
   *
   * @param filePath - Path to config file
   * @param property - Name of property to read
   * @return Integer - port number
   * @throws Exception the exception
   */
  protected Integer getPortFromConfigPropertiesFile(String filePath, String property) throws Exception {
    FSOperations fsOperations = new FSOperations();
    return Integer.valueOf(fsOperations.getValueFromPropertiesFile(filePath, property));
  }


  /**
   * Read port from XML configuration file
   * <p>
   * Example: (file, "/conf/service[@name='myservice']/@port")
   *
   * @param filePath - Path to config file
   * @param xpath    - Xpath expression
   * @return Integer - port number
   * @throws Exception the exception
   */
  protected Integer getPortFromConfigXMLFile(String filePath, String xpath) throws Exception {
    return Integer.valueOf(XMLOperations.getValueFromXMLFile(filePath, xpath));
  }

  /**
   * Gathers logs from different service logs directories to one single directory
   *
   * @param dstPath Path to the directory where the logs will be moved
   * @throws Exception the exception
   */
  public void collectLogs(String dstPath) throws Exception {
    List<String> logsFiles = getServiceLogs();
    if (!logsFiles.isEmpty()) {
      for (String log : logsFiles) {
        FSOperations fsOperation = new FSOperations();
        if (fsOperation.exists(log)) {
          File file = new File(log);
          if (file.isFile()) {
            File dstPathAsFile = new File(dstPath);
            FileUtils.copyFileToDirectory(file, dstPathAsFile);
          } else {
            fsOperation.copyDirectory(file.getPath(), dstPath);
          }
        } else {
          throw new Exception(String.format("There is no such file or directory: " + log + " !"));
        }
      }
    }
  }

  /**
   * Backup a list of files to given location. Skip backup for files that already exist in backup location.
   *
   * @param backupPath  location where the backup will be stored
   * @param servicePath path to service
   * @param srcHost     host to backup from
   * @param dstHost     host to backup to
   * @throws Exception the exception
   */
  public void backupServiceFiles(String backupPath, String servicePath, String srcHost, String dstHost)
      throws Exception {

    FSOperations fs = new FSOperations();
    fs.srcHost = srcHost;
    fs.dstHost = dstHost;

    List<String> filesToBackup = getServiceFilesToBackup();

    for (String fileToBackup : filesToBackup) {
      File f = new File(servicePath + "/" + fileToBackup);

      if (!fs.exists(backupPath + "/" + fileToBackup)) {
        if (f.isFile()) {
          fs.copyFile(servicePath + "/" + fileToBackup, backupPath + "/" + fileToBackup);
        } else {
          fs.copyDirectory(servicePath + "/" + fileToBackup, backupPath + "/" + fileToBackup);
        }
      }
    }
  }

  /**
   * Restore a list of files from given location
   *
   * @param backupPath  location where the files are stored
   * @param servicePath path to service
   * @param srcHost     host to restore from
   * @param dstHost     host to restore to
   * @throws Exception the exception
   */
  public void restoreServiceFiles(String backupPath, String servicePath, String srcHost, String dstHost)
      throws Exception {

    FSOperations fs = new FSOperations();
    fs.srcHost = srcHost;
    fs.dstHost = dstHost;

    List<String> filesToRestore = getServiceFilesToBackup();

    for (String fileToRestore : filesToRestore) {
      File f = new File(backupPath + "/" + fileToRestore);

      if (f.isFile()) {
        fs.copyFile(backupPath + "/" + fileToRestore, servicePath + "/" + fileToRestore);
      } else {
        fs.copyDirectory(backupPath + "/" + fileToRestore, servicePath + "/" + fileToRestore);
      }
    }
  }

  /**
   * Delete service files returned from getServiceFilesToBackup()
   *
   * @param servicePath path to service
   * @throws Exception the exception
   */
  public void clearServiceFiles(String servicePath) throws Exception {
    FSRuntime fso = new FSRuntime();

    for (String fileToBackup : getServiceFilesToBackup()) {
      File file = new File(servicePath + "/" + fileToBackup);

      if (file.exists()) {
        fso.delete(file);
      }
    }
  }

  /**
   * Log netstat output for all service ports
   *
   * @throws Exception the exception
   */
  public void logPortsState() throws Exception {
    String netstatCommand = SystemUtils.IS_OS_WINDOWS ? "netstat -aon | findstr :%s" : "netstat -anptu | grep :%s";
    CommandLineExecutor cle = new CommandLineExecutor();
    for (Integer servicePort : getServicePorts()) {
      cle.setCommand(String.format(netstatCommand, servicePort));
      cle.setWaitFor(true);
      cle.execute();
      logger.info(String.format("netstat for process on port: %s", servicePort));
      logger.info(String.format("%s", cle.getOutput()));
    }
  }

  /**
   * Clear service logs
   *
   * @throws Exception the exception
   */
  public void clearLogs() throws Exception {
    String servicePath = getServicePath();
    List<String> ServiceLogs = getServiceLogs();
    FSOperations fsOperations = new FSOperations();
    for (String sl : ServiceLogs){
      fsOperations.delete(servicePath + "/" + sl);
    }
  }
}