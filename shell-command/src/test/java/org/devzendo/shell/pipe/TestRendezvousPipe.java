/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.devzendo.shell.pipe;

import org.devzendo.commoncode.concurrency.ThreadUtils;
import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.Option;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestRendezvousPipe {
    private final RendezvousPipe pipe = new RendezvousPipe();

    @BeforeClass
    public static void setupLogging() {
        LoggingUnittestHelper.setupLogging();
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 4000)
    public void receiverBlocksUntilObjectPushed() throws InterruptedException {
        final CountDownLatch stored = new CountDownLatch(1);
        final Object[] store = new Object[] {null};
        final Thread receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                store[0] = pipe.next();
                stored.countDown();
            }
        });
        receiver.start();
        pipe.push("hello");
        stored.await();
        assertThat((Option<String>) store[0], OptionMatcher.isSome("hello"));
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 4000)
    public void receiverGetsNoneWhenTerminated() throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch stored = new CountDownLatch(1);
        final Object[] store = new Object[] {"replaced-with-none"};
        final Thread receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                started.countDown();
                store[0] = pipe.next();
                stored.countDown();
            }
        });
        receiver.setName("receiver");
        receiver.start();
        started.await();
        ThreadUtils.waitNoInterruption(1000); // give it chance to block on
                                              // next()
        pipe.setTerminated();
        stored.await();
        final Option<Object> option = (Option<Object>) store[0];
        assertThat(option, OptionMatcher.isNone());
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 4000)
    public void receiverGetsNoneWhenTerminatedBeforeNext() throws InterruptedException {
        pipe.setTerminated();
        final CountDownLatch stored = new CountDownLatch(1);
        final Object[] store = new Object[] {"replaced-with-none"};
        final Thread receiver = new Thread(new Runnable() {
            @Override
            public void run() {
                store[0] = pipe.next();
                stored.countDown();
            }
        });
        receiver.setName("receiver");
        receiver.start();
        stored.await();
        final Option<Object> option = (Option<Object>) store[0];
        assertThat(option, OptionMatcher.isNone());
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 4000)
    public void pushIntoTerminatedPipePushesNothing() throws InterruptedException {
        pipe.setTerminated();
        pipe.push("data");
        final Option<Object> option = pipe.next();
        assertThat(option, OptionMatcher.isNone());
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 4000)
    public void pushingThreadInterruptedWhenPipeTerminated() throws InterruptedException {
        pipe.push("one"); // does not block, but next push will

        final CountDownLatch outOfPush = new CountDownLatch(1);
        final Thread pusher = new Thread(new Runnable() {
            @Override
            public void run() {
                pipe.push("two"); // will block
                outOfPush.countDown();
            }
        });
        pusher.setName("pusher");
        pusher.start();
        ThreadUtils.waitNoInterruption(250);
        pipe.setTerminated();
        outOfPush.await();
        // can't sense any more here
    }

}
