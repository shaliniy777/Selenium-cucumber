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
 * The type Kafka operations.
 */
public class KafkaOperations extends ServiceOperations {

  private String binSuffix;
  private Config config = new Config();
  private final String KAFKA_PID = config.getAsUnixPath("temp.dir") + "/kafka.pid";

  /**
   * Instantiates a new Kafka operations.
   *
   * @throws Exception the exception
   */
  public KafkaOperations() throws Exception {

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
    pidFilePath = KAFKA_PID;
    startExecutable = "/start-kafka." + binSuffix;
  }

  public void stop()
  {

    pidFilePath = KAFKA_PID;
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    return new ArrayList<Integer>();
  }
}
