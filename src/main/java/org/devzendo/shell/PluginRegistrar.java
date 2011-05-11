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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PluginRegistrar {
    public List<String> mArgList;
    private final PluginMethodScanner mPluginMethodScanner;
    private final CommandRegistry mCommandRegistry;
    private final String mPropertiesResourcePath;

    public PluginRegistrar(final String propertiesResourcePath, final List<String> argList) {
        mArgList = argList;
        mPluginMethodScanner = new PluginMethodScanner();
        mCommandRegistry = new CommandRegistry();
        mPropertiesResourcePath = propertiesResourcePath;
    }

    public CommandRegistry loadAndRegisterPluginMethods(final ShellPlugin ... staticPlugins) throws ShellPluginException {
        final ArrayList<ShellPlugin> allPlugins = loadAllPlugins(staticPlugins);
        for (final ShellPlugin shellPlugin : allPlugins) {
            shellPlugin.processCommandLine(mArgList);
            final Map<String, Method> nameMethodMap = mPluginMethodScanner.scanPluginMethods(shellPlugin);
            for (final Entry<String, Method> entry : nameMethodMap.entrySet()) {
                mCommandRegistry.registerCommand(entry.getKey(), shellPlugin, entry.getValue());
            }
        }
        return mCommandRegistry;
    }

    private ArrayList<ShellPlugin> loadAllPlugins(final ShellPlugin ... staticPlugins) throws ShellPluginException {
        final ArrayList<ShellPlugin> allPlugins = new ArrayList<ShellPlugin>();
        final List<ShellPlugin> pluginsFromClasspath = new PluginLoader().loadPluginsFromClasspath(mPropertiesResourcePath);
        allPlugins.addAll(Arrays.asList(staticPlugins));
        allPlugins.addAll(pluginsFromClasspath);
        return allPlugins;
    }
}