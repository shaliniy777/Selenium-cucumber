/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;


import com.experian.automation.logger.Logger;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import java.lang.reflect.Field;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Command line executor.
 */
public class CommandLineExecutor {

  /**
   * The type Stream gatherer.
   */
  class StreamGatherer implements Runnable {

    private String output;
    private boolean done = false;
    private BufferedReader is;

    /**
     * Instantiates a new Stream gatherer.
     *
     * @param is the is
     */
    public StreamGatherer(BufferedReader is) {
      this.output = "";
      this.is = is;
    }

    public void run() {
      try {
        String line;
        while (((line = is.readLine()) != null)) {
          output += line + "\n";
        }
        is.close();
      } catch (IOException e) {
        output += "Exception:" + e.getMessage() + "\n";
      }
  
      this.escapeOutput();

      done = true;
    }

    /**
     * Gets output.
     *
     * @return the output
     */
    public String getOutput() {
      return output;
    }

    /**
     * Escape output.
     */
    protected void escapeOutput() {
      output = output.replace("\f", "\\\\f");
    }
  }

  private class TimeoutProcess extends Thread {

    private long timeoutMillis;
    private Process process;
    private boolean killed = false;

    /**
     * Instantiates a new Timeout process.
     *
     * @param process       the process
     * @param timeoutMillis the timeout millis
     */
    TimeoutProcess(Process process, long timeoutMillis) {
      this.process = process;
      this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(timeoutMillis);
        process.destroy();
        this.killed = true;
      } catch (InterruptedException e) {
      }
    }

    /**
     * Is killed boolean.
     *
     * @return the boolean
     */
    boolean isKilled() {
      return killed;
    }

