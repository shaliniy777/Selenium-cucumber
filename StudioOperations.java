/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers.powercurve;

import com.experian.automation.helpers.Config;
import com.experian.automation.helpers.ServiceOperations;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Studio operations.
 */
public class StudioOperations extends ServiceOperations {

  /**
   * Instantiates a new Studio operations.
   */
  public StudioOperations() {
    startWorkDirPath = Config.get("studio.path") + "/bin/";
  }

  @Override
  protected List<Integer> getServicePorts() {
    return new ArrayList<>();
  }

  @Override
  protected List<String> getServiceFilesToBackup() {
    ArrayList<String> filesToBackup = new ArrayList<>();
    filesToBackup.add("etc");
    filesToBackup.add("properties");

    return filesToBackup;
  }
}