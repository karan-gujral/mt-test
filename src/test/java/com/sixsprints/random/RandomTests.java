package com.sixsprints.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class RandomTests {

  @Test
  public void asyncTestWithoutBugMustPass() throws InterruptedException {
    Random rand = new Random();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Thread.sleep(1000L);
      Thread t = new Thread(() -> {
        testMe(rand.nextInt(10));
      });
      threads.add(t);
      t.start();
    }
    waitForThreadsToFinish(threads);
  }

  @Test
  public void asyncTestWithBugMustFail() throws InterruptedException {
    Random rand = new Random();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Thread.sleep(1000L);
      Thread t = new Thread(() -> {
        testMeBuggy(rand.nextInt(10));
      });
      threads.add(t);
      t.start();
    }
    waitForThreadsToFinish(threads);
  }

  private void testMe(int n) {
    Random rand = new Random();
    List<Long> nums = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      nums.add((long) rand.nextInt(100));
    }
    Long result = new AggregationService().run(nums);
    Long assertResult = 0L;
    for (Long num : nums) {
      assertResult += num * 2;
    }
    System.out.println(result + ", " + assertResult);
    assertThat(result).isEqualTo(assertResult);
  }

  private void testMeBuggy(int n) {
    Random rand = new Random();
    List<Long> nums = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      nums.add((long) rand.nextInt(100));
    }
    Long result = new AggregationServiceWithBug().run(nums);
    Long assertResult = 0L;
    for (Long num : nums) {
      assertResult += num * 2;
    }
    System.out.println(result + ", " + assertResult);
    assertThat(result).isEqualTo(assertResult);
  }

  public static void waitForThreadsToFinish(List<Thread> threads) {
    try {
      for (Thread thread : threads) {
        thread.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
