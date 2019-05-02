package org.riversun.promise;

import java.util.concurrent.CountDownLatch;

/**
 * Tests for Promise<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public class TestPromiseAsync extends TestPromiseTestCase {

    @Override
    public Thennable PromiseResolve(Object value) {
        return Promise.resolve(value);
    }

    @Override
    public Thennable PromiseReject(Object value) {
        return Promise.reject(value);
    }

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

}