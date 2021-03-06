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
import org.devzendo.shell.ast._
import scala.collection.JavaConversions._
import org.devzendo.shell.ast.Switch
import scala.Some
import org.apache.log4j.Logger

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
    private val LOGGER = Logger.getLogger(classOf[CommandHandlerWirer])
    val verboseLog = new Log4JLog(true)
    val nonverboseLog = new Log4JLog(false)
}

case class CommandHandlerWirer(commandRegistry: CommandRegistry) {

    private[this] def isFilterVerboseSwitch(p: AnyRef) = p match {
        case sw: Switch =>
            sw.switchName.equals("Verbose")
        case _ =>
            false
    }

    @throws[CommandNotFoundException]
    def wire(parentVariableRegistry: VariableRegistry, statement: Statement): List[CommandHandler] = {
        statement match {
            case blockStatements: BlockStatements => List(wireBlockStatements(parentVariableRegistry, blockStatements))
            case commandPipeline: CommandPipeline => wireCommandPipeline(parentVariableRegistry, commandPipeline)
        }
    }


    @throws[CommandNotFoundException]
    def wireBlockStatements(parentVariableRegistry: VariableRegistry, blockStatements: BlockStatements): CommandHandler = {
        val childVariableRegistry = new DefaultVariableRegistry(Some(parentVariableRegistry))
        blockStatements.setVariableRegistry(childVariableRegistry) // needed?
        val listOfCommandHandlerLists = blockStatements.getStatements.map { wire(childVariableRegistry, _) }
        val blockCommandHandler = new SequentialCommandHandler(listOfCommandHandlerLists)
        blockCommandHandler.setVariableRegistry(childVariableRegistry)
        blockCommandHandler.setVerbose(false)
        blockCommandHandler.setLog(CommandHandlerWirer.nonverboseLog)
        blockCommandHandler.setInputPipe(new NullInputPipe())
        blockCommandHandler.setOutputPipe(new NullOutputPipe())  // note, blocks as args use a VariableOutputPipe

        blockCommandHandler
    }

    @throws[CommandNotFoundException]
    def wireCommandPipeline(variableRegistry: VariableRegistry, commandPipeline: CommandPipeline): List[CommandHandler] = {
        val handlers = scala.collection.mutable.ArrayBuffer[CommandHandler]()
        for (command <- commandPipeline.getCommands) {
            handlers += initialiseCommandHandler(command, variableRegistry)
        }
        // TODO convert this null to Option
        // cat /dev/null > first, unless storing in a variable
        if (handlers.size > 0) {
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
        }
        handlers.toList
    }

    private def initialiseCommandHandler(command: Command, variableRegistry: VariableRegistry): CommandHandler = {
        val handler = commandRegistry.getHandler(command.getName)
        val args = command.getArgs

        val verbose = args.exists(isFilterVerboseSwitch)
        handler.setVerbose(verbose)
        handler.setLog(if (verbose) CommandHandlerWirer.verboseLog else CommandHandlerWirer.nonverboseLog)

        val filteredArgs = args.filterNot(isFilterVerboseSwitch).toList
        val (subCommandHandlers, arguments) = filteredArgs.map((arg: AnyRef) =>
            arg match {
                case subCommand: Command =>
                    val subCommandHandler = initialiseCommandHandler(subCommand, variableRegistry)
                    val avp = new AnonymousVariablePipe()
                    subCommandHandler.setInputPipe(new NullInputPipe())
                    subCommandHandler.setOutputPipe(avp)
                    (Some(subCommandHandler), avp.contents)

                case block: BlockStatements =>
                    val blockCommandHandler = wireBlockStatements(variableRegistry, block)
                    // wireBlockStatements fills in a NullInputPipe, and NullOutputPipe, but we
                    // want to store the output in an anonymous variable, so that output can be
                    // captured by the block executor (e.g. 'if', 'while', etc.) after lazy
                    // evaluation
                    val avp = new AnonymousVariablePipe()
                    blockCommandHandler.setOutputPipe(avp)
                    (None, blockCommandHandler)

                case x: AnyRef =>
                    (None, x)
            }
        ).unzip
        handler.setArgs(arguments)
        handler.setSubCommandHandlers(subCommandHandlers)

        CommandHandlerWirer.LOGGER.debug("setting command " + command.name + " variable registry to " + variableRegistry)
        handler.setVariableRegistry(variableRegistry)
        handler
    }

    private def connectByRendezvousPipe(left: CommandHandler, right: CommandHandler) {
        val pipe = new RendezvousPipe()
        left.setOutputPipe(pipe)
        right.setInputPipe(pipe)
    }
}
