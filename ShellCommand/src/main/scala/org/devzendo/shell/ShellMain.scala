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

package org.devzendo.shell

import org.apache.log4j.Logger
import org.devzendo.commoncode.logging.Logging
import org.devzendo.commonapp.prefs.{DefaultPrefsLocation, PrefsLocation}
import java.io.File
import org.fusesource.jansi.{AnsiRenderer, AnsiConsole}
import jline.console.completer.CompletionHandler
import jline.console.ConsoleReader
import java.util
import org.devzendo.shell.plugin._
import org.devzendo.shell.pipe.{VariableOutputPipe, VariableInputPipe}
import collection.JavaConverters._
import org.devzendo.shell.parser.{CommandExists, CommandParserException, CommandParser}
import org.devzendo.shell.interpreter._
import org.devzendo.shell.interpreter.CommandHandlerWirer
import org.devzendo.shell.ast.{BlockStatements, CommandPipeline}

class ShellMain(val argList: List[String]) {
    val commandRegistry = new CommandRegistry()
    val variableRegistry = new DefaultVariableRegistry(None)
    val pluginRegistry = new DefaultPluginRegistry(ShellMain.SHELLPLUGIN_PROPERTIES, commandRegistry, variableRegistry, argList)

    private var quitShell = false

    class InternalShellPlugin extends ShellPlugin {
        @Override
        def getName: String = {
            "Internal"
        }

        @CommandAlias(alias = "exit")
        def quit() {
            quitShell = true
        }

        @Override
        def initialise(env: ExecutionEnvironment) {
            // do nothing
        }
    }

    def start() {
        ShellMain.LOGGER.debug("Starting DevZendo.org shell")
        argList.foreach( (f: String) => { ShellMain.LOGGER.debug("ARG: [" + f + "]") } )

        val prefsLocation: PrefsLocation = new DefaultPrefsLocation(".shell", "prefs.ini")
        val historyFile = new File(prefsLocation.getPrefsDir, "history.txt")

        // if isatty... {
        AnsiConsole.systemInstall()
        banner()
        // }
        val completionHandler = new CompletionHandler() {
            def complete(reader: ConsoleReader, candidates: util.List[CharSequence], position: Int) = {
                false
            }
        }

        val lineReader: LineReader = new JLineLineReader(historyFile, completionHandler)

        try {
            variableRegistry.incrementUsage()
            pluginRegistry.loadAndRegisterPluginMethods(List(
                new InternalShellPlugin(),
                new VariablesShellPlugin(),
                new PluginsShellPlugin(),
                new CommandsShellPlugin(),
                new LoggingShellPlugin(),
                new BasicOperatorsPlugin(),
                new ExperimentalShellPlugin())
            )

            val commandExists = new CommandExists {
                def commandExists(name: String) = commandRegistry.exists(name)
            }

            val parser = new CommandParser(commandExists)
            val wirer = new CommandHandlerWirer(commandRegistry)
            while (!quitShell) {
                val input = lineReader.readLine("] ")
                ShellMain.LOGGER.debug("input: [" + input + "]")
                for (line <- input) {
                    try {
                        val statements = parser.parse(line.trim())
                        for (statement <- statements) {
                            val commandHandlers = wirer.wire(variableRegistry, statement)
                            if (ShellMain.LOGGER.isDebugEnabled) {
                                dumpHandlers(commandHandlers)
                            }

                            val executionContainer = new ExecutionContainer(commandHandlers)
                            executionContainer.execute()
                            ShellMain.LOGGER.debug("variable registry: [" + variableRegistry + "]")
                        }
                    } catch {
                        case cpe: CommandParserException =>
                            ShellMain.LOGGER.warn(cpe.getMessage)
                        case cnfe: CommandNotFoundException =>
                            ShellMain.LOGGER.warn(cnfe.getMessage)
                        case cee: CommandExecutionException =>
                            ShellMain.LOGGER.warn(cee.getMessage)
                    }
                }
            }
            variableRegistry.decrementUsage()
        } catch {
            case e: ShellPluginException =>
                ShellMain.LOGGER.fatal("Can't continue: " + e.getMessage)
        }
    }

    private[this] def banner() {
        val lines = List(
            " __ _          _ _",
            "/ _\\ |__   ___| | |",
            "\\ \\| '_ \\ / _ \\ | |",
            "_\\ \\ | | |  __/ | |",
            "\\__/_| |_|\\___|_|_|",
            "DevZendo.org Object Shell")
        lines.foreach( line => {
            AnsiConsole.out().println(AnsiRenderer.render("@|bold,blue " + line + "|@"))
        } )
    }

    private[this] def dumpHandlers(commandHandlers: List[CommandHandler]) {
        val sb = new StringBuilder()
        commandHandlers.foreach( handler => {
            sb.append("<")
            val inputPipe = handler.getInputPipe
            sb.append(inputPipe.getClass.getSimpleName)
            if (inputPipe.isInstanceOf[VariableInputPipe]) {
                sb.append("(")
                sb.append(inputPipe.asInstanceOf[VariableInputPipe].getVariable)
                sb.append(")")
            }

            sb.append(" [")

            sb.append(handler.getName)

            sb.append("] >")
            val outputPipe = handler.getOutputPipe
            sb.append(outputPipe.getClass.getSimpleName)
            if (outputPipe.isInstanceOf[VariableOutputPipe]) {
                sb.append("(")
                sb.append(outputPipe.asInstanceOf[VariableOutputPipe].getVariable)
                sb.append(")")
            }

            sb.append(" ")
        })
        if (sb.length > 0) {
            sb.deleteCharAt(sb.length -1)
        }
        ShellMain.LOGGER.debug("pipeline: " + sb.toString())
    }

}

object ShellMain {
    val LOGGER = Logger.getLogger(classOf[ShellMain])
    private val SHELLPLUGIN_PROPERTIES = "shellplugin.properties"

    /**
     * @param args the command line arguments.
     */
    def main(args: Array[String]) {
        val logging = Logging.getInstance()
        val argList: java.util.List[String] = new java.util.ArrayList[String]()
        args.foreach(s => argList.add(s))
        val finalArgList = logging.setupLoggingFromArgs(argList).asScala.toList
        new ShellMain(finalArgList).start()
    }
}


