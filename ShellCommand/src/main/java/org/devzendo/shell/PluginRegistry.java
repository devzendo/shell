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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class PluginRegistry implements IPluginRegistry {
    private final String mPropertiesResourcePath;
    private final CommandRegistry mCommandRegistry;
    private final VariableRegistry mVariableRegistry;
    public List<String> mArgList;
    private final PluginMethodScanner mPluginMethodScanner;
    private Set<ShellPlugin> mPlugins = new HashSet<ShellPlugin>();

    public PluginRegistry(
            final String propertiesResourcePath, 
            final CommandRegistry commandRegistry, 
            final VariableRegistry variableRegistry, 
            final List<String> argList) {
        mArgList = argList;
        mPluginMethodScanner = new PluginMethodScanner();
        mCommandRegistry = commandRegistry;
        mVariableRegistry = variableRegistry;
        mPropertiesResourcePath = propertiesResourcePath;
    }

    @Override
    public void loadAndRegisterPluginMethods(final List<ShellPlugin> staticPlugins) throws ShellPluginException {
        final ExecutionEnvironment env = new ExecutionEnvironment(mArgList, mCommandRegistry, mVariableRegistry, this);
        final ArrayList<ShellPlugin> allPlugins = loadAllPlugins(staticPlugins);
        for (final ShellPlugin shellPlugin : allPlugins) {
            mPlugins.add(shellPlugin);
            shellPlugin.initialise(env);
            final Map<String, AnalysedMethod> nameMethodMap = mPluginMethodScanner.scanPluginMethods(shellPlugin);
            for (final Entry<String, AnalysedMethod> entry : nameMethodMap.entrySet()) {
                try {
                    mCommandRegistry.registerCommand(entry.getKey(), shellPlugin, entry.getValue());
                } catch (DuplicateCommandException e) {
                    throw new ShellPluginException(e.getMessage());
                }
            }
        }
    }

    private ArrayList<ShellPlugin> loadAllPlugins(final List<ShellPlugin> staticPlugins) throws ShellPluginException {
        final ArrayList<ShellPlugin> allPlugins = new ArrayList<ShellPlugin>();
        final List<ShellPlugin> pluginsFromClasspath = new PluginLoader().loadPluginsFromClasspath(mPropertiesResourcePath);
        allPlugins.addAll(staticPlugins);
        allPlugins.addAll(pluginsFromClasspath);
        return allPlugins;
    }

    @Override
    public final Set<ShellPlugin> getPlugins() {
        return mPlugins;
    }
}