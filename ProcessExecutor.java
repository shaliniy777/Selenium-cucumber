/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

/**
 * The type Process executor.
 */
public class ProcessExecutor {

  /**
   * Execute int.
   *
   * @param command the command
   * @param waitFor the wait for
   * @return the int
   * @throws Exception the exception
   */
  public int execute(String command, Boolean waitFor) throws Exception {

    String[] processCommand = command.split(" ");

    ProcessBuilder pb = new ProcessBuilder().inheritIO().command(processCommand);
    pb.redirectError();

    Process p = pb.start();

    if (waitFor) {
      p.waitFor();

      return p.exitValue();
    }

    return 0;
  }

  /**
   * Execute int.
   *
   * @param command the command
   * @return the int
   * @throws Exception the exception
   */
  public int execute(String command) throws Exception {
    return execute(command, true);
  }

}
