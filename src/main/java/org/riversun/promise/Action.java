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

/**
 * Action object is an argument of Func#run method.
 * 
 * Call action.resolve( [fulfillment value] ) to make the Promise's status fulfilled and move
 * on to the next processing(specified by then) with the result(fulfillment value).
 * <code>action.resolve("Success");</code>
 * 
 * Call action.reject( [rejection reason] ) to make the Promise's status rejected and move on to the next processing(specified by then) with the
 * result(rejection reason).
 * <code>action.reject("Failure");</code>
 * 
 * Argument is optional, you can call action.resolve() or action.reject()
 * <code>action.resolve();//Argument can be omitted</code>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public interface Action {

    /**
     * To make the Promise's status fulfilled and move on to the next processing(specified by then) with the result(fulfillment value).
     * 
     * @param result
     */
    public void resolve(Object result);

    /**
     * To make the Promise's status fulfilled and move on to the next processing(specified by then) with null result
     * 
     * @param result
     */
    public void resolve();

    /**
     * To make the Promise's status rejected and move on to the next processing(specified by then) with reason
     * 
     * @param result
     */
    public void reject(Object reason);

    /**
     * To make the Promise's status rejected and move on to the next processing(specified by then) with null reason
     * 
     * @param result
     */
    public void reject();

}
