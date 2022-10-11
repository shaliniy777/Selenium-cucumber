/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import java.util.HashMap;
import java.util.List;

/**
 * The type Context resource manager.
 */
public class ContextResourceManager {

  private static volatile HashMap<String, HashMap<String, Boolean>> resources = new HashMap<>();

  /**
   * Allocate string.
   *
   * @param contextName the context name
   * @return the string
   */
  public static synchronized final String allocate(String contextName) {

    if (resources.containsKey(contextName)) {
      HashMap<String, Boolean> contextResources = resources.get(contextName);

      for (String resourceName : contextResources.keySet()) {
        if (allocate(contextName, resourceName)) {
          return resourceName;
        }
      }
    }

    return null;
  }

  /**
   * Allocate boolean.
   *
   * @param contextName  the context name
   * @param resourceName the resource name
   * @return the boolean
   */
  public static synchronized final Boolean allocate(String contextName, String resourceName) {

    Boolean resourceIsAvalable = false;

    if (resources.containsKey(contextName)) {
      HashMap<String, Boolean> contextResources = resources.get(contextName);

      if (resources.containsKey(contextName)) {

        // Check if the resource is available
        resourceIsAvalable = contextResources.get(resourceName);

        if (resourceIsAvalable) {
          contextResources.put(resourceName, false);
        }
      }

    }

    return resourceIsAvalable;
  }

  /**
   * Release.
   *
   * @param contextName  the context name
   * @param resourceName the resource name
   */
  public static synchronized final void release(String contextName, String resourceName) {
    if (resources.containsKey(contextName) && resources.get(contextName).containsKey(resourceName)) {
      resources.get(contextName).put(resourceName, true);
    }
  }

  /**
   * Add.
   *
   * @param contextName   the context name
   * @param resourceNames the resource names
   */
  public static synchronized final void add(String contextName, List<String> resourceNames) {
    for (String resourceName : resourceNames) {
      add(contextName, resourceName);
    }
  }

  /**
   * Add.
   *
   * @param contextName  the context name
   * @param resourceName the resource name
   * @param unallocated  the unallocated
   */
  public static synchronized final void add(String contextName, String resourceName, Boolean unallocated) {

    if (!resources.containsKey(contextName)) {
      resources.put(contextName, new HashMap<String, Boolean>());
    }

    HashMap<String, Boolean> contextResources = resources.get(contextName);

    if (!contextResources.containsKey(resourceName)) {
      contextResources.put(resourceName, unallocated);
    }

  }

  /**
   * Add.
   *
   * @param contextName  the context name
   * @param resourceName the resource name
   */
  public static synchronized final void add(String contextName, String resourceName) {
    add(contextName, resourceName, true);
  }

  /**
   * Delete.
   *
   * @param contextName  the context name
   * @param resourceName the resource name
   */
  public static synchronized final void delete(String contextName, String resourceName) {

    if (resources.containsKey(contextName)) {

      HashMap<String, Boolean> contextResources = resources.get(contextName);

      if (contextResources.containsKey(resourceName)) {
        contextResources.remove(resourceName);
      }
    }
  }
}