    /**
     * Wait for.
     *
     * @throws InterruptedException the interrupted exception
     */
    void waitFor() throws InterruptedException {
      start();
      process.waitFor();
      interrupt();
    }
  }

  private final Logger logger = Logger.getLogger(this.getClass());
  private Process process;
  private String output;
  private String command;
  private String workingDir = "";
  private Boolean waitFor = false;
  private HashMap<String, String> environmentVariables = new HashMap<>();
  private StreamGatherer gatherer;
  private Integer exitCode;
  private Long processID = 0L;
  private long processTimeoutMillis = 600000;
  private String CLI;
  private String CLIOptions;
  private Thread gathererThread;

  /**
   * Sets command.
   *
   * @param command the command
   */
  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * Gets command.
   *
   * @return the command
   */
  public String getCommand() {
    return command;
  }

  /**
   * Sets working dir.
   *
   * @param workingDir the working dir
   */
  public void setWorkingDir(String workingDir) {
    this.workingDir = FilenameUtils.separatorsToUnix(workingDir);
  }

  /**
   * Gets working dir.
   *
   * @return the working dir
   */
  public String getWorkingDir() {
    return workingDir;
  }

  /**
   * Sets environment variables.
   *
   * @param environmentVariables the environment variables
   */
  public void setEnvironmentVariables(HashMap<String, String> environmentVariables) {
    this.environmentVariables = environmentVariables;
  }

  /**
   * Gets environment variables.
   *
   * @return the environment variables
   */
  public HashMap<String, String> getEnvironmentVariables() {
    return environmentVariables;
  }

  /**
   * Sets wait for.
   *
   * @param waitFor the wait for
   */
  public void setWaitFor(Boolean waitFor) {
    this.waitFor = waitFor;
  }

  /**
   * Gets wait for.
   *
   * @return the wait for
   */
  public Boolean getWaitFor() {
    return waitFor;
  }

  /**
   * Gets pid.
   *
   * @return the pid
   */
  public Long getPID() {
    return processID;
  }

  /**
   * Gets child pi ds.
   *
   * @return the child pi ds
   * @throws Exception the exception
   */
  public List<Long> getChildPIDs() throws Exception {
    return getChildPIDs(processID);
  }

  /**
   * Gets child pi ds.
   *
   * @param pid the pid
   * @return the child pi ds
   * @throws Exception the exception
   */
  public List<Long> getChildPIDs(Long pid) throws Exception {
    List<Long> result = new ArrayList<>();

    if (SystemUtils.IS_OS_WINDOWS) {
      CommandLineExecutor cle = new CommandLineExecutor();
      cle.setCommand(String.format("wmic process where (ParentProcessId=%s) get ProcessId", pid));
      cle.setWorkingDir("C:/Windows/System32/wbem");
      cle.setWaitFor(true);
      cle.execute();
      String[] childPIDs = cle.getOutput().split("\\n");
      for (int i = 0; i < childPIDs.length; i++) {
        String pidValue = childPIDs[i];
        pidValue = pidValue.replaceAll("[|]", "");
        pidValue = pidValue.replace(" ", "");
        if (StringUtils.isNotEmpty(pidValue) && StringUtils.isNumeric(pidValue)) {
          result.add(Long.valueOf(pidValue));
        }
      }
    } else {
      throw new Exception("Operating system is not supported");
    }

    return result;
  }

  /**
   * Gets cli.
   *
   * @return the cli
   */
  public String getCLI() {
    return CLI;
  }

  /**
   * Sets cli.
   *
   * @param CLI the cli
   */
  public void setCLI(String CLI) {
    this.CLI = CLI;
  }

  /**
   * Gets cli options.
   *
   * @return the cli options
   */
  public String getCLIOptions() {
    return CLIOptions;
  }

  /**
   * Sets cli options.
   *
   * @param CLIOptions the cli options
   */
  public void setCLIOptions(String CLIOptions) {
    this.CLIOptions = CLIOptions;
  }

  /**
   * Sets process timeout.
   *
   * @param processTimeoutMillis the process timeout millis
   */
  public void setProcessTimeout(long processTimeoutMillis) {
    this.processTimeoutMillis = processTimeoutMillis;
  }

  /**
   * Instantiates a new Command line executor.
   */
  public CommandLineExecutor() {
    this.output = "";

    if (SystemUtils.IS_OS_LINUX) {

      this.CLI = "bash";
      this.CLIOptions = "-c";
    } else if (SystemUtils.IS_OS_WINDOWS) {

      this.CLI = "cmd.exe";
      this.CLIOptions = "/c";
    }
  }

  /**
   * Execute process.
   *
   * @return the process
   * @throws Exception the exception
   */
  public Process execute() throws Exception {
    return execute(false);
  }

  /**
   * Execute process.
   *
   * @param waitForGathererThreadToFinish the wait for gatherer thread to finish
   * @return the process
   * @throws Exception the exception
   */
  public Process execute(boolean waitForGathererThreadToFinish) throws Exception {
    List<String> processCommand = new ArrayList<String>();
    processCommand.add(CLI);
    processCommand.add(CLIOptions);

    if (workingDir.length() == 0) {
      logger.info(String.format("Executing command %s ", getCommand()));
      Runtime rt = Runtime.getRuntime();
      processCommand.add(command);
      String[] pc = processCommand.toArray(new String[0]);

      process = rt.exec(pc);
    } else {
      ProcessBuilder pb = new ProcessBuilder();
      pb.redirectErrorStream(true);

      Map<String, String> envMap = pb.environment();

      processCommand.add(command);
      pb.command(processCommand);
      pb.directory(new File(FilenameUtils.separatorsToUnix(workingDir)));

      if (environmentVariables.size() != 0) {
        envMap.putAll(environmentVariables);
      }

      logger.info(String.format("Executing command %s", getCommand()));
      process = pb.start();
    }

    gatherer = collectOutput();
    if (waitForGathererThreadToFinish) {
      gathererThread.join();
    }
    if (waitFor) {
      waitForProcessToFinish(processTimeoutMillis);
    }
    processID = setPID();

    return process;
  }

  /**
   * Wait for process to finish.
   *
   * @param timeOutMillis the time out millis
   * @throws InterruptedException the interrupted exception
   */
  public void waitForProcessToFinish(long timeOutMillis) throws InterruptedException {
    if (process.isAlive()) {
      TimeoutProcess timeoutProcess = new TimeoutProcess(process, timeOutMillis);
      long startTime = System.currentTimeMillis();
      timeoutProcess.waitFor();
      long duration = System.currentTimeMillis() - startTime;
      if (timeoutProcess.isKilled()) {
        throw new RuntimeException(
            "Process timed out after " + duration + " milliseconds using timeout " + processTimeoutMillis + ".");
      }
    }
    exitCode = process.exitValue();
  }

  /**
   * Execute process.
   *
   * @param command              the command
   * @param workingDir           the working dir
   * @param environmentVariables the environment variables
   * @param waitFor              the wait for
   * @param processTimeoutMillis the process timeout millis
   * @return the process
   * @throws Exception the exception
   */
  public Process execute(String command, String workingDir, HashMap<String, String> environmentVariables,
      Boolean waitFor, long processTimeoutMillis) throws Exception {
    setCommand(command);
    setWorkingDir(workingDir);
    setEnvironmentVariables(environmentVariables);
    setWaitFor(waitFor);
    setProcessTimeout(processTimeoutMillis);

    process = execute();
    return process;
  }

  /**
   * Gets exit code.
   *
   * @return the exit code
   * @throws Exception the exception
   */
  public Integer getExitCode() throws Exception {
    process.waitFor();
    exitCode = process.exitValue();
    return exitCode;
  }

  /**
   * Gets output.
   *
   * @return the output
   * @throws Exception the exception
   */
  public String getOutput() throws Exception {
    return output = gatherer.getOutput();
  }

  private StreamGatherer collectOutput() throws Exception {
    BufferedReader s = new BufferedReader(
        new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"), 0xffff));
    StreamGatherer gatherer = new StreamGatherer(s);
    gathererThread = new Thread(gatherer);
    try {
      gathererThread.start();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    return gatherer;
  }

  private Long setPID() {
    long processID = -1;
    try {
      if (process.getClass().getName().equals("java.lang.Win32Process") ||
          process.getClass().getName().equals("java.lang.ProcessImpl")) {

        Field f = process.getClass().getDeclaredField("handle");
        f.setAccessible(true);
        long handl = f.getLong(process);
        Kernel32 kernel = Kernel32.INSTANCE;
        WinNT.HANDLE hand = new WinNT.HANDLE();
        hand.setPointer(Pointer.createConstant(handl));
        processID = kernel.GetProcessId(hand);
        f.setAccessible(false);

      } else if (process.getClass().getName().equals("java.lang.UNIXProcess")) {

        Field f = process.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        processID = f.getLong(process);
        f.setAccessible(false);
      }
    } catch (Exception ex) {
      processID = -1;
    }

    return processID;
  }
}