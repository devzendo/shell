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

import java.util.ArrayList;
import java.util.List;

public final class CommandPipeline {
    final List<Command> commands = new ArrayList<Command>();

    VariableReference inputVariable;

    VariableReference outputVariable;

    public VariableReference getInputVariable() {
        return inputVariable;
    }

    public void setInputVariable(final VariableReference inputVariable) {
        this.inputVariable = inputVariable;
    }

    public VariableReference getOutputVariable() {
        return outputVariable;
    }

    public void setOutputVariable(final VariableReference outputVariable) {
        this.outputVariable = outputVariable;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void addCommands(final List<Command> commandsToAdd) {
        for (Command command : commandsToAdd) {
            commands.add(command);
        }
    }

    public void addCommand(final Command commandToAdd) {
        commands.add(commandToAdd);
    }
}
