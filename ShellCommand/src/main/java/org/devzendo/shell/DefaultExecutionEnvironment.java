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

import java.util.List;

public class DefaultExecutionEnvironment implements ExecutionEnvironment {
    private final List<String> mArgList;
    private final CommandRegistry mCommandRegistry;
    private final VariableRegistry mVariableRegistry;
    private final PluginRegistry mPluginRegistry;

    public DefaultExecutionEnvironment(
            final List<String> argList,
            final CommandRegistry commandRegistry,
            final VariableRegistry variableRegistry,
            final PluginRegistry pluginRegistry) {
                mArgList = argList;
                mCommandRegistry = commandRegistry;
                mVariableRegistry = variableRegistry;
                mPluginRegistry = pluginRegistry;
    }

    @Override
    public final List<String> getArgList() {
        return mArgList;
    }

    @Override
    public final CommandRegistry getCommandRegistry() {
        return mCommandRegistry;
    }

    @Override
    public final VariableRegistry getVariableRegistry() {
        return mVariableRegistry;
    }

    @Override
    public final PluginRegistry getPluginRegistry() {
        return mPluginRegistry;
    }
}

