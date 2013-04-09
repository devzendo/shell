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

import collection.JavaConversions._
import org.apache.log4j.Logger
import org.devzendo.commoncode.logging.Logging
import org.devzendo.commonapp.prefs.{DefaultPrefsLocation, PrefsLocation}
import java.io.File
import org.fusesource.jansi.{AnsiRenderer, AnsiConsole}
import jline.console.completer.CompletionHandler
import jline.console.ConsoleReader
import java.util
import org.devzendo.shell.plugin.{ExperimentalShellPlugin, LoggingShellPlugin, PluginsShellPlugin, VariablesShellPlugin}
import org.devzendo.shell.pipe.{VariableOutputPipe, VariableInputPipe}


class ShellMain2(val argList:java.util.List[String]) {
    val commandRegistry = new CommandRegistry()
    val variableRegistry = new VariableRegistry()
    // TODO convert PluginRegistry argList to a scala list, then convert the
    // argList val to a Scala list
    val pluginRegistry = new PluginRegistry(ShellMain2.SHELLPLUGIN_PROPERTIES, commandRegistry, variableRegistry, argList)

    private var quitShell = false

    class InternalShellPlugin extends ShellPlugin {
        @Override
        def getName: String = {
            "Internal"
        }

        def quit() {
            quitShell = true
        }

        @Override
        def initialise(env: ExecutionEnvironment) {
            // do nothing
        }
    }

    def start() {
        ShellMain2.LOGGER.debug("Starting DevZendo.org shell")
        argList.foreach( (f: String) => { ShellMain2.LOGGER.debug("ARG: [" + f + "]") } )

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
            pluginRegistry.loadAndRegisterPluginMethods(
                new InternalShellPlugin(),
                new VariablesShellPlugin(),
                new PluginsShellPlugin(),
                new LoggingShellPlugin(),
                new ExperimentalShellPlugin()
            )

            val parser = new CommandParser()
            val wirer = new CommandHandlerWirer(commandRegistry, variableRegistry)
            while (!quitShell) {
                val input = lineReader.readLine("] ")
                ShellMain2.LOGGER.debug("input: [" + input + "]")
                for (line <- input) {
                    try {
                        val commandPipeline = parser.parse(line.trim())
                        if (!commandPipeline.isEmpty) {
                            val commandHandlers = wirer.wire(commandPipeline)
                            if (ShellMain2.LOGGER.isDebugEnabled) {
                                dumpHandlers(commandHandlers)
                            }
                            val executionContainer = new ExecutionContainer(commandHandlers)
                            executionContainer.execute()
                        }
                    } catch {
                        case cpe: CommandParserException =>
                            ShellMain2.LOGGER.warn(cpe.getMessage)
                        case cnfe: CommandNotFoundException =>
                            ShellMain2.LOGGER.warn(cnfe.getMessage)
                        case cee: CommandExecutionException =>
                            ShellMain2.LOGGER.warn(cee.getMessage)
                    }
                }
            }
        } catch {
            case e: ShellPluginException =>
                ShellMain2.LOGGER.fatal("Can't continue: " + e.getMessage)
        }
    }

    private[this] def banner() {
        val lines = List(
            " __ _          _ _",
            "/ _\\ |__   ___| | |",
            "\\ \\| '_ \\ / _ \\ | |",
            "_\\ \\ | | |  __/ | |",
            "\\__/_| |_|\\___|_|_|")
        lines.foreach( line => { AnsiConsole.out().println(AnsiRenderer.render("@|bold,blue " + line + "|@")) } )
    }

    private[this] def dumpHandlers(commandHandlers: util.List[CommandHandler]) {
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
        sb.deleteCharAt(sb.length -1)
        ShellMain2.LOGGER.debug("pipeline: " + sb.toString())
    }

}

object ShellMain2 {
    private val LOGGER = Logger.getLogger(classOf[ShellMain2])
    private val SHELLPLUGIN_PROPERTIES = "shellplugin.properties"

    /**
     * @param args the command line arguments.
     */
    def main(args: Array[String]) {
        val logging = Logging.getInstance()
        val argList: java.util.List[String] = new java.util.ArrayList[String]()
        args.foreach(s => argList.add(s))
        val finalArgList = logging.setupLoggingFromArgs(argList)
        new ShellMain2(finalArgList).start()
    }
}

