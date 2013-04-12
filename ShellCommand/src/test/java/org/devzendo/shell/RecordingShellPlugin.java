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

import org.devzendo.shell.plugin.ShellPlugin;

public class RecordingShellPlugin implements ShellPlugin {
    private scala.collection.immutable.List<String> mArgs;
    private VariableRegistry mVariableRegistry;
    private CommandRegistry mCommandRegistry;

    public final scala.collection.immutable.List<String> getArgs() {
        return mArgs;
    }

    public String getName() {
        return "Recording";
    }

    public CommandRegistry getCommandRegistry() {
        return mCommandRegistry;
    }

    public VariableRegistry getVariableRegistry() {
        return mVariableRegistry;
    }

    @Override
    public void initialise(final ExecutionEnvironment env) {
        mArgs = env.argList();
        mCommandRegistry = env.commandRegistry();
        mVariableRegistry = env.variableRegistry();
    }
}
