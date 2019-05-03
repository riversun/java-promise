package org.riversun.promise;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

/**
 * Tests for Promise.all<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public class TestPromiseAllAsync extends TestPromiseAllTestCase {
    private CountDownLatch mLatch;

    public void sync(Integer... integers) {
        if (integers != null && integers.length > 0) {
            mLatch = new CountDownLatch(integers[0]);
        } else {
            mLatch = new CountDownLatch(1);
        }
    }

    public void consume() {
        mLatch.countDown();
    }

    public void await() {
        try {
            mLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Thennable PromiseAll(Thennable... promises) {
        return Promise.all(promises);
    }

    @Override
    public Thennable PromiseAll(Func... funcs) {
        return Promise.all(funcs);
    }

    @Override
    public Thennable PromiseResolve(Object data) {
        return Promise.resolve(data);
    }

    @Override
    public Thennable PromiseReject(Object data) {
        return Promise.reject(data);
    }

    /**
     * Test that all worker threads executed in promise.all are created from the same thread pool.
     */
    @Test
    public void test_promise_all_threads_is_from_the_same_thread() {
        sync();
        final List<String> threadNames = new ArrayList<String>();
        Func func1 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p1 = new Promise("1st", func1);

        Func func2 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p2 = new Promise("2nd", func2);

        Func func3 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p3 = new Promise("3rd", func3);

        Promise.all(
                p1, p2, p3)
                .then((action, data) -> {
                    final String myThread = Thread.currentThread().getName();
                    for (Object o : (List<Object>) data) {
                        threadNames.add((String) o);
                    }
                    threadNames.add(myThread);
                    action.resolve();
                    consume();
                })
                .start();
        await();

        // make sure all threads are from the same thread-pool
        for (int i = 0; i < threadNames.size() - 1; i++) {
            final String threadName = threadNames.get(i);
            final String threadPoolName = threadName.substring(0, threadName.lastIndexOf("-"));

            final String threadNameNext = threadNames.get(i + 1);
            final String threadPoolNameNext = threadName.substring(0, threadNameNext.lastIndexOf("-"));

            assertEquals(threadPoolName, threadPoolNameNext);
        }
    }

    // Original test for Promise
    /**
     * Make sure that user's executor is guaranteed to work.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_promise_all_specify_original_thread_pool() {

        // At least one worker thread to be used in Promise.all,
        // and one thread for overall asynchronous execution, so a total of two or more threads must be needed.
        final int NUM_OF_THREADS = 2;
        final ExecutorService myExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS);

        sync();
        final List<String> threadNames = new ArrayList<String>();
        Func func1 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p1 = new Promise("1st", func1);

        Func func2 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p2 = new Promise("2nd", func2);

        Func func3 = (action, data) -> {
            final String result = Thread.currentThread().getName();
            action.resolve(result);
        };
        Promise p3 = new Promise("3rd", func3);

        Promise.all(myExecutor,
                p1, p2, p3)
                .then((action, data) -> {
                    final String myThread = Thread.currentThread().getName();
                    for (Object o : (List<Object>) data) {
                        threadNames.add((String) o);
                    }
                    threadNames.add(myThread);
                    action.resolve();
                    // If you specify your own executor, you have to shut it down at the end
                    myExecutor.shutdown();
                    consume();
                })
                .start();
        await();

        // make sure all threads are from the same thread-pool
        for (int i = 0; i < threadNames.size() - 1; i++) {
            final String threadName = threadNames.get(i);
            final String threadPoolName = threadName.substring(0, threadName.lastIndexOf("-"));

            final String threadNameNext = threadNames.get(i + 1);
            final String threadPoolNameNext = threadName.substring(0, threadNameNext.lastIndexOf("-"));

            assertEquals(threadPoolName, threadPoolNameNext);
        }
    }
}