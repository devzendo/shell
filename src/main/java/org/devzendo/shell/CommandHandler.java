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

import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;

public abstract class CommandHandler {
    private InputPipe mInputPipe;
    private OutputPipe mOutputPipe;
    private List<Object> mArgs;
    private final String mName;
    
    /**
     * @param name the command name that this handler will execute
     */
    public CommandHandler(String name) {
        mName = name;
    }

    public final String getName() {
        return mName;
    }

    public final void setInputPipe(InputPipe inputPipe) {
        mInputPipe = inputPipe;
    }

    public final void setOutputPipe(OutputPipe outputPipe) {
        mOutputPipe = outputPipe;
    }

    public final void setArgs(List<Object> args) {
        mArgs = args;
    }

    public final InputPipe getInputPipe() {
        return mInputPipe;
    }

    public final OutputPipe getOutputPipe() {
        return mOutputPipe;
    }

    public final List<Object> getArgs() {
        return mArgs;
    }
    
    public abstract void execute() throws CommandExecutionException;
}
