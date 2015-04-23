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
import org.devzendo.commoncode.resource.ResourceLoader
import org.devzendo.shell.ast.VariableReference

class ShellMain(val argList: List[String]) {
    import ShellMain._

    val commandRegistry = new CommandRegistry()
    val variableRegistry = new DefaultVariableRegistry(None)
    val pluginRegistry = new DefaultPluginRegistry(ShellMain.SHELLPLUGIN_PROPERTIES, commandRegistry, variableRegistry, argList)
    val shellProperties = loadShellProperties()
    
    private var quitShell = false
    private var showBanner = true


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
        LOGGER.info("-nobanner   - do not display banner on startup")
        LOGGER.info("-help, -?   - just display this help text")
        LOGGER.info("-version    - just display the version of dzsh")
        LOGGER.info("Log4j output control options:")
        LOGGER.info("-debug      - set the log level to debug (default is info)")
        LOGGER.info("-warn       - set the log level to warning")
        LOGGER.info("-level      - show log levels of each log line output")
        LOGGER.info("-classes    - show class names in each log line output")
        LOGGER.info("-threads    - show thread names in each log line output")
        LOGGER.info("-times      - show timing data in each log line output")
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
        argList.foreach( (f: String) => {
            ShellMain.LOGGER.debug("ARG: [" + f + "]")
            f match {
                case "-nobanner" => showBanner = false
                case "-help" => { usage(); exit() }
                case "-?" => { usage(); exit() }
                case "-version"  => { version(); exit() }
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
        })

        // TODO prefs only loaded if this is an interactive shell?
        val prefsLocation: PrefsLocation = new DefaultPrefsLocation(".dzsh", "dzsh.ini")
        val historyFile = new File(prefsLocation.getPrefsDir, "history.txt")

        // TODO console handling only done if this is an interactive shell i.e. not launching a script
        if (isatty()) {
            AnsiConsole.systemInstall()
            if (showBanner) {
                banner()
            }
        }
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
                new ControlShellPlugin(),
                new VersionPlugin(),
                new ExperimentalShellPlugin())
            )

            val commandExists = new CommandExists {
                def commandExists(name: String) = commandRegistry.exists(name)
            }

            val parser = new CommandParser(commandExists)
            val wirer = new CommandHandlerWirer(commandRegistry)
            // TODO load and parse scripts, execute them.
            while (!quitShell) {
                val input = lineReader.readLine("] ")
                ShellMain.LOGGER.debug("input: [" + input + "]")
                for (line <- input) {
                    try {
                        val statements = parser.parse(line.trim())
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


