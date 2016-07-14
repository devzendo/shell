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

import org.fusesource.jansi.{AnsiConsole, AnsiRenderer}
import jline.console.completer.CompletionHandler
import jline.console.ConsoleReader
import java.util

import org.devzendo.shell.plugin._
import org.devzendo.shell.pipe.{VariableInputPipe, VariableOutputPipe}

import collection.JavaConverters._
import org.devzendo.shell.parser.{CommandParser, CommandParserException, ExistenceChecker}
import org.devzendo.shell.interpreter._
import org.devzendo.shell.interpreter.CommandHandlerWirer
import org.devzendo.commoncode.resource.ResourceLoader
import org.devzendo.shell.analyser.SemanticAnalyser
import org.devzendo.shell.ast.VariableReference

import scala.io.Source


object ExecutionMode extends Enumeration {
    type ExecutionMode = Value
    val Interactive, Script, OneLiner = Value
}

trait ScriptSource {
    def initialise
    def nextScript: Option[String]
}

class ShellMain(val argList: List[String]) {
    import ShellMain._
    import ExecutionMode._

    val commandRegistry = new CommandRegistry()
    val variableRegistry = new DefaultVariableRegistry(None)
    val pluginRegistry = new DefaultPluginRegistry(ShellMain.SHELLPLUGIN_PROPERTIES, commandRegistry, variableRegistry, argList)
    val shellProperties = loadShellProperties()
    
    private var quitShell = false

    class InternalShellPlugin extends ShellPlugin {
        @Override
        def getName: String = {
            "Internal"
        }

        @CommandAlias(alias = "exit")
        def quit() {
            LOGGER.debug("Requesting interpreter termination")
            quitShell = true
        }

        @Override
        def initialise(env: ExecutionEnvironment) {
            // do nothing
        }
    }

    class VersionPlugin extends ShellPlugin {
        def initialise(env: ExecutionEnvironment): Unit = {
            val variable = new Variable()
            variable.add(getShellPropertiesVersion())
            env.variableRegistry().setVariable(VariableReference("SHELL_VERSION"), variable)
        }

        def getName: String = "Version"
    }

    def isatty() = System.console() != null

    def usage() {
        LOGGER.info("dzsh [options] [script.dzsh ... script.dzsh]")
        LOGGER.info("Shell options:")
        LOGGER.info("-nobanner      - do not display banner on startup")
        LOGGER.info("-help, -?      - just display this help text")
        LOGGER.info("-version       - just display the version of dzsh")
        LOGGER.info("-script '....' - execute a 'one-liner' script")
        LOGGER.info("Log4j output control options:")
        LOGGER.info("-debug         - set the log level to debug (default is info)")
        LOGGER.info("-warn          - set the log level to warning")
        LOGGER.info("-level         - show log levels of each log line output")
        LOGGER.info("-classes       - show class names in each log line output")
        LOGGER.info("-threads       - show thread names in each log line output")
        LOGGER.info("-times         - show timing data in each log line output")
        LOGGER.info("Interactive mode is used if no -script ... or script file")
        LOGGER.info("names are given.")
    }

    def exit() {
        System.exit(0)
    }


    def loadShellProperties(): util.Properties = {
        val propertiesResourceName = "shell.properties"
        val propertiesResource = ResourceLoader.readPropertiesResource(propertiesResourceName)
        if (propertiesResource == null) {
            LOGGER.fatal("Could not load " + propertiesResourceName)
            exit()
        }
        propertiesResource
    }

    def getShellPropertiesVersion() = {
        shellProperties.getProperty("version")
    }

    def version() {
        LOGGER.info(ShellMain.SHELL_NAME + " " + getShellPropertiesVersion())
    }

