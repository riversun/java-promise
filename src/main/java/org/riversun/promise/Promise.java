/*
 * Copyright (c) 2018-2019 Tom Misawa(riversun.org@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */
package org.riversun.promise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * You can chain asynchronous operations and execute them sequentially
 * by using Promise.
 * 
 * Of course, not only asynchronous operations but also synchronous operations
 * can be chained.
 *
 * Example Code
 * <code>
    Promise.resolve()
        .then((action, data) -> {
            new Thread(() -> {
                // Do something
                System.out.println("process-1");
                action.resolve("result-1");
            }).start();
        })
        .then((action, data) -> {
            new Thread(() -> {
                // Do something
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                System.out.println("process-2");
                action.resolve("result-1");
            }).start();

        })
        .then((action, data) -> {
            new Thread(() -> {
                // Do something
                System.out.println("process-3");
                action.resolve("result-1");
            }).start();

        });
        
    Result in:
    process-1
    process-2
    process-3

 *</code>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class Promise implements Thennable {

    private Status mStatus;

    public String mName = "";
    private ExecutorService mExecutor = null;
    private Promise mFounder = null;
    private Promise mParentPromise = null;

    private Promise mPreviousPromise;
    private Promise mNextPromise;

    private Promise mOnFulfilled = null;
    private Promise mOnRejected = null;

    private Object mResult;
    private Func mFunc;

    public Promise() {
        mStatus = Status.PENDING;
    }

    public Promise(String name, Func func) {
        this();
        this.mName = name;
        this.mFunc = func;
    }

    public Promise(Func func) {
        this();
        this.mName = this.toString();
        this.mFunc = func;
    }

    public ExecutorService createExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Specify the operation to be performed after the promise processing.
     * In this method,you can specify only "Func" instead of doing "new
     * Promise(func)".You may omit "new Promise(func)".
     * 
     * @param onFulfilled,[onRejected]
     * @return Promise
     */
    @Override
    public Promise then(Func... funcs) {
        final List<Promise> promiseList = new ArrayList<Promise>();
        if (funcs != null) {
            for (Func func : funcs) {
                final Promise promise;
                if (func == null) {
                    promise = null;
                } else {
                    promise = new Promise(func);
                }
                promiseList.add(promise);
            }
        }
        return then(promiseList.toArray(new Promise[0]));
    }

    @Override
    public Promise always(Thennable promise) {
        return then(promise, promise);
    }

    @Override
    public Promise always(Func func) {
        final Promise promise = new Promise(func);
        return always(promise);
    }

    @Override
    public Promise then(Thennable... promises) {

        Thennable onFulfilled = null;
        Thennable onRejected = null;

        if (promises != null && promises.length > 0) {
            onFulfilled = promises[0];
            if (promises.length > 1) {
                onRejected = promises[1];
            }
        }
        // Create executor at first access
        if (mExecutor == null) {
            mExecutor = createExecutor();
        }

        // Remember "ancestor" promise at first access
        if (mFounder == null) {
            mFounder = Promise.this;
        }

        mNextPromise = createNextPromise("***(next of " + mName + ")", (Promise) onFulfilled, (Promise) onRejected);

        // Warning:If you don't "ignite" after all "#then"s called, an inconsistency will occur.
        // Do not call ignite before returning all mNextPromise by all of "#then"s
        // So I added the "Promise#start" method to start reliably after calling all "then".
        // ignite();//<=bad practice

        return mNextPromise;
    }

    @Override
    public Promise start() {
        mFounder.ignite();
        return Promise.this;
    }

    /**
     * Run first procedure
     */
    private void ignite() {

        if (mPreviousPromise == null) {
            // first "then" call
            runOnThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        doNext(mNextPromise, mResult);
                    } catch (Exception e) {
                        if (mExecutor != null) {
                            mExecutor.shutdown();
                        }
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void invokeFunction(final Object previousPromiseResult) {

        try {

            Promise.this.mFunc.run(new Action() {
                @Override
                public void resolve(Object... result) {
                    mStatus = Status.FULFILLED;
                    onFinish(result);
                }

                @Override
                public void reject(Object... result) {
                    mStatus = Status.REJECTED;
                    onFinish(result);
                }
            }, previousPromiseResult);

        } catch (Exception e) {
            // e.printStackTrace();
            mStatus = Status.REJECTED;
            onFinish(e);

        }

    }

    private void onFinish(Object... result) {

        final Object crrResult;

        if (result != null && result.length > 0) {
            crrResult = result[0];
        } else {
            crrResult = null;
        }

        final Promise nextPromise = mParentPromise.mNextPromise;

        if (nextPromise != null) {
            doNext(nextPromise, crrResult);
        } else {
            // means "nextPromise == null"

            // Since there is no next promise,it means that the execution is the last here.
            // So shut down the executor
            mParentPromise.mExecutor.shutdown();
        }
    }

    private void doNext(Promise nextPromise, Object crrResult) {

        switch (mStatus) {
        case FULFILLED:

            if (nextPromise.mOnFulfilled == null) {
                // Skip if resolve is not explicitly set
                final Promise skipPromise = new Promise("skipper(fulfilled)", new Func() {
                    @Override
                    public void run(Action action, Object data) {
                        // skip
                        action.resolve(data);
                    }
                });
                // If there is no next promise to receive the "fulfilled" of the previous promise,
                // Also, move to the next to continue "then" chain.
                nextPromise.mOnFulfilled = skipPromise;
                nextPromise.mOnFulfilled.populateParentPromise(nextPromise);
            }

            try {
                nextPromise.mOnFulfilled.invokeFunction(crrResult);
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        case REJECTED:
            if (nextPromise.mOnRejected == null) {
                // Skip if reject is not explicitly set
                final Promise skipPromise = new Promise("skipper(rejected)", new Func() {
                    @Override
                    public void run(Action action, Object data) {
                        // skip
                        action.reject(data);
                    }
                });
                // If there is no next promise to receive the "rejected" of the previous promise,
                // Also, move to the next to continue "then" chain.
                nextPromise.mOnRejected = skipPromise;
                nextPromise.mOnRejected.populateParentPromise(nextPromise);
            }

            nextPromise.mOnRejected.invokeFunction(crrResult);
            break;
        case PENDING:
            throw new RuntimeException("Cannot proceed operation with PENDING promise, please call Promise.resolve to start chain.");
        }
    }

    private Promise createNextPromise(String promiseName, Promise onFulfilled, Promise onRejected) {

        final Promise nextPromise = new Promise(promiseName, null);

        nextPromise.mExecutor = Promise.this.mExecutor;
        nextPromise.mFounder = Promise.this.mFounder;
        nextPromise.mPreviousPromise = Promise.this;

        if (onFulfilled != null) {
            nextPromise.mOnFulfilled = onFulfilled;
            nextPromise.mOnFulfilled.populateParentPromise(nextPromise);
        }

        if (onRejected != null) {
            nextPromise.mOnRejected = onRejected;
            nextPromise.mOnRejected.populateParentPromise(nextPromise);
        }
        return nextPromise;
    }

    private void populateParentPromise(Promise parentPromise) {
        mParentPromise = parentPromise;
        mPreviousPromise = parentPromise.mPreviousPromise;
        mExecutor = parentPromise.mExecutor;
        mFounder = parentPromise.mFounder;
    }

    public void runOnThread(Runnable r) {
        mExecutor.submit(r);
    }

    @Override
    public Status getStatus() {
        return mStatus;
    }

    @Override
    public Object getValue() {
        return mResult;
    }

    /**
     * Returns a Promise object that is resolved with a given value
     * 
     * @param data
     * @return
     */
    public static Promise resolve(Object... data) {
        final Promise promise = new Promise();
        promise.mStatus = Status.FULFILLED;
        if (data != null && data.length > 0) {
            promise.mResult = data[0];
        }
        return promise;
    }

 

    /**
     * Returns a Promise object that is rejected with a given reason.
     * 
     * @param reason
     * @return
     */
    public static Promise reject(Object... reason) {
        final Promise promise = new Promise();
        promise.mStatus = Status.REJECTED;
        if (reason != null && reason.length > 0) {
            promise.mResult = reason[0];
        }
        return promise;
    }

    private static final class Holder {
        public Object result;
        public boolean rejected = false;
    }

    /**
     * Promise.all waits for all fulfillments (or the first rejection).
     * 
     * Promise.all is rejected if any of the elements are rejected.
     * For example,
     * if you pass in four promises that resolve after a sleep and one promise
     * that rejects immediately, then Promise.all will reject immediately.
     * 
     * Since each promise is executed on its own worker thread, the
     * execution of the promise itself continues on the worker thread.
     * But, once reject received, Promise.all will move on to then when it receives
     * reject even if the worker thread is moving
     * 
     * If fulfilled, all results are returned as "List<Object>" at
     * {@link Func#run(Action, List<Object>)} method.
     * 
     * If rejected, only rejected promise results will be returned as "Error" at
     * {@link Func#run(Action, Error)} method.
     * 
     * @param promises
     * @return
     * 
     */
    public static Promise all(Thennable... promises) {
        if (promises == null || promises.length == 0) {
            // If an empty iterable is passed, then this method returns an
            // already resolved promise.
            return Promise.resolve();
        }

        final List<Object> resultList = new ArrayList<Object>();
        final List<Holder> resultHolderList = new ArrayList<Holder>();

        final Func func = new Func() {

            @Override
            public void run(Action _action, Object data) throws Exception {

                final CountDownLatch latch = new CountDownLatch(promises.length);

                final Holder rejectedHolder = new Holder();

                for (Thennable promise : promises) {

                    final Holder resultHolder = new Holder();
                    resultHolderList.add(resultHolder);

                    Promise.resolve().then(promise).then(
                            // fulfilled
                            new Func() {
                                @Override
                                public void run(Action action, Object data) throws Exception {
                                    resultHolder.result = data;
                                    action.resolve();
                                    latch.countDown();
                                }
                            },
                            // rejected
                            new Func() {
                                @Override
                                public void run(Action action, Object data) throws Exception {
                                    rejectedHolder.rejected = true;
                                    rejectedHolder.result = data;
                                    resultHolder.result = data;
                                    action.resolve();

                                    // Countdown latches to cancel to move forward even if there is something else thread running
                                    for (int i = 0; i < promises.length; i++) {
                                        latch.countDown();
                                    }
                                }
                            }).start();
                }

                latch.await();

                final boolean isRejected = rejectedHolder.rejected;
                final Object rejectedResultObject = rejectedHolder.result;

                for (Holder holder : resultHolderList) {
                    resultList.add(holder.result);
                }

                if (isRejected) {
                    _action.reject(rejectedResultObject);
                } else {
                    _action.resolve(resultList);
                }
            }
        };

        return Promise.resolve().then(func);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Promise.all waits for all fulfillments (or the first rejection).
     * 
     * Promise.all is rejected if any of the elements are rejected.
     * For example,
     * if you pass in four funcs that resolve after a sleep and one func
     * that rejects immediately, then Promise.all will reject immediately.
     * 
     * Since each func is executed on its own worker thread, the
     * execution of the func itself continues on the worker thread.
     * But, once reject received, Promise.all will move on to then when it receives
     * reject even if the worker thread is moving
     * 
     * If fulfilled, all results are returned as "List<Object>" at
     * {@link Func#run(Action, List<Object>)} method.
     * 
     * If rejected, only rejected func results will be returned as "Error" at
     * {@link Func#run(Action, Error)} method.
     * 
     * @param funcs
     * @return
     * 
     */
    public static Promise all(Func... funcs) {
        if (funcs == null || funcs.length == 0) {
            return Promise.resolve();
        }

        final List<Promise> promiseList = new ArrayList<Promise>();
        if (funcs != null) {

            for (Func func : funcs) {
                final Promise promise;
                if (func == null) {
                    promise = null;
                } else {
                    promise = new Promise(func);
                }

                promiseList.add(promise);
            }
        }
        return Promise.all(promiseList.toArray(new Promise[0]));
    }

}
