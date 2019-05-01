package org.riversun.promise;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for java-promise<br>
 * 
 * Tom Misawa (riversun.org@gmail.com)
 */
@RunWith(Suite.class)
@SuiteClasses({
        TestPromiseSync.class, TestPromiseAsync.class,
        TestPromiseAllSync.class, TestPromiseAllAsync.class
})
public class AppTest {

}