package com.sixsprints.random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public class AggregationServiceWithBug extends AbstractAsyncServiceWithBug<Long, List> {

  @Override
  protected Callable<Long> primaryCallable(Map<String, Object> params) {
    return (() -> {
      int nextInt = new Random().nextInt(10);
      log.info("Sleeping for {}s", nextInt);
      Thread.sleep(nextInt * 1000L);
      return multiply(map(params, "val"));
    });
  }

  private Long multiply(Long val) {
    return val * 2;
  }

  @Override
  protected void handleError(Exception ex, Map<String, Object> params, String type) {
    System.out.println("ERROR!!");
  }

  @Override
  protected Long primaryOperation(Long provider, Long collector) {
    return provider += collector;
  }

  @Override
  protected List secondaryOperation(List provider, List collector) {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unchecked")
  public Long run(List<Long> nums) {
    CompletionService<Long> primaryExecutor = (CompletionService<Long>) cache().get(0);
    List<Future<Long>> futures = new ArrayList<>();
    Long result = 0L;
    for (Long num : nums) {
      Map<String, Object> map = new HashMap<>();
      put(map, "val", num);
      primaryThread(map, futures, primaryExecutor);
    }
    result = collectPrimary(result, futures, primaryExecutor);
    log.info("Done {}", result);
    return result;
  }

}
