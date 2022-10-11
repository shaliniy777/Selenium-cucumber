/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

/**
 * The type Remote file.
 */
public class RemoteFile {
  private String host;
  private String path;

  /**
   * Instantiates a new Remote file.
   *
   * @param host the host
   * @param path the path
   */
  public RemoteFile(String host, String path){
    this.host = host;
    this.path = path;
  }

  /**
   * Get host string.
   *
   * @return the string
   */
  public String getHost(){
    return host;
  }

  /**
   * Get path string.
   *
   * @return the string
   */
  public String getPath(){
    return path;
  }
}
