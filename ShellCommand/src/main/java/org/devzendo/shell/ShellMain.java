/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.devzendo.shell;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.CompletionHandler;
import org.apache.log4j.Logger;
import org.devzendo.commonapp.prefs.DefaultPrefsLocation;
import org.devzendo.commonapp.prefs.PrefsLocation;
import org.devzendo.commoncode.logging.Logging;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.devzendo.shell.pipe.VariableInputPipe;
import org.devzendo.shell.pipe.VariableOutputPipe;
import org.devzendo.shell.plugin.LoggingShellPlugin;
import org.devzendo.shell.plugin.PluginsShellPlugin;
import org.devzendo.shell.plugin.VariablesShellPlugin;
import org.devzendo.shell.plugin.ExperimentalShellPlugin;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiRenderer;
import scala.Option;

public class ShellMain {
    private static final String SHELLPLUGIN_PROPERTIES = "shellplugin.properties";
    public static final Logger LOGGER = Logger.getLogger(ShellMain.class);

    public boolean quit;

    private final List<String> mArgList;
    private final PluginRegistry mPluginRegistry;
    private final CommandRegistry mCommandRegistry;
    private final VariableRegistry mVariableRegistry;

    public ShellMain(final List<String> argList) {
        mArgList = argList;
        mCommandRegistry = new CommandRegistry();
        mVariableRegistry = new VariableRegistry();
        mPluginRegistry = new PluginRegistry(SHELLPLUGIN_PROPERTIES, mCommandRegistry, mVariableRegistry, argList);
    }

    public class InternalShellPlugin implements ShellPlugin {
        @Override
        public String getName() {
            return "Internal";
        }
        
        public void quit() {
            quit = true;
        }

        @Override
        public void initialise(ExecutionEnvironment env) {
            // do nothing
        }
    }

    private void start() {
        LOGGER.debug("Starting DevZendo.org shell");
        for (final String arg : mArgList) {
            LOGGER.debug("ARG: [" + arg + "]");
        }

        final PrefsLocation prefsLocation = new DefaultPrefsLocation(".shell", "prefs.ini");
        final File historyFile = new File(prefsLocation.getPrefsDir(), "history.txt");

        // if isatty... {
        AnsiConsole.systemInstall();
        banner();
        // }
        final CompletionHandler completionHandler = new CompletionHandler() {
            @Override
            public boolean complete(final ConsoleReader reader, final List<CharSequence> candidates, final int position) throws IOException {
                return false;
            }
        };
        final LineReader lineReader = new JLineLineReader(historyFile, completionHandler);

        try {
            mPluginRegistry.loadAndRegisterPluginMethods(
                    new InternalShellPlugin(),
                    new VariablesShellPlugin(),
                    new PluginsShellPlugin(),
                    new LoggingShellPlugin(),
                    new ExperimentalShellPlugin()
                );
            
            final CommandParser parser = new CommandParser();
            final CommandHandlerWirer wirer = new CommandHandlerWirer(mCommandRegistry, mVariableRegistry);
            while (!quit) {
                final Option<String> input = lineReader.readLine("] ");
                LOGGER.debug("input: [" + input + "]");
                if (input.isDefined()) {
                    try {
                        final CommandPipeline commandPipeline = parser.parse(input.get().trim());
                        if (!commandPipeline.isEmpty()) {
                            final List<CommandHandler> commandHandlers = wirer.wire(commandPipeline);
                            if (LOGGER.isDebugEnabled()) {
                                dumpHandlers(commandHandlers);
                            }
                            final ExecutionContainer executionContainer = new ExecutionContainer(commandHandlers);
                            executionContainer.execute();
                        }
                    } catch (final CommandParserException cpe) {
                        LOGGER.warn(cpe.getMessage());
                    } catch (final CommandNotFoundException cnfe) {
                        LOGGER.warn(cnfe.getMessage());
                    } catch (final CommandExecutionException cee) {
                        LOGGER.warn(cee.getMessage());
                    }
                }
            }
        } catch (final ShellPluginException e) {
            LOGGER.fatal("Can't continue: " + e.getMessage());
        }
    }

    private void banner() {
        final List<String> banner = Arrays.asList(
                " __ _          _ _",
                "/ _\\ |__   ___| | |",
                "\\ \\| '_ \\ / _ \\ | |",
                "_\\ \\ | | |  __/ | |",
                "\\__/_| |_|\\___|_|_|");
        for (String line : banner) {
            AnsiConsole.out().println(AnsiRenderer.render("@|bold,blue " + line + "|@"));
        }
    }

    private void dumpHandlers(List<CommandHandler> commandHandlers) {
        final StringBuilder sb = new StringBuilder();
        for (CommandHandler handler: commandHandlers) {
            sb.append("<");
            final InputPipe inputPipe = handler.getInputPipe();
            sb.append(inputPipe.getClass().getSimpleName());
            if (inputPipe instanceof VariableInputPipe) {
                sb.append("(");
                sb.append(((VariableInputPipe)inputPipe).getVariable());
                sb.append(")");
            }
            
            sb.append(" [");
            
            sb.append(handler.getName());
            
            sb.append("] >");
            final OutputPipe outputPipe = handler.getOutputPipe();
            sb.append(outputPipe.getClass().getSimpleName());
            if (outputPipe instanceof VariableOutputPipe) {
                sb.append("(");
                sb.append(((VariableOutputPipe)outputPipe).getVariable());
                sb.append(")");
            }
            
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() -1);
        LOGGER.debug("pipeline: " + sb.toString());
    }

    public static void main(final String[] args) {
        final Logging logging = Logging.getInstance();
        final List<String> finalArgList = logging.setupLoggingFromArgs(Arrays
                .asList(args));
        new ShellMain(finalArgList).start();
    }
}
