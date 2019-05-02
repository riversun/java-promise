package org.riversun.promise;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Tests for promise.all<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
public abstract class TestPromiseAllTestCase {

    public abstract Thennable PromiseAll(Thennable... promises);

    public abstract Thennable PromiseAll(Func... funcs);

    public abstract Thennable PromiseResolve(Object data);

    public Thennable PromiseResolve() {
        return PromiseResolve(null);
    }

    public abstract Thennable PromiseReject(Object data);

    public Thennable PromiseReject() {
        return PromiseReject(null);
    }

    public abstract void sync(Integer... counter);

    public abstract void consume();

    public abstract void await();

    private static class ObjectHolder {
        public Object data;
    }

    /**
     * Test PromiseAll with empty arg
     */
    @Test
    public void test_promiseAll_empty_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(new Func[] {})
                .then((action, obj) -> {
                    holder.data = obj;
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals(null, holder.data);

    }

    /**
     * Test PromiseAll with empty arg
     */
    @Test
    public void test_promiseAllp_empty_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(new Promise[] {})
                .then((action, obj) -> {
                    holder.data = obj;
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals(null, holder.data);

    }

    /**
     * Test PromiseAll #1-1 typical sync chain
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_promiseAll_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    action.resolve("1");
                },
                (action, data) -> {
                    action.resolve("2");
                },
                (action, data) -> {
                    action.resolve("3");
                })
                        .then((Action action, Object data) -> {
                            holder.data = data;

                            action.resolve();
                            consume();
                        }).start();
        await();
        assertEquals(true, holder.data instanceof List);
        assertThat((List<Object>) holder.data, hasItems("1", "2", "3"));
        assertEquals((List<Object>) holder.data, Arrays.asList("1", "2", "3"));// check order

    }

    /**
     * Test PromiseAll #1-1 typical sync chain(null data)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_promiseAll_returns_null_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    action.resolve();
                },
                (action, data) -> {
                    action.resolve();
                },
                (action, data) -> {
                    action.resolve();
                })
                        .then((Action action, Object data) -> {
                            holder.data = data;
                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(true, holder.data instanceof List);
        assertEquals((List<Object>) holder.data, Arrays.asList(null, null, null));
    }

    /**
     * Test PromiseAll #2-1 thread-execution in the chain
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_promiseAll_with_thread_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    new Thread(() -> {
                        Promise.sleep(500);
                        action.resolve("1");
                    }).start();
                },
                (action, data) -> {
                    action.resolve("2");
                },
                (action, data) -> {
                    Promise.sleep(100);
                    action.resolve("3");
                })
                        .then((Action action, Object data) -> {
                            holder.data = data;

                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(true, holder.data instanceof List);
        assertThat((List<Object>) holder.data, hasItems("1", "2", "3"));
        assertEquals((List<Object>) holder.data, Arrays.asList("1", "2", "3"));// check order
    }

    /**
     * Test PromiseAll #2-2 thread-execution in the chain
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test_promiseAll_with_thread_full_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    new Thread(() -> {
                        Promise.sleep(500);
                        action.resolve("1");
                    }).start();
                },
                (action, data) -> {
                    new Thread(() -> {
                        Promise.sleep(1000);
                        action.resolve("2");
                    }).start();
                },
                (action, data) -> {
                    new Thread(() -> {
                        Promise.sleep(1500);
                        action.resolve("3");
                    }).start();
                })
                        .then((Action action, Object data) -> {
                            holder.data = data;
                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(true, holder.data instanceof List);
        assertThat((List<Object>) holder.data, hasItems("1", "2", "3"));
        assertEquals((List<Object>) holder.data, Arrays.asList("1", "2", "3"));// check order
    }

    /**
     * Test PromiseAll #3-1 test reject by action#reject
     */
    @Test
    public void test_promiseAll_with_rejection_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    Promise.sleep(500);
                    action.resolve("1");
                },
                (action, data) -> {
                    action.resolve("2");
                },
                (action, data) -> {
                    action.reject("ERROR");
                })
                        .then(null, (Action action, Object data) -> {
                            holder.data = data;

                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(false, holder.data instanceof List);
        assertEquals("ERROR", holder.data);
    }

    /**
     * Test PromiseAll #3-2 test reject by action#reject
     */
    @Test
    public void test_promiseAll_with_multi_rejection_chain() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    Promise.sleep(50);
                    action.reject("ERROR1");
                },
                (action, data) -> {
                    Promise.sleep(150);
                    action.reject("ERROR2");
                },
                (action, data) -> {
                    Promise.sleep(500);
                    action.reject("ERROR3");
                })
                        .then(null, (Action action, Object data) -> {
                            holder.data = data;
                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(false, holder.data instanceof List);
        assertEquals("ERROR1", holder.data);

    }

    /**
     * Test PromiseAll #4 test reject by exception
     */
    @Test
    public void test_promiseAll_with_rejection_by_exception() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseAll(
                (action, data) -> {
                    Promise.sleep(500);
                    action.resolve("1");
                },
                (action, data) -> {
                    throw new Exception("My Exception");
                },
                (action, data) -> {
                    action.resolve("3");
                })
                        .then(null, (Action action, Object data) -> {
                            holder.data = data;
                            action.resolve();
                            consume();
                        })
                        .start();
        await();
        assertEquals(true, holder.data instanceof Exception);
        assertEquals("My Exception", ((Exception) holder.data).getMessage());
    }

    /**
     * Test Promise.resolve
     */
    @Test
    public void test_promiseResolve() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseResolve().then(
                (action, data) -> {
                    holder.data = data;
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals(null, holder.data);
    }

    /**
     * Test Promise.resolve #2 check resolve(String)
     */
    @Test
    public void test_promiseResolve_check_arg_string() {
        sync();
        final ObjectHolder holder = new ObjectHolder();
        PromiseResolve("str").then(
                (action, data) -> {
                    holder.data = data;
                    action.resolve();
                    consume();
                })
                .start();
        await();
        assertEquals("str", holder.data);
    }

}