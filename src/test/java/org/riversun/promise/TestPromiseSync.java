package org.riversun.promise;

/**
 * Tests for SyncPromise<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public class TestPromiseSync extends TestPromiseTestCase {

    @Override
    public Thennable PromiseResolve(Object value) {
        return SyncPromise.resolve(value);

    }

    @Override
    public Thennable PromiseReject(Object value) {
        return SyncPromise.reject(value);
    }

    @Override
    public void sync(Integer... integers) {
    }

    @Override
    public void consume() {
    }

    @Override
    public void await() {
    }

}