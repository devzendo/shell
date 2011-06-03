/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devzendo.shell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

    private class PluginMethod {
        private final ShellPlugin mShellPlugin;
        private final AnalysedMethod mAnalysedMethod;

        public PluginMethod(final ShellPlugin plugin, final AnalysedMethod analysedMethod) {
            mShellPlugin = plugin;
            mAnalysedMethod = analysedMethod;
        }

        public final ShellPlugin getShellPlugin() {
            return mShellPlugin;
        }

        public final AnalysedMethod getAnalysedMethod() {
            return mAnalysedMethod;
        }
    }
    private final Map<String, PluginMethod> nameToPluginMethod = new HashMap<String, PluginMethod>();
    
    public void registerCommand(final String name, final ShellPlugin plugin, final AnalysedMethod analysedMethod) throws DuplicateCommandException {
        final PluginMethod pluginMethod = nameToPluginMethod.get(name);
        if (pluginMethod != null) {
            throw new DuplicateCommandException("Command '" + name + "' from plugin '"
                + plugin.getName() + "' is duplicated; initially declared in plugin '" 
                + pluginMethod.getShellPlugin().getName() + "'");

        }
        nameToPluginMethod.put(name, new PluginMethod(plugin, analysedMethod));
    }

    public CommandHandler getHandler(final String name) throws CommandNotFoundException {
        final PluginMethod pluginMethod = nameToPluginMethod.get(name);
        if (pluginMethod == null) {
            throw new CommandNotFoundException("'" + name + "' not found");
        }
        return new CommandHandler(name) {
            @Override
            public void execute() throws CommandExecutionException {
                try {
                    pluginMethod.getAnalysedMethod().getMethod().invoke(pluginMethod.getShellPlugin(), getArgs());
                } catch (final IllegalArgumentException e) {
                    throw new CommandExecutionException("Illegal arguments: " + e.getMessage());
                } catch (final IllegalAccessException e) {
                    throw new CommandExecutionException("Illegal acces: " + e.getMessage());
                } catch (final InvocationTargetException e) {
                    throw new CommandExecutionException("Invocation target exception: " + e.getMessage());
                }
            }
        };
    }

    
}
