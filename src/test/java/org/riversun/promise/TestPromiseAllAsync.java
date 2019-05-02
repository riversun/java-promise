package org.riversun.promise;

import java.util.concurrent.CountDownLatch;

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

}