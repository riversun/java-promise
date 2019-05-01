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
 * Func is a java interface equivalent to JavaScript's Function for argument of #then
 * 
 * You can write Func like a JavaScript function.
 * I want to show two ways of implementing Func class.
 * 
 * No.1)Write Func object in the normal way.
 * <code>
Func function = new Func() {
    public void run(Action action, Object data) throws Exception {
        System.out.println("Process");//write your logic
        action.resolve();
    }
};
</code>
 * 
 * No.2)Write Func object using lambda expression.
 * <code>
Func function = (action, data) -> {
    System.out.println("Process");//write your logic
    action.resolve();
};
</code>
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public interface Func {
    public void run(Action action, Object data) throws Exception;
}
