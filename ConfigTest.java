package com.experian.automation.helpers;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigTest {

  private static final String expectedUnixPath =
      System.getProperty("user.dir").replaceAll("\\\\", "/") + "/features";

  private static final String expectedWindowsPath =
      System.getProperty("user.dir").replaceAll("/", "\\") + "\\features";

  private static final String expectedSystemPath =
      System.getProperty("user.dir") + File.separator + "features";
  @Test
  void getAsUnixPath() {

    Assert.assertEquals(
        Config.getAsUnixPath("features.path"),
        expectedUnixPath);
  }

  @Test
  void getAsWindowsPath() {
    Assert.assertEquals(
        Config.getAsWindowsPath("features.path"),
        expectedWindowsPath);
  }

  @Test
  void getAsSystemPath() {
    Assert.assertEquals(
        Config.getAsSystemPath("features.path"),
        expectedSystemPath
    );
  }

  @Test
  void normalGet() {
    Assert.assertEquals(Config.get("browser"), "ie");
  }

}