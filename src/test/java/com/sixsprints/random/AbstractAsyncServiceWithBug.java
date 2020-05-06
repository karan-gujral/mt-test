package com.sixsprints.random;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAsyncServiceWithBug<PT, ST> {

  private static Map<Long, List<CompletionService<?>>> threadData = new ConcurrentHashMap<>();

  public List<CompletionService<?>> cache() {
    log.info("Cache called {}", threadData.get(-1L));
    if (threadData.get(-1L) == null || threadData.get(-1L).isEmpty()) {
      threadData.put(-1L, ImmutableList.<CompletionService<?>>of(
        new ExecutorCompletionService<PT>(Executors.newFixedThreadPool(4)),
        new ExecutorCompletionService<ST>(Executors.newFixedThreadPool(4))));
    }
    return threadData.get(-1L);
  }

  public void putInCache(List<CompletionService<?>> executors) {
    if (threadData.get(-1L) == null) {
      threadData.put(-1L, executors);
    }
  }

  /**
   * Helper method to put and object in a map only if it is not null
   * 
   * @param map
   * @param key
   * @param obj
   */
  public static void put(Map<String, Object> map, String key, Object obj) {
    if (key == null || obj == null) {
      return;
    }
    map.put(key, obj);
  }

  /**
   * Helper method to get and type cast a value from a map
   * 
   * @param <UDT>
   * @param params
   * @param key
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <UDT> UDT map(Map<String, Object> params, String key) {
    return params.get(key) == null ? null : (UDT) params.get(key);
  }

  /**
   * The Task that will run as a part of the primary thread. Please note the map
   * params need to be deserialized to call the task.
   * 
   * @param params
   * @return
   */
  protected abstract Callable<PT> primaryCallable(Map<String, Object> params);

  /**
   * First level of Aync Task.
   * 
   * @param params
   * @param futures
   * @param primaryExecutor The CompletionService executor that will run on the
   *                        primary thread
   */
  protected void primaryThread(Map<String, Object> params, List<Future<PT>> futures,
    CompletionService<PT> primaryExecutor) {
    try {
      log.info("Submitted {} thread with params {}", "PRIMARY", params);
      futures.add(primaryExecutor.submit(primaryCallable(params)));
    } catch (Exception ex) {
      handleError(ex, params, "PRIMARY");
    }
  }

  /**
   * Collects the response from the primary task and adds it to the collection
   * passed.
   * 
   * @param collection
   * @param futures
   * @param primaryExecutor The CompletionService executor that will run on the
   *                        primary thread
   */
  protected PT collectPrimary(PT collection, List<Future<PT>> futures, CompletionService<PT> primaryExecutor) {
    int count = 0;
    while (count < futures.size()) {
      try {
        collection = primaryOperation(primaryExecutor.take().get(), collection);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        count++;
      }
    }
    return collection;
  }

  /**
   * The Task that will run as a part of the secondary thread. Please note the map
   * params need to be deserialized to call the task.
   * 
   * @param params
   * @return
   */
  protected Callable<ST> secondaryCallable(Map<String, Object> params) {
    return null;
  }

  /**
   * Second level of Async Task.
   * 
   * @param params
   * @param futures
   * @param secondaryExecutor The CompletionService executor that will run on the
   *                          secondary thread
   */
  protected void secondaryThread(Map<String, Object> params, List<Future<ST>> futures,
    CompletionService<ST> secondaryExecutor) {
    if (secondaryExecutor == null) {
      return;
    }
    try {
      log.info("Submitted {} thread with params {}", "SECONDARY", params);
      futures.add(secondaryExecutor.submit(secondaryCallable(params)));
    } catch (Exception ex) {
      handleError(ex, params, "SECONDARY");
    }
  }

  /**
   * Collects the response from the secondary task and adds it to the collection
   * passed.
   * 
   * @param collection
   * @param futures
   * @param secondaryExecutor The CompletionService executor that will run on the
   *                          secondary thread
   */
  protected void collectSecondary(ST collection, List<Future<ST>> futures, CompletionService<ST> secondaryExecutor) {
    int count = 0;
    while (count < futures.size()) {
      try {
        collection = secondaryOperation(secondaryExecutor.take().get(), collection);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        count++;
      }
    }
  }

  /**
   * Can be used to handle the errors that get thrown during the execution of the
   * Async task.
   * 
   * @param ex
   * @param params - the map initially passed to the task
   * @param type   - defined as either PRIMARY or SECONDARY
   */
  protected abstract void handleError(Exception ex, Map<String, Object> params, String type);

  protected abstract PT primaryOperation(PT provider, PT collector);

  protected abstract ST secondaryOperation(ST provider, ST collector);

}
