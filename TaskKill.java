/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.SystemUtils;

/**
 * The type Task kill.
 */
public class TaskKill {

  private static final String LISTENING_STATE_NAME = "listening";

  private static final Logger logger = Logger.getLogger(TaskKill.class);

  /**
   * By port.
   *
   * @param port the port
   * @throws Exception the exception
   */
  public static void byPort(int port) throws Exception {
    List<String> socketStates = new ArrayList<String>();
    socketStates.add(LISTENING_STATE_NAME);
    killConnections("0.0.0.0", port, socketStates);
  }

  /**
   * By port.
   *
   * @param port        the port
   * @param socketState the socket state
   * @throws Exception the exception
   */
  public static void byPort(int port, String socketState) throws Exception {
    List<String> socketStates = new ArrayList<String>();
    socketStates.add(socketState);
    killConnections("", port, socketStates);
  }

  /**
   * Kill connections on loop back ip.
   *
   * @param port the port
   * @throws Exception the exception
   */
  public static void killConnectionsOnLoopBackIP(int port) throws Exception {
    List<String> socketStates = new ArrayList<String>();
    socketStates.add(LISTENING_STATE_NAME);
    socketStates.add("established");
    killConnections("127.0.0.1", port, socketStates);
  }

  /**
   * Kill connections on all interfaces.
   *
   * @param port the port
   * @throws Exception the exception
   */
  public static void killConnectionsOnAllInterfaces(int port) throws Exception {
    List<String> socketStates = new ArrayList<String>();
    socketStates.add(LISTENING_STATE_NAME);
    socketStates.add("established");
    socketStates.add("listen");
    socketStates.add("syn_sent");
    socketStates.add("syn_recv");
    socketStates.add("time_wait");
    socketStates.add("close_wait");
    socketStates.add("fin_wait_1");
    socketStates.add("fin_wait_2");
    socketStates.add("closing");
    killConnections("", port, socketStates);
  }

  /**
   * Kill connections.
   *
   * @param IP           the ip
   * @param port         the port
   * @param socketStates the socket states
   * @throws Exception the exception
   */
  public static void killConnections(String IP, int port, List<String> socketStates) throws Exception {
    String listCommand = null;

    if (SystemUtils.IS_OS_WINDOWS) {
      listCommand =
          "netstat -aon | findstr " + IP + ":" + String.valueOf(port) + " | findstr /i /R " + String.join("||",
                                                                                                          socketStates);
    } else if (SystemUtils.IS_OS_LINUX) {
      listCommand =
          "netstat -anptu | grep " + IP + ":" + String.valueOf(port) + " | grep -i " + "'" + String.join("\\|",
                                                                                                         socketStates)
              + "'";
    }

    CommandLineExecutor cmd = new CommandLineExecutor();
    cmd.setCommand(listCommand);
    cmd.setWaitFor(true);
    cmd.execute();
    String output = cmd.getOutput().trim();
    logger.info(String.format("netstat for process on %s:%s", IP, port));
    logger.info(String.format("%s", output));

    if (!output.isEmpty()) {
      String[] outputLines = output.split("\n");
      Pattern p = Pattern.compile(
          "\\s*(\\w*)"                                                    //Protocol
              + ".*?(\\d*\\[*[.:]*\\d*[.:]\\d*[.:]\\]*\\d*):(\\d+|\\*)+"  //Local address and port
              + "\\s*(\\d*\\[*[.:]*\\d*[.:]\\d*[.:]\\d*\\]*):(\\d+|\\*)"  //Foreign address and port
              + "\\s*((?i)[a-z_]*)"                                        //Socket state
              + "\\s*(\\d*)\\/*(\\w*)");                                  //PID (in case of linux os - PID/Process name

      for (String line : outputLines) {
        Matcher m = p.matcher(line.trim());
        if (m.matches()) {
          String localAddressPort = m.group(3);
          String portToKill = String.valueOf(port);
          String pid = m.group(7).trim();
          if (localAddressPort.equals(portToKill)) {
            killByPid(Integer.valueOf(pid));
          }
        } else {
          logger.error("Cannot kill process by port. Netstat output did not match the pattern " + line);
        }
      }
    }
  }

  /**
   * Kill by pid.
   *
   * @param pid the pid
   * @throws Exception the exception
   */
  public static void killByPid(int pid) throws Exception {
    String killCommand = null;

    if (SystemUtils.IS_OS_WINDOWS) {
      killCommand = "taskkill /T /F /PID ";
    } else if (SystemUtils.IS_OS_LINUX) {
      killCommand = "kill ";
    }

    CommandLineExecutor cmdKill = new CommandLineExecutor();
    cmdKill.setCommand(killCommand + String.valueOf(pid).trim());
    cmdKill.setWaitFor(true);
    cmdKill.execute();

    logger.info(String.format("Kill process with pid %s", pid));
    logger.info(String.format("%s", cmdKill.getOutput()));
  }


}
