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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.devzendo.commoncode.logging.Logging;

public class ShellMain {
    private static final String SHELLPLUGIN_PROPERTIES = "shellplugin.properties";
    private static final Logger LOGGER = Logger.getLogger(ShellMain.class);

    public boolean quit;

    private final List<String> mArgList;
    private final PluginRegistrar mPluginRegistrar;

    public ShellMain(final List<String> argList) {
        mArgList = argList;
        mPluginRegistrar = new PluginRegistrar(SHELLPLUGIN_PROPERTIES, argList);
    }


    public class InternalShellPlugin implements ShellPlugin {
        public void processCommandLine(final List<String> args) {
            // do nothing
        }

        public String getName() {
            return "Internal";
        }
        
        public void quit() {
            quit = true;
        }
    }

    private void start() {
        LOGGER.debug("Starting DevZendo.org shell");
        for (final String arg : mArgList) {
            LOGGER.debug("ARG: [" + arg + "]");
        }
        
        try {
            final CommandRegistry commandRegistry = mPluginRegistrar.loadAndRegisterPluginMethods(
                    new InternalShellPlugin()
                );
            
            final CommandParser parser = new CommandParser();
            final CommandHandlerWirer wirer = new CommandHandlerWirer(commandRegistry);
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (!quit) {
                    System.out.print("] ");
                    System.out.flush();
                    final String input = br.readLine();
                    LOGGER.info("[" + input + "]");
                    final List<Command> commands = parser.parse(input.trim());
                    final List<CommandHandler> executors = wirer.wire(commands);
                    // not sure how to execute at the moment...
                }
            } catch (final IOException ioe) {
                LOGGER.error(ioe.getMessage());
            }
        } catch (final ShellPluginException e) {
            LOGGER.fatal("Can't continue: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        final Logging logging = Logging.getInstance();
        final List<String> finalArgList = logging.setupLoggingFromArgs(Arrays
                .asList(args));
        new ShellMain(finalArgList).start();
    }
}
