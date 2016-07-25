/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.shell.pipe

import org.apache.log4j.Logger
import java.util.concurrent.ArrayBlockingQueue

object RendezvousPipe {
    private val LOGGER = Logger.getLogger(classOf[RendezvousPipe])
}
class RendezvousPipe extends InputPipe with OutputPipe {
    private val lock = new AnyRef()
    private val queue = new ArrayBlockingQueue[AnyRef](1)
    // TODO change this to Option
    private var threadCallingNext: Thread = null
    private var threadCallingPush: Thread = null
    private var terminated = false

    def setTerminated() {
        RendezvousPipe.LOGGER.debug("Waiting for lock to terminate the RendezvousPipe")
        lock.synchronized {
            RendezvousPipe.LOGGER.debug("setTerminate has lock")
            terminated = true
            RendezvousPipe.LOGGER.debug("Terminating the RendezvousPipe (thread calling next())")
            if (threadCallingNext == null) {
                RendezvousPipe.LOGGER.debug("No thread is calling next() when the RendezvousPipe is terminated")
            } else {
                RendezvousPipe.LOGGER.debug("Interrupting thread '" + threadCallingNext.getName + "'")
                threadCallingNext.interrupt()
                RendezvousPipe.LOGGER.debug("Interrupted thread '" + threadCallingNext.getName + "'")
            }
            RendezvousPipe.LOGGER.debug("Terminating the RendezvousPipe (thread calling push())")
            if (threadCallingPush == null) {
                RendezvousPipe.LOGGER.debug("No thread is calling push() when the RendezvousPipe is terminated")
            } else {
                RendezvousPipe.LOGGER.debug("Interrupting thread '" + threadCallingPush.getName + "'")
                threadCallingPush.interrupt()
                RendezvousPipe.LOGGER.debug("Interrupted thread '" + threadCallingPush.getName + "'")
            }
            RendezvousPipe.LOGGER.debug("setTerminate released lock")
        }
    }

    def push(any: AnyRef) {
        RendezvousPipe.LOGGER.debug("push waiting for lock")
        lock.synchronized {
            RendezvousPipe.LOGGER.debug("push has lock")
            if (terminated) {
                RendezvousPipe.LOGGER.debug("Pipe terminated; not pushing data")
                return
            }
            RendezvousPipe.LOGGER.debug("push releasing lock")
            threadCallingPush = Thread.currentThread()
        }
        try {
            RendezvousPipe.LOGGER.debug("pushing '" + any + "' in " + this)
            queue.put(any)
            RendezvousPipe.LOGGER.debug("pushed '" + any + "'")
        } catch {
            // this is quite normal - see setTerminated; the message will be
            // 'null', so don't log it.
            case e: InterruptedException =>
                RendezvousPipe.LOGGER.debug("Interrupted pushing into RendezvousPipe")
        } finally {
            lock.synchronized {
                threadCallingPush = null
            }
        }
    }

    def next(): Option[AnyRef] = {
        RendezvousPipe.LOGGER.debug("next waiting for lock")
        lock.synchronized {
            RendezvousPipe.LOGGER.debug("next has lock")
            if (terminated) {
                RendezvousPipe.LOGGER.debug("Pipe terminated; not returning data")
                return None
            }
            threadCallingNext = Thread.currentThread()
            RendezvousPipe.LOGGER.debug("Waiting for data")
            RendezvousPipe.LOGGER.debug("next releasing lock")
        }
        try {
            val obj = queue.take()
            RendezvousPipe.LOGGER.debug("Got data '" + obj + "'")
            Some(obj)
        } catch {
            case e: InterruptedException =>
            // this is quite normal - see setTerminated; the message will be
            // 'null', so don't log it.
            RendezvousPipe.LOGGER.debug("Interrupted pulling from RendezvousPipe")
            return None
        } finally {
            RendezvousPipe.LOGGER.debug("next waiting for lock to nullify threadCallingNext")
            lock.synchronized {
                RendezvousPipe.LOGGER.debug("next has lock to nullify threadCallingNext")
                threadCallingNext = null
                RendezvousPipe.LOGGER.debug("next releasing lock")
            }
        }
    }
}
