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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

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
	SyncPromise.resolve()
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
public class SyncPromise implements Thennable {

    private final Func mFunc;

    private Status mStatus = Status.PENDING;
    private Object mResult;
    private SyncPromise mNextPromise;

    public SyncPromise() {
        mFunc = null;
    }

    /**
     * 
     * Unlike javascript promises, even if executor is passed,
     * it will NOT be executed immediately.
     * 
     * If you want to start a series of execution-flows using Promise,
     * USE "Promise.resolve().then([YOUR PROMISE])" or
     * "Promise.reject().then({YOUR_PROMISE)".
     * 
     * @param executor
     */
    public SyncPromise(Func executor) {
        mFunc = executor;
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
    public SyncPromise then(Func... funcs) {
        final List<SyncPromise> promiseList = new ArrayList<SyncPromise>();
        if (funcs != null) {
            for (Func func : funcs) {
                SyncPromise promise = new SyncPromise(func);
                promiseList.add(promise);
            }
        }
        return then(promiseList.toArray(new SyncPromise[0]));
    }

    /**
     * Specify the operation to be performed after the promise processing.
     * 
     * @param promises
     * @return
     */
    @Override
    public SyncPromise then(Thennable... promises) {
        Thennable onFulfilled = null;
        Thennable onRejected = null;

        if (promises != null && promises.length > 0) {

            onFulfilled = promises[0];
            if (promises.length > 1) {
                onRejected = promises[1];
            }
        } else {
            throw new RuntimeException("Please set  at least one Promise.");
        }

        if (this.mStatus == Status.FULFILLED) {
            mNextPromise = (SyncPromise) onFulfilled;

            if (mNextPromise != null && mNextPromise.mFunc != null) {
                this.invokeFunction(mNextPromise.mFunc, this.mResult);
            } else {
                // Skip if resolve is not explicitly set
                final SyncPromise skipPromise = new SyncPromise(new Func() {
                    @Override
                    public void run(Action action, Object data) {
                        action.resolve(data);
                    }
                });
                mNextPromise = skipPromise;
                this.invokeFunction(mNextPromise.mFunc, this.mResult);
                return mNextPromise;
            }
        }

        if (this.mStatus == Status.REJECTED) {
            mNextPromise = (SyncPromise) onRejected;
            if (mNextPromise != null && mNextPromise.mFunc != null) {
                this.invokeFunction(mNextPromise.mFunc, this.mResult);
            } else {

                // Following should only be displayed if there was no then to handle rejects at the very end
                // System.err.println("Unhandled promise rejection.Please handle rejection with #then(,[rejection-handler])");

                // Skip if reject function is not explicitly set
                final SyncPromise skipPromise = new SyncPromise(new Func() {
                    @Override
                    public void run(Action action, Object data) {
                        action.reject(data);
                    }
                });
                mNextPromise = skipPromise;
                this.invokeFunction(mNextPromise.mFunc, this.mResult);
            }
        }

        return mNextPromise;
    }

    @Override
    public SyncPromise always(Func func) {
        final SyncPromise promise = new SyncPromise(func);
        return always(promise);
    }

    @Override
    public SyncPromise always(Thennable promise) {
        return then(promise, promise);
    }

    @Override
    public SyncPromise start() {
        return SyncPromise.this;
    }

    /**
     * Invoke function object
     * 
     * @param func
     * @param previousPromiseResult
     */
    private void invokeFunction(Func func, Object previousPromiseResult) {

        // Semaphore for blocking func execution until resolve or reject is called
        final Semaphore semaphore = new Semaphore(0);

        try {
            func.run(new Action() {
                @Override
                public void resolve(Object... result) {
                    if (result != null && result.length > 0) {
                        mNextPromise.mResult = result[0];
                    }
                    mNextPromise.mStatus = Status.FULFILLED;

                    // Releases a permit, returning it to the semaphore.
                    semaphore.release();
                }

                @Override
                public void reject(Object... result) {
                    if (result != null && result.length > 0) {
                        mNextPromise.mResult = result[0];
                    }
                    mNextPromise.mStatus = Status.REJECTED;

                    // Releases a permit, returning it to the semaphore.
                    semaphore.release();
                }
            }, previousPromiseResult);
        } catch (Exception e) {
            // Exception is treated as reject
            mNextPromise.mResult = e;
            mNextPromise.mStatus = Status.REJECTED;

            // Releases a permit, returning it to the semaphore.
            semaphore.release();
        }

        try {
            // Use semaphores to wait for resolve or reject execution
            // Acquires a permit from this semaphore, blocking until semaphore is available,
            // or the thread is interrupted.
            semaphore.acquire();
        } catch (InterruptedException e) {
            // Interruption is treated as reject
            e.printStackTrace();
            mNextPromise.mResult = null;
            // mNextPromise.mIsRejected = true;
            mNextPromise.mStatus = Status.REJECTED;
        }

    }

    public Status getStatus() {
        return mStatus;
    }

    public Object getValue() {
        return mResult;
    }

    /**
     * Returns a Promise object that is resolved with a given value
     * 
     * @param data
     * @return
     */
    public static SyncPromise resolve(Object... data) {
        final SyncPromise promise = new SyncPromise();
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
    public static SyncPromise reject(Object... reason) {
        final SyncPromise promise = new SyncPromise();
        promise.mStatus = Status.REJECTED;
        if (reason != null && reason.length > 0) {
            promise.mResult = reason[0];
        }
        return promise;
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
    public static SyncPromise all(Thennable... promises) {

        if (promises == null || promises.length == 0) {
            // If an empty iterable is passed, then this method returns an
            // already resolved promise.
            return SyncPromise.resolve();
        }

        final ExecutorService executor = Executors.newCachedThreadPool();

        final List<Future<SyncPromise>> futureList = new ArrayList<Future<SyncPromise>>();

        final List<Object> resultList = new ArrayList<Object>();

        try {// Make sure executor.shutdown is called at "try-finally".

            for (Thennable promise : promises) {

                final Callable<SyncPromise> callable = new Callable<SyncPromise>() {

                    @Override
                    public SyncPromise call() throws Exception {
                        SyncPromise result = SyncPromise.resolve().then(promise);
                        if (result.getStatus() == Status.REJECTED) {
                            // throw exception to interrupt
                            throw new PromiseException(result.getValue());
                        }
                        return result;
                    }
                };

                final Future<SyncPromise> future = executor.submit(callable);
                futureList.add(future);
            }
        } finally {
            executor.shutdown();
        }

        boolean rejected = false;
        Object rejectedError = null;

        for (Future<SyncPromise> f : futureList) {
            try {
                SyncPromise result = f.get();
                resultList.add(result.getValue());
            } catch (InterruptedException e) {
                // exit loop
                break;
            } catch (ExecutionException e) {

                final Throwable cause = e.getCause();

                if (cause instanceof PromiseException) {
                    PromiseException pe = (PromiseException) cause;
                    rejectedError = pe.getValue();
                    rejected = true;
                }
                // exit loop
                break;
            }
        }

        if (rejected) {
            return SyncPromise.reject(rejectedError);
        } else {
            return SyncPromise.resolve(resultList);
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
    public static SyncPromise all(Func... funcs) {
        if (funcs == null || funcs.length == 0) {
            return SyncPromise.resolve();
        }

        final List<SyncPromise> promiseList = new ArrayList<SyncPromise>();
        if (funcs != null) {
            for (Func func : funcs) {
                SyncPromise promise = new SyncPromise(func);
                promiseList.add(promise);
            }
        }
        return SyncPromise.all(promiseList.toArray(new SyncPromise[0]));
    }

    public static void sleep(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}