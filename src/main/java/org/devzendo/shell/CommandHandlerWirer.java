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
import java.util.List;

import org.devzendo.shell.pipe.LogInfoOutputPipe;
import org.devzendo.shell.pipe.NullInputPipe;
import org.devzendo.shell.pipe.RendezvousPipe;
import org.devzendo.shell.pipe.VariableInputPipe;
import org.devzendo.shell.pipe.VariableOutputPipe;

/**
 * Given a command pipeline, create a list of command handlers for each
 * command in the pipeline, and wire up the input and output pipes between
 * them, also, connect the pipeline input and outputs.
 * 
 * @author matt
 *
 */
public class CommandHandlerWirer {
    private final CommandRegistry mCommandRegistry;
    private final VariableRegistry mVariableRegistry;

    public CommandHandlerWirer(final CommandRegistry commandRegistry, final VariableRegistry variableRegistry) {
        mCommandRegistry = commandRegistry;
        mVariableRegistry = variableRegistry;
    }

    public List<CommandHandler> wire(final CommandPipeline commandPipeline) throws CommandNotFoundException {
        final ArrayList<CommandHandler> handlers = new ArrayList<CommandHandler>();
        final List<Command> commands = commandPipeline.getCommands();
        assert commands.size() > 0;
        for (Command command: commands) {
            CommandHandler handler = mCommandRegistry.getHandler(command.getName());
            handler.setArgs(command.getArgs());
            handlers.add(handler);
        }
        // cat /dev/null > first, unless storing in a variable
        final VariableReference pipelineInputVariable = commandPipeline.getInputVariable();
        if (pipelineInputVariable == null) {
            handlers.get(0).setInputPipe(new NullInputPipe());
        } else {
            final Variable inputVariable = mVariableRegistry.getVariable(pipelineInputVariable);
            handlers.get(0).setInputPipe(new VariableInputPipe(inputVariable));
        }
        // last | echo, unless storing in a variable
        final VariableReference pipelineOutputVariable = commandPipeline.getOutputVariable();
        if (pipelineOutputVariable == null) {
            handlers.get(handlers.size() - 1).setOutputPipe(new LogInfoOutputPipe());
        } else {
            final Variable outputVariable = mVariableRegistry.getVariable(pipelineOutputVariable);
            handlers.get(0).setOutputPipe(new VariableOutputPipe(outputVariable));
        }
        for (int i = 0; i < handlers.size() - 1; i++) {
            // left | right
            CommandHandler left = handlers.get(i);
            CommandHandler right = handlers.get(i + 1);
            // In cases where left has output, and right has input:
            connectByRendezvousPipe(left, right);
        }
        return handlers;
    }

    private void connectByRendezvousPipe(final CommandHandler left, final CommandHandler right) {
        final RendezvousPipe pipe = new RendezvousPipe();
        left.setOutputPipe(pipe);
        right.setInputPipe(pipe);
    }
}
