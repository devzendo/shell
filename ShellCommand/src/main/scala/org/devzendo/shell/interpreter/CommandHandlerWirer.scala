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

import org.devzendo.shell.pipe._
import org.devzendo.shell.ast.{Switch, CommandPipeline}
import scala.collection.JavaConversions._

/**
 * Given a command pipeline, create a list of command handlers for each
 * command in the pipeline, and wire up the input and output pipes between
 * them, also, connect the pipeline input and outputs. Set standard command
 * handler flags (Verbose), and remove these Switches from the arguments.
 *
 * @author matt
 *
 */
object CommandHandlerWirer {
    val verboseLog = new Log4JLog(true)
    val nonverboseLog = new Log4JLog(false)
}
case class CommandHandlerWirer(commandRegistry: CommandRegistry, variableRegistry: VariableRegistry) {

    private[this] def isFilterSwitch(p: AnyRef) = p match {
        case sw: Switch =>
            sw.switchName.equals("Verbose")
        case _ =>
            false
    }

    @throws[CommandNotFoundException]
    def wire(commandPipeline: CommandPipeline): List[CommandHandler] = {
        val handlers = scala.collection.mutable.ArrayBuffer[CommandHandler]()
        val commands = commandPipeline.getCommands
        assert(commands.size > 0)
        for (command <- commands) {
            val handler = commandRegistry.getHandler(command.getName)
            val args = command.getArgs
            val verbose = args.exists( isFilterSwitch )
            val filteredArgs = args.filterNot( isFilterSwitch ).toList
            handler.setVerbose(verbose)
            handler.setArgs(filteredArgs)
            handler.setLog(if (verbose) CommandHandlerWirer.verboseLog else CommandHandlerWirer.nonverboseLog)
            handlers += handler
        }
        // TODO convert this null to Option
        // cat /dev/null > first, unless storing in a variable
        val pipelineInputVariable = commandPipeline.getInputVariable
        if (pipelineInputVariable == null) {
            handlers.head.setInputPipe(new NullInputPipe())
        } else {
            val inputVariable = variableRegistry.getVariable(pipelineInputVariable)
            handlers.head.setInputPipe(new VariableInputPipe(inputVariable))
        }
        // last | echo, unless storing in a variable
        val pipelineOutputVariable = commandPipeline.getOutputVariable
        if (pipelineOutputVariable == null) {
            handlers.last.setOutputPipe(new LogInfoOutputPipe())
        } else {
            val outputVariable = variableRegistry.getVariable(pipelineOutputVariable)
            handlers.head.setOutputPipe(new VariableOutputPipe(outputVariable))
        }
        // left | right
        for (i <- 0 until (handlers.size - 1)) {
            val left = handlers(i)
            val right = handlers(i + 1)

            if (right.getInputPipePos.isEmpty || left.getOutputPipePos.isEmpty) {
                left.setOutputPipe(new NullOutputPipe())
                right.setInputPipe(new NullInputPipe())
            } else {
                // In cases where left has output, and right has input:
                connectByRendezvousPipe(left, right)
            }
        }
        handlers.toList
    }

    private def connectByRendezvousPipe(left: CommandHandler, right: CommandHandler) {
        val pipe = new RendezvousPipe()
        left.setOutputPipe(pipe)
        right.setInputPipe(pipe)
    }
}
