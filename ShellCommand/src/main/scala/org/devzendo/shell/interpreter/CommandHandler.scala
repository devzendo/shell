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
import org.devzendo.shell.pipe.{OutputPipe, InputPipe}
import java.util

object CommandHandler {
    private val LOGGER = Logger.getLogger(classOf[CommandHandler])
}

/**
 * @param name the command name that this handler will execute
 * @param argumentsPos
 * @param inputPipePos
 * @param outputPipePos
 */
abstract class CommandHandler(
    val name: String,
    val argumentsPos: Option[Integer],
    val inputPipePos: Option[Integer],
    val outputPipePos: Option[Integer]) {

    def getName = name
    def getArgumentsPos = argumentsPos
    def getInputPipePos = inputPipePos
    def getOutputPipePos = outputPipePos

    // TODO use Option[Input|OutputPipe] here instead of null
    @scala.reflect.BeanProperty
    final var inputPipe: InputPipe = null

    @scala.reflect.BeanProperty
    final var outputPipe: OutputPipe = null

    @scala.reflect.BeanProperty
    final var args: java.util.List[AnyRef] = new util.ArrayList[AnyRef]()

    @throws[CommandExecutionException]
    def execute()

    @throws[CommandExecutionException]
    final def executeAndTerminatePipes() {
        try {
            CommandHandler.LOGGER.debug("executing...")
            execute()
        } finally {
            CommandHandler.LOGGER.debug("terminating pipes")
            terminatePipes()
            CommandHandler.LOGGER.debug("pipes terminated")
            CommandHandler.LOGGER.debug("...executed")
        }
    }

    private final def terminatePipes() {
        if (getInputPipe != null) {
            getInputPipe.setTerminated()
        }
        if (getOutputPipe != null) {
            getOutputPipe.setTerminated()
        }
    }
}
