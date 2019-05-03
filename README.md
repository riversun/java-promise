# Overview
**java-promise** is a Promise Library for Java.

- You can easily control asynchronous operations like JavaScript's **[Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise)**.
- Supports both synchronous and asynchronous execution.

It is licensed under [MIT](https://opensource.org/licenses/MIT).

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.riversun/java-promise/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.riversun/java-promise)


# Quick Look

**Writing a Promise in Javascript**

A typical example of using promise in JavaScript is:

```JavaScript
Promise.resolve('foo')
    .then(function (data) {
        return new Promise(function (resolve, reject) {
            setTimeout(function () {
                const newData = data + 'bar';
                resolve(newData);
            }, 1);
        });
    })
    .then(function (data) {
        return new Promise(function (resolve, reject) {
            console.log(data);
        });
    });
console.log("Promise in JavaScript");
```

Result:

```
Promise in JavaScript
foobar
```

**Writing a Promise in java-promise**

Write the same thing using **java-promise**

```Java
import org.riversun.promise.Promise;
public class Example {

    public static void main(String[] args) {
        Promise.resolve("foo")
                .then(new Promise((action, data) -> {
                    new Thread(() -> {
                        String newData = data + "bar";
                        action.resolve(newData);
                    }).start();
                }))
                .then(new Promise((action, data) -> {
                    System.out.println(data);
                    action.resolve();
                }))
                .start();
        System.out.println("Promise in Java");
    }
}
```

Result:

```
Promise in Java
foobar
```

**Syntax:**  
Yes,you can write in a syntax similar to JavaScript as follows:

```Java
Promise.resolve()
        .then(new Promise(funcFulfilled1), new Promise(funcRejected1))
        .then(new Promise(functionFulfilled2), new Promise(functionRejected2))
        .start();
```        

<img src="https://user-images.githubusercontent.com/11747460/56907143-a11fa380-6ade-11e9-9d23-195cce2c1543.png">

# Dependency

**Maven**

```xml
<dependency>
    <groupId>org.riversun</groupId>
    <artifactId>java-promise</artifactId>
    <version>1.1.0</version>
</dependency>
```

**Gradle**

```
compile group: 'org.riversun', name: 'java-promise', version: '1.1.0'
```

# Quick Start

### Execute sequentially by chained "then"

- Use **Promise.then()** to chain operations.  
- Write your logic in **Func.run(action,data)**.
- Start operation by **Promise.start** and run **asynchronously**(run on worker thread)
- Calling **action.resolve** makes the promise **fullfilled** state and passes the **result** to the next then

```java
public class Example00 {

    public static void main(String[] args) {

        Func function1 = (action, data) -> {
            new Thread(() -> {
                System.out.println("Process-1");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                //Specify result value.(Any type can be specified)
                action.resolve("Result-1");
            }).start();
        };

        Func function2 = (action, data) -> {
            System.out.println("Process-2 result=" + data);
            action.resolve();
        };

        Promise.resolve()
                .then(new Promise(function1))
                .then(new Promise(function2))
                .start();// start Promise operation

        System.out.println("Hello,Promise");
    }
}
```

**Diagram:**
<img src="https://user-images.githubusercontent.com/11747460/56915517-acc89580-6af1-11e9-9ab2-c4274e8675ed.png">

**Result:**

```
Hello,Promise
Process-1
Process-1 result=Result-1
```

**Tips**

It's also OK to just write ``Promise.then(func) ``.

```Java
Promise.resolve()
        .then(function1)
        .then(function2)
        .start();// start Promise operation
```        

# Description
### What is "**Func**" ?

**Func** is a java interface equivalent to JavaScript's **Function** for argument of **#then**

```java
public interface Func {
	public void run(Action action, Object data) throws Exception;
}
```

You can write **Func** like a JavaScript function.  
I want to show two ways of implementing **Func** class.

No.1)Write **Func** object in the normal way.

```Java
Func function = new Func() {
    @Override
    public void run(Action action, Object data) throws Exception {
        System.out.println("Process");//write your logic
        action.resolve();
    }
};
```

No.2)Write **Func** object using lambda expression.

```Java
Func function = (action, data) -> {
    System.out.println("Process");//write your logic
    action.resolve();
};
```

### What is "**Action**" ?

Action object is an argument of **Func#run** method.  

- Call **action.resolve( [fulfillment value] )** to make the Promise's status **fulfilled** and move on to the next processing(specified by then) with the result(**fulfillment value**).

```Java
action.resolve("Success");
```

- Call **action.reject( [rejection reason] )** to make the Promise's status **rejected** and move on to the next processing(specified by then) with the result(**rejection reason**).

```Java
action.reject("Failure");
```

- Argument is optional, you can call **action.resolve()** or **action.reject()**

```Java
action.resolve();//Argument can be omitted
```

# Usage

### Rejection

If **action.reject()** is called, or if an **exception thrown** while executing **Func.run()**, **rejected** status is set to Promise, and the **onRejected** function specified to **then** is called.

- call ``action.reject``

```Java
Func function = (action, data) -> {
  action.reject("Failure");
};
```

- throw an exception

```Java
Func function = (action, data) -> {
  throw new Exception("something");
};

```

Let's see **Promise.then()** method,  
the **2nd argument** of **Promise.then()** can be set to a **Func** to receive the result of **rejection** when receiving the result of **then**.

- **Syntax**  
Usage ``Promise.then(onFulfilled[, onRejected]);``

- **onFulfilled** is a **Func** object called if the Promise is fulfilled.  
You can receive the previous execution **"fulfilled"** result as an argument named **data**.