    def start() {
        ShellMain.LOGGER.debug("Starting " + SHELL_NAME)
        val shellProperties = loadShellProperties()
        var scripts = new scala.collection.mutable.ListBuffer[File]
        var oneLiner = new scala.collection.mutable.ListBuffer[String]
        var executionMode = Interactive
        var showBanner = true

        var argIndex = 0
        while (argIndex < argList.length)
        {
            val f = argList(argIndex)

            ShellMain.LOGGER.debug("ARG: [" + f + "]")
            f match {
                case "-nobanner" => showBanner = false
                case "-help" => { usage(); exit() }
                case "-?" => { usage(); exit() }
                case "-version"  => { version(); exit() }

                case "-script" => {
                    if (argIndex == argList.length - 1) {
                        LOGGER.error("-script requires a script as its argument")
                        exit()
                    }
                    oneLiner += argList(argIndex + 1)
                    executionMode = OneLiner
                    argIndex += 1
                }

                case _ => {
                    if (f.startsWith("-")) {
                        LOGGER.error("Unknown command line option: '" + f + "'")
                        exit()
                    }
                    val file = new File(f)
                    if (file.exists()) {
                        scripts += file
                    } else {
                        LOGGER.error("The script '" + f + "' does not exist")
                    }
                }
            }

            argIndex += 1
        }

        if (!scripts.isEmpty) {
            executionMode = Script
        }

        // TODO prefs only loaded if this is an interactive shell?
        val prefsLocation: PrefsLocation = new DefaultPrefsLocation(".dzsh", "dzsh.ini")
        val historyFile = new File(prefsLocation.getPrefsDir, "history.txt")

        val scriptSource = executionMode match {
            case Interactive => new ScriptSource {
                val completionHandler = new CompletionHandler() {
                    def complete(reader: ConsoleReader, candidates: util.List[CharSequence], position: Int) = {
                        false
                    }
                }

                val lineReader: LineReader = new JLineLineReader(historyFile, completionHandler)

                def initialise: Unit = {
                    // TODO console handling only done if this is an interactive shell i.e. not launching a script
                    if (isatty()) {
                        AnsiConsole.systemInstall()
                        if (showBanner) {
                            banner()
                        }
                    }

                }

                def nextScript: Option[String] = {
                    lineReader.readLine("] ") match {
                        case Some(x) => Some(x.trim())
                        case None => None
                    }
                }
            }

            case OneLiner => new ScriptSource {
                // TODO this seems a bit funky...
                oneLiner += "\nquit\n"
                LOGGER.info("one line script: " + oneLiner)
                val it = oneLiner.iterator
                def initialise: Unit = {}

                def nextScript: Option[String] = {
                    if (it.hasNext) {
                        Some(it.next())
                    } else {
                        None
                    }
                }
            }

            case Script => new ScriptSource {
                // TODO investigate incremental parsing with combinators
                // TODO funky bodging in of a quit
                val scriptsAsStrings = scripts.map { Source.fromFile(_).getLines().toList.mkString("\n") ++ "\nquit\n" }
                val scriptIterator = scriptsAsStrings.iterator

                def initialise: Unit = {}

                def nextScript: Option[String] = {
                    if (scriptIterator.hasNext) {
                        Some(scriptIterator.next())
                    } else {
                        None
                    }
                }
            }
        }


        try {
            variableRegistry.incrementUsage()
            pluginRegistry.loadAndRegisterPluginMethods(List(
                new InternalShellPlugin(),
                new VariablesShellPlugin(),
                new PluginsShellPlugin(),
                new CommandsShellPlugin(),
                new LoggingShellPlugin(),
                new BasicOperatorsPlugin(),
                new ControlShellPlugin(),
                new VersionPlugin(),
                new ExperimentalShellPlugin())
            )

            val commandExists = new ExistenceChecker {
                def exists(name: String) = commandRegistry.exists(name)
            }

            val semanticAnalyser = new SemanticAnalyser(commandExists)
            val parser = new CommandParser(commandExists, false, semanticAnalyser)
            val wirer = new CommandHandlerWirer(commandRegistry)

            while (!quitShell) {
                val input = scriptSource.nextScript
                ShellMain.LOGGER.debug("input: [" + input + "]")
                for (line <- input) {
                    try {
                        val statements = parser.parse(line)
                        if (ShellMain.LOGGER.isDebugEnabled) {
                            ShellMain.LOGGER.debug(">>> parsed statements...")
                            for (statement <- statements) {
                                ShellMain.LOGGER.debug("  " + statement)
                            }
                            ShellMain.LOGGER.debug("<<< parsed statements")

                        }
                        for (statement <- statements) {
                            val commandHandlers = wirer.wire(variableRegistry, statement)
                            if (ShellMain.LOGGER.isDebugEnabled) {
                                ShellMain.LOGGER.debug(">>> parsed & wired statement: ")
                                ShellMain.LOGGER.debug("  " + statement)
                                ShellMain.LOGGER.debug(">>> wired command handlers...")
                                ShellMain.LOGGER.debug("  " + dumpHandlers(commandHandlers))
                                ShellMain.LOGGER.debug("<<< wired command handlers")
                            }

                            ShellMain.LOGGER.debug(">>> executing...")
                            val executionContainer = new ExecutionContainer(commandHandlers)
                            executionContainer.execute()
                            ShellMain.LOGGER.debug("<<< executed")

                            ShellMain.LOGGER.debug("variable registry: [" + variableRegistry + "]")
                        }
                    } catch {
                        case cpe: CommandParserException =>
                            ShellMain.LOGGER.warn(cpe.getMessage)
                            ShellMain.LOGGER.debug(cpe.getMessage, cpe)
                        case cnfe: CommandNotFoundException =>
                            ShellMain.LOGGER.warn(cnfe.getMessage)
                            ShellMain.LOGGER.debug(cnfe.getMessage, cnfe)
                        case cee: CommandExecutionException =>
                            ShellMain.LOGGER.warn(cee.getMessage)
                            ShellMain.LOGGER.debug(cee.getMessage, cee)
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
            ShellMain.SHELL_NAME)
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
        sb.toString()
    }

}

object ShellMain {
    val LOGGER = Logger.getLogger(classOf[ShellMain])
    private val SHELLPLUGIN_PROPERTIES = "shellplugin.properties"
    val SHELL_NAME = "DevZendo.org Object Shell"

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


