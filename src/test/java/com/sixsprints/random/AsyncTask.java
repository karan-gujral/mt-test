package com.sixsprints.random;

import java.util.Random;
import java.util.concurrent.Callable;

public class AsyncTask implements Callable<String> {

  private long i;

  public AsyncTask(long i) {
    this.i = i;
  }

  @Override
  public String call() throws Exception {
    try {
      int nextInt = new Random().nextInt(10);
      Thread.sleep(nextInt * 1000L);
      return "done: " + i + ", " + nextInt * 1000L;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "oops";
  }

}
