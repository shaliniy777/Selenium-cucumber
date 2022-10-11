/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The type Staf executor.
 */
public class STAFExecutor {

  private STAFResult result;

  /**
   * Execute int.
   *
   * @param host    the host
   * @param service the service
   * @param command the command
   * @return the int
   * @throws Exception the exception
   */
  public int execute(String host, String service, String command) throws Exception {

    STAFHandle handle = new STAFHandle("Test");

    result = handle.submit2(host, service, command);

    if (result.resultObj.getClass().equals(HashMap.class) && ((HashMap) result.resultObj).get("rc") != null) {
      return Integer.parseInt(((HashMap) result.resultObj).get("rc").toString());
    }

    return result.rc;
  }

  /**
   * Gets result.
   *
   * @return the result
   * @throws Exception the exception
   */
  public String getResult() throws Exception {

    return result.result;
  }

  /**
   * Gets result list.
   *
   * @return the result list
   * @throws Exception the exception
   */
  public LinkedList getResultList() throws Exception {
    return LinkedList.class.cast(result.resultObj);
  }
}