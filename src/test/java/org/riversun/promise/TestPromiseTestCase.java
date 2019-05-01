package org.riversun.promise;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for promise<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public abstract class TestPromiseTestCase {

    public abstract Thennable PromiseResolve(Object... value);

    public abstract Thennable PromiseReject(Object... value);

    public abstract void sync(Integer... counter);

    public abstract void consume();

    public abstract void await();

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test Promise.resolve()
     */
    @Test
    public void test_promise_resolve_with_null() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve().then((action, data) -> {
            action.resolve();
        }).always((action, data) -> {
            if (data == null) {
                sb.append("data is null");
            }
            action.resolve();
            consume();
        }).start();
        await();
        assertEquals("data is null", sb.toString());
    }

    /**
     * Test Promise.resolve()
     */
    @Test
    public void test_promise_resolve_chain() {

        final StringBuilder sb = new StringBuilder();

        sync();

        PromiseResolve("0")
                .then((action, data) -> {
                    sb.append(data);
                    sb.append("1");
                    action.reject();
                })
                .always((action, data) -> {

                    action.resolve();
                    consume();
                }).start();

        await();
        assertEquals("01", sb.toString());
    }

    /**
     * Test Promise.reject()
     */
    @Test
    public void test_promise_reject_with_null() {
        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseReject()
                .then(null, (action, data) -> {
                    if (data == null) {
                        sb.append("data is null");
                    }
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("data is null", sb.toString());
    }

    /**
     * Test Promise.reject()
     */
    @Test
    public void test_promise_reject_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseReject("0")
                .then(null, (action, data) -> {
                    sb.append(data);
                    sb.append("1");
                    action.reject();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("01", sb.toString());

    }

    /**
     * Test to propagate resolve result
     */
    @Test
    public void test_resolve_result_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("0")
                .then((action, data) -> {
                    sb.append(data);
                    action.resolve("1");
                })
                .then((action, data) -> {
                    sb.append(data);
                    action.resolve("2");
                })
                .then((action, data) -> {
                    sb.append(data);
                    action.resolve();
                })
                .always((action, data) -> {

                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("012", sb.toString());

    }

    @Test
    public void test_reject_result_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseReject("0")
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.reject("1");
                })
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.reject("2");
                })
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.reject();
                })
                .always((action, data) -> {

                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("012", sb.toString());

    }

    /**
     * Make sure skip null of resolve func on then
     */
    @Test
    public void test_reject_result_skip_resolve_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseReject("0")
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.resolve("1");
                })
                .then((Func) null, (Func) null)
                .then((action, data) -> {
                    sb.append(data);
                    action.reject();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("01", sb.toString());

    }

    /**
     * Make sure skip null of reject func on then
     */
    @Test
    public void test_reject_result_skip_reject_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseReject("0")
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.reject("1");
                })
                .then((Func) null, (Func) null)
                .then(null, (action, data) -> {
                    sb.append(data);
                    action.reject();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("01", sb.toString());

    }

    /**
     * Test whether the handling of reject is done properly
     */
    @Test
    public void test_fail_chain() {

        sync();
        final StringBuilder sb = new StringBuilder();

        Thennable promiseResolve = PromiseResolve("start");

        promiseResolve.then((action, data) -> {
            sb.append("1");
            action.reject();
        }).then((action, data) -> {
            sb.append("2");
            action.resolve();
        }, (action, data) -> {
            sb.append("fail");
            action.resolve();
        }).then((action, data) -> {
            sb.append("3");
            action.resolve();
        }).always((action, data) -> {
            action.resolve();
            consume();
        })
                .start();
        await();
        assertEquals("1fail3", sb.toString());

    }

    /**
     * Test that treats exception as reject
     */
    @Test
    public void test_exception_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("start")
                .then((action, data) -> {
                    sb.append("1");
                    throw new Exception("ErrorCase");
                })
                .then((action, data) -> {
                    sb.append("2");
                    action.resolve();
                }, (action, error) -> {
                    Exception e = (Exception) error;
                    String errMessage = e.getMessage();
                    sb.append(errMessage);
                    action.resolve();
                })
                .then((action, data) -> {
                    sb.append("3");
                    action.resolve();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("1ErrorCase3", sb.toString());

    }

    /**
     * Test sync
     */
    @Test
    public void test_sync_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("start")
                .then((action, data) -> {
                    sb.append("1");
                    action.resolve();
                })
                .then((action, data) -> {
                    sb.append("2");
                    action.resolve();
                })
                .then((action, data) -> {
                    sb.append("3");
                    action.resolve();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("123", sb.toString());

    }

    /**
     * Test sync with sleep
     */
    @Test
    public void test_sync_with_sleep_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("start")
                .then((action, data) -> {
                    sb.append("1");
                    sleep(100);
                    action.resolve();
                })
                .then((action, data) -> {
                    sb.append("2");
                    sleep(100);
                    action.resolve();
                })
                .then((action, data) -> {
                    sb.append("3");
                    sleep(100);
                    action.resolve();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("123", sb.toString());

    }

    /**
     * Test to make sure that asynchronous operation is performed correct order.
     */
    @Test
    public void test_async_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("start")
                .then((action, data) -> {
                    new Thread(() -> {
                        sb.append("1");
                        sleep(100);
                        action.resolve();
                    }).start();

                })
                .then((action, data) -> {
                    new Thread(() -> {
                        sb.append("2");
                        sleep(100);
                        action.resolve();
                    }).start();
                })
                .then((action, data) -> {
                    new Thread(() -> {
                        sb.append("3");
                        sleep(100);
                        action.resolve();
                    }).start();
                })
                .always((action, data) -> {
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("123", sb.toString());

    }

    /**
     * Test to make sure that even sync and async are performed correct order.
     */
    @Test
    public void test_sync_async_chain() {

        final StringBuilder sb = new StringBuilder();
        sync();
        PromiseResolve("start")
                .then((action, data) -> {
                    new Thread(() -> {
                        sb.append("1");
                        action.resolve();
                    }).start();
                })
                .then((action, data) -> {
                    new Thread(() -> {
                        sb.append("2");
                        sleep(100);
                        action.resolve();
                    }).start();
                })
                .then((action, data) -> {
                    sb.append("3");
                    action.resolve();
                })
                .always((action, data) -> {
                    consume();
                    action.resolve();
                })
                .start();
        await();
        assertEquals("123", sb.toString());

    }

    /**
     * In the case of Promise, execute the same j asynchronously. In the case of SyncPromise, confirm the synchronous execution.
     */
    @Test
    public void test_concurrent_execution_chain() {

        final int concurrencyNumber = 10;
        sync(concurrencyNumber);
        StringBuilder[] sbs = new StringBuilder[concurrencyNumber];

        for (int i = 0; i < concurrencyNumber; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("(" + i + ")");
            sbs[i] = sb;
            PromiseResolve("start")
                    .then((action, data) -> {
                        new Thread(() -> {
                            sb.append("1");
                            action.resolve();
                        }).start();
                    })
                    .then((action, data) -> {
                        new Thread(() -> {
                            sb.append("2");
                            // sleep(10);
                            action.resolve();
                        }).start();
                    })
                    .then((action, data) -> {
                        sb.append("3");
                        action.resolve();
                    })
                    .always((action, data) -> {
                        action.resolve();
                        consume();
                    })
                    .start();
        }
        await();

        for (int i = 0; i < concurrencyNumber; i++) {
            assertEquals("(" + i + ")" + "123", sbs[i].toString());
        }

    }
}