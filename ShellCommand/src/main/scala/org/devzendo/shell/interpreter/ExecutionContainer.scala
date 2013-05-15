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

package org.devzendo.shell.interpreter

import org.apache.log4j.Logger
import scala.throws
import java.util.concurrent.CountDownLatch
import java.util.Collections
import java.util

object ExecutionContainer {
    private val LOGGER = Logger.getLogger(classOf[ExecutionContainer])
}

case class ExecutionContainer(commandHandlers: List[CommandHandler]) {

    @throws[CommandExecutionException]
    def execute() {
        commandHandlers.size match {
            case 0 =>
                // do nothing
            case 1 =>
                executeOnCurrentThread()
            case _ =>
                executeOnMultipleThreads()
        }
    }

    @throws[CommandExecutionException]
    private def executeOnMultipleThreads() {
        val latch = new CountDownLatch(commandHandlers.size)
        val exceptions = Collections.synchronizedList(new util.ArrayList[CommandExecutionException]())
        commandHandlers.foreach {
            (handler: CommandHandler) => {
                val thread = new Thread(new Runnable() {
                    def run() {
                        try {
                            handler.executeAndTerminatePipes()
                        } catch {
                            case e: CommandExecutionException =>
                                exceptions.add(e)
                        } finally {
                            latch.countDown()
                        }
                    }
                })
                thread.setName(handler.getName)
                thread.start()
            }
        }
        ExecutionContainer.LOGGER.debug("waiting for execution to end")
        try {
            latch.await()
        } catch {
            case e: InterruptedException =>
                throw new CommandExecutionException("Wait for commands to finish executing was interrupted: " + e.getMessage)
        }
        ExecutionContainer.LOGGER.debug("execution has ended")

        if (exceptions.size() != 0) {
            val sb = new StringBuilder()
            sb.append("Multiple commands failed: ")
            for (i <- 0 until exceptions.size()) {
                sb.append(exceptions.get(i).getMessage)
                if (i != exceptions.size() - 1) {
                    sb.append("; ")
                }
            }
            throw new CommandExecutionException(sb.toString())
        }
    }

    @throws[CommandExecutionException]
    private def executeOnCurrentThread() {
        commandHandlers.head.executeAndTerminatePipes()
    }
}
