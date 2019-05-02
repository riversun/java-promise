package org.riversun.promise;

/**
 * Tests for SyncPromise.all<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public class TestPromiseAllSync extends TestPromiseAllTestCase {

    public void sync(Integer... integers) {
    }

    public void consume() {
    }

    public void await() {
    }

    @Override
    public Thennable PromiseAll(Thennable... promises) {
        return SyncPromise.all(promises);
    }

    @Override
    public Thennable PromiseAll(Func... funcs) {
        return SyncPromise.all(funcs);
    }

    @Override
    public Thennable PromiseResolve(Object... data) {
        return SyncPromise.resolve(data);
    }

    @Override
    public Thennable PromiseReject(Object... data) {
        return SyncPromise.reject(data);
    }

}