- **onRejected** is a **Func** object called if the Promise is rejected.  
You can receive the previous execution **"rejected"** result(mainly the objects are exceptions) as an argument named **data**.

```Java
//Rejection
public class ExampleRejection {
    public static void main(String[] args) {
        Promise.resolve()
                .then((action, data) -> {
                    System.out.println("Process-1");
                    action.reject();
                })
                .then(
                        // call when resolved
                        (action, data) -> {
                            System.out.println("Resolved Process-2");
                            action.resolve();
                        },
                        // call when rejected
                        (action, data) -> {
                            System.out.println("Rejected Process-2");
                            action.resolve();
                        })
                .start();// start Promise operation

        System.out.println("Hello,Promise");
    }
}
```

**Diagram:**

<img src="https://user-images.githubusercontent.com/11747460/56916023-efd73880-6af2-11e9-8f92-fae5eff48e32.png">

**Result:**

```
Hello,Promise
Process-1
Rejected Process-2
```


### Promise.always

**Promise.always()** always receive both **fulfilled** and **rejected** results.

```Java
public class ExampleAlways {

    public static void main(String[] args) {
        Func func2OutReject = (action, data) -> {
            action.reject("I send REJECT");
            //action.resolve("I send RESOLVE");
        };
        Func func2ReceiveAlways = (action, data) -> {
            System.out.println("Received:" + data);
            action.resolve();
        };
        Promise.resolve()
                .then(func2OutReject)
                .always(func2ReceiveAlways)
                .start();
    }
}
```

**Diagram:**
<img src="https://user-images.githubusercontent.com/11747460/56918747-d1c10680-6af9-11e9-886b-2e7949d1114c.png">

**Result**

```
Received:I send REJECT
```

### Promise.all

Execute multiple promises at the same time, and after all executions are complete, move to the next processing with then

- Execute multiple promises simultaneously and wait until all the execution is finished before proceeding.
- If all finishes with resolve, execution results will be stored as **java.util.List<Object\>** in the order of invocation.
- If there is even one rejection, store that rejection reason in the result when the rejection occurs and move on to the next "then".

```Java
public class ExampleAll {
    public static void main(String[] args) {
        Func func1 = (action, data) -> {
            Promise.sleep(1000);
            System.out.println("func1 running");
            action.resolve("func1-result");
        };
        Func func2 = (action, data) -> {
            Promise.sleep(500);
            System.out.println("func2 running");
            action.resolve("func2-result");
        };
        Func func3 = (action, data) -> {
            Promise.sleep(1500);
            System.out.println("func3 running");
            action.resolve("func3-result");
        };
        Func funcGetResult = (action, data) -> {
            List<Object> resultList = (List<Object>) data;
            for (int i = 0; i < resultList.size(); i++) {
                Object o = resultList.get(i);
                System.out.println("No." + (i + 1) + " result is " + o);
            }
            action.resolve();
        };
        Promise.all(func1, func2, func3)
                .always(funcGetResult)
                .start();
    }
}
```

**Diagram:**
<img src="https://user-images.githubusercontent.com/11747460/57029142-d6adc380-6c7b-11e9-9e15-ddb84dad0d24.png">


**Result:**

```
func2 running
func1 running
func3 running
No.1 result is func1-result
No.2 result is func2-result
No.3 result is func3-result
```

### Threading

It is also possible to execute Promise processing on the specified executor.
<font color=red>Note if you use your own executor, remember to shut it down after use.
If you use your own executor, it will **NOT** be shutdown automatically</font>　　

At least one worker thread to be used in Promise.all,
and one thread for overall asynchronous execution, so a total of <font color=red>two or more threads</font> must be needed.

```Java
public class Example {

    public static void main(String[] args) {

        final ExecutorService myExecutor = Executors.newFixedThreadPool(5);

        Func func1 = (action, data) -> {
            System.out.println("func1 on " + Thread.currentThread().getName());
            action.resolve();
        };

        Func func2 = (action, data) -> {
            System.out.println("func2 on " + Thread.currentThread().getName());
            action.resolve();
        };

        Func func3 = (action, data) -> {
            System.out.println("func3 on " + Thread.currentThread().getName());
            action.resolve();
        };

        Promise.all(myExecutor, func1, func2, func3)
                .then((action, data) -> {
                    System.out.println("final process on " + Thread.currentThread().getName());
                    myExecutor.shutdown();//If you use your own executor, remember to shut it down after use
                    action.resolve();
                })
                .start();
    }
}
```

**Result:**

```
func1 on pool-1-thread-2
func2 on pool-1-thread-3
func3 on pool-1-thread-4
final process on pool-1-thread-1
```

# SyncPromise

SyncPromise, as the name implies, is a synchronous promise.  
While Promise is executed asynchronously, SyncPromise does NOT move next while it is chained by "then".  
All other features are the same as Promise.

```Java
public class Example02 {

    public static void main(String[] args) {
        Func func1 = (action, data) -> {
            new Thread(() -> {
                System.out.println("Process-1");
                action.resolve();
            }).start();

        };
        Func func2 = (action, data) -> {
            new Thread(() -> {
                System.out.println("Process-2");
                action.resolve();
            }).start();

        };
        SyncPromise.resolve()
                .then(func1)
                .then(func2)
                .start();
        System.out.println("Hello,Promise");
    }
}
```

**Result:**

```
Process-1
Process-2
Hello,Promise
```

Even if **func1** and **func2** are executed in a thread,  
``System.out.println("Hello,Promise")`` is always executed after that.Because **SyncPromise** is synchronous execution.
