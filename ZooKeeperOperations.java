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
 * The type Zoo keeper operations.
 */
public class ZooKeeperOperations extends ServiceOperations {

  private String binSuffix;
  private Config config = new Config();
  private final String ZOOKEEPER_PID = config.getAsUnixPath("temp.dir") + "/zookeeper.pid";

  /**
   * Instantiates a new Zoo keeper operations.
   *
   * @throws Exception the exception
   */
  public ZooKeeperOperations() throws Exception {

    startWorkDirPath = config.getAsUnixPath("kafka.bin.path");

    if (SystemUtils.IS_OS_WINDOWS) {
      startWorkDirPath += "/windows";
    }

    binSuffix = "sh";

    if (SystemUtils.IS_OS_WINDOWS) {
      binSuffix = "bat";
    }
  }

  public void start()
  {
    pidFilePath = ZOOKEEPER_PID;
    startExecutable = "/start-zookeeper." + binSuffix;
  }

  public void stop()
  {

    pidFilePath = ZOOKEEPER_PID;
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    return new ArrayList<Integer>();
  }
}
