/*
 * Copyright (c) Experian, 2020. All rights reserved.
 */
package com.experian.automation.helpers;

import com.experian.automation.logger.Logger;
import java.util.concurrent.Callable;

/**
 * Retry mechanism conditioned to thrown Exception
 */
public class RetryExecutor {

  private final Logger logger = Logger.getLogger(this.getClass());

  // Number of trials
  private int retry = 3;

  // Thread sleep duration in milliseconds
  private long delay = 1000l;

  /**
   * The interface Runnable with exception.
   */
// Covering functional calls
  @FunctionalInterface
  public interface RunnableWithException {

    /**
     * Run.
     *
     * @throws Exception the exception
     */
    void run() throws Exception;
  }

  /**
   * Retry retry executor.
   *
   * @param times the times
   * @return the retry executor
   */
// Builder method to set retry
  public RetryExecutor retry(int times) {
    retry = times;
    return this;
  }

  /**
   * Delay retry executor.
   *
   * @param timeoutMillis the timeout millis
   * @return the retry executor
   */
// Builder method to set delay
  public RetryExecutor delay(long timeoutMillis) {
    delay = timeoutMillis;
    return this;
  }

  /**
   * Callable type of re-run, example of usage:
   * <p>
   * new RetryExecutor().execute(() -> {
   * <p>
   * Long count = 0; return count;
   * <p>
   * });
   *
   * @param <V>      Object type for return
   * @param callable callable typed object
   * @return return the typed object
   * @throws Exception the exception
   */
  public <V> V execute(Callable<V> callable) throws Exception {
    return rerun(callable, retry, delay);
  }

  /**
   * An option with no concern to returnable, example of usage:
   * <p>
   * new RetryExecutor().execute(() -> System.out.println("hey"));
   *
   * @param runnable @see FunctionalInterface
   * @throws Exception the exception
   */
  public void execute(RunnableWithException runnable) throws Exception {
    rerun(() -> {
      runnable.run();
      return null;
    }, retry, delay);
  }

  /**
   * Core logic to do retry with delays on the execution block
   *
   * @param callable @see Callable
   * @param retry Number of trials
   * @param delay Thread sleep duration in milliseconds
   * @param <T> Accepted type
   * @return
   * @throws Exception
   */
  private <T> T rerun(Callable<T> callable, int retry, long delay) throws Exception {
    int counter = 0;
    Throwable t = null;

    while (counter < retry) {
      try {
        return callable.call();
      } catch (Exception e) {
        t = e.fillInStackTrace();
        counter++;
        logger.warn(String.format("retry %s / %s", counter, retry));

        try {
          Thread.sleep(delay);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }

    throw new Exception("Exceeded limit of retry: " + counter + "/ " + retry, t);

  }
}
