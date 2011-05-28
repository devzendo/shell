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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class CommandHandlerFactory {
    /**
     * Create a CommandHandler that adapts a Method to a CommandHandler.
     * 
     * @param plugin the plugin instance containing the method to adapt to a
     * CommandHandler
     * @param method the method to adapt, this has already been checked by the
     * PluginMethodScanner to conform to the CommandHandler pattern, i.e.
     * optional List<Object> for args, optional InputPipe, optional OutputPipe,
     * no other args, void return type.
     * @return the CommandHandler
     */
    public CommandHandler createHandler(final ShellPlugin plugin, final Method method) {
        final CommandHandler handler = new CommandHandler(method.getName()) {
            @Override
            public void execute() throws CommandExecutionException {
                try {
                    final ArrayList<Object> argsList = new ArrayList<Object>();
                    // Scan method, and put args, input pipe, output pipe in the
                    // correct position in argsList before invocation, if this
                    // method requires them.
                    method.invoke(plugin, argsList.toArray());
                } catch (final IllegalArgumentException e) {
                    throw new CommandExecutionException("Illegal arguments: " + e.getMessage());
                } catch (final IllegalAccessException e) {
                    throw new CommandExecutionException("Illegal acces: " + e.getMessage());
                } catch (final InvocationTargetException e) {
                    throw new CommandExecutionException("Invocation target exception: " + e.getMessage());
                }
            }};
        return handler;
    }
}
