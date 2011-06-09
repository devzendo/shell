/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devzendo.shell.pipe;

import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import scala.Option;

public class RendezvousPipe implements InputPipe, OutputPipe {
    private static final Logger LOGGER = Logger.getLogger(RendezvousPipe.class);
    private final Object lock = new Object(); 
    private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);
    private Thread threadCallingNext;
    private boolean terminated = false;
    
    @Override
    public void setTerminated() {
        LOGGER.debug("Waiting to terminate the RendezvousPipe");
        synchronized (lock) {
            terminated = true;
            LOGGER.debug("Terminating the RendezvousPipe");
            if (threadCallingNext == null) {
                LOGGER.debug("No thread is calling next() when the RendezvousPipe is terminated");
            } else {
                threadCallingNext.interrupt();
            }
        }
    }

    @Override
    public void push(Object object) {
        try {
            queue.put(object);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted pushing into RendezvousPipe: " + e.getMessage());
        }
    }

    @Override
    public Option<Object> next() {
        synchronized (lock) {
            if (terminated) {
                LOGGER.debug("Pipe terminated; not returning data");
                return scala.Option.apply(null); // a.k.a. None
            }
            threadCallingNext = Thread.currentThread();
            LOGGER.debug("Waiting for data");
        }
        try {
            final Object object = queue.take();
            LOGGER.debug("Got data " + object);
            return new scala.Some<Object>(object);
        } catch (InterruptedException e) {
            // this is quite normal - see setTerminated; the message will be 
            // 'null', so don't log it.
            LOGGER.debug("Interrupted pulling from RendezvousPipe");
            return scala.Option.apply(null); // a.k.a. None
        } finally {
            synchronized (lock) {
                threadCallingNext = null;
            }
        }
    }
}
