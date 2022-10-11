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
 * The type Kafka console consumer operations.
 */
public class KafkaConsoleConsumerOperations extends ServiceOperations {

  private String binSuffix;
  private Config config = new Config();
  private final String CONSOLE_CONSUMMER_PID = config.getAsUnixPath("temp.dir") + "/kafka-console-consumer.pid";

  /**
   * Instantiates a new Kafka console consumer operations.
   *
   * @throws Exception the exception
   */
  public KafkaConsoleConsumerOperations() throws Exception {

    startWorkDirPath = config.getAsUnixPath("kafka.bin.path");

    if (SystemUtils.IS_OS_WINDOWS) {
      startWorkDirPath += "/windows";
    }

    binSuffix = "sh";

    if (SystemUtils.IS_OS_WINDOWS) {
      binSuffix = "bat";
    }
  }


  /**
   * Start.
   *
   * @param server          the server
   * @param topic           the topic
   * @param messageFilePath the message file path
   */
  public void start(String server, String topic, String messageFilePath) {
    pidFilePath = CONSOLE_CONSUMMER_PID;

    startExecutable = startWorkDirPath +
        "/kafka-console-consumer." + binSuffix +
        " --bootstrap-server " + server +
        " --topic " + topic +
        " > " + messageFilePath;
  }

  public void stop()
  {

    pidFilePath = CONSOLE_CONSUMMER_PID;
  }

  @Override
  protected List<Integer> getServicePorts() throws Exception {
    return new ArrayList<Integer>();
  }
}
