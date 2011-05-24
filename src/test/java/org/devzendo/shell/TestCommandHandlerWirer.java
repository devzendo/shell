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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Collections;
import java.util.List;

import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.LogInfoOutputPipe;
import org.devzendo.shell.pipe.NullInputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.devzendo.shell.pipe.RendezvousPipe;
import org.devzendo.shell.pipe.VariableInputPipe;
import org.devzendo.shell.pipe.VariableOutputPipe;
import org.junit.Test;


public class TestCommandHandlerWirer {
    final CommandRegistry commandRegistry = new CommandRegistry();
    final VariableRegistry variableRegistry = new VariableRegistry();
    final CommandHandlerWirer wirer = new CommandHandlerWirer(commandRegistry, variableRegistry);
    final CommandPipeline pipeline = new CommandPipeline();

    @Test
    public void noPipelineInputOrOutput() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, null);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.get(0);
        assertThat(commandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(commandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }



    @SuppressWarnings("unchecked")
    @Test
    public void pipeBetweenCommandHandlers() throws SecurityException, NoSuchMethodException, DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, null);
        commandRegistry.registerCommand("bar", null, null);

        final Command fooCommand = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(fooCommand);
        final Command barCommand = new Command("bar", Collections.EMPTY_LIST);
        pipeline.addCommand(barCommand);
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler fooCommandHandler = handlers.get(0);
        assertThat(fooCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(fooCommandHandler.getOutputPipe(), instanceOf(OutputPipe.class));
        assertThat(fooCommandHandler.getOutputPipe(), instanceOf(RendezvousPipe.class));
        
        CommandHandler barCommandHandler = handlers.get(1);
        assertThat(barCommandHandler.getInputPipe(), instanceOf(InputPipe.class));
        assertThat(barCommandHandler.getInputPipe(), instanceOf(RendezvousPipe.class));
        assertThat(barCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }
    
    @Test
    public void inputToPipelineFromVariable() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, null);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setInputVariable(new VariableReference("var")); // variable registry autocreates
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.get(0);
        assertThat(commandHandler.getInputPipe(), instanceOf(InputPipe.class));
        assertThat(commandHandler.getInputPipe(), instanceOf(VariableInputPipe.class));
    }

    @Test
    public void outputFromPipelineToVariable() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, null);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setOutputVariable(new VariableReference("var")); // variable registry autocreates
        final List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.get(0);
        assertThat(commandHandler.getOutputPipe(), instanceOf(OutputPipe.class));
        assertThat(commandHandler.getOutputPipe(), instanceOf(VariableOutputPipe.class));
    }

    @Test
    public void noArgsInCommandYieldsEmptyListPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, null);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        final List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.get(0);
        final List<Object> args = commandHandler.getArgs();
        assertThat(args.size(), equalTo(0));
    }
    
    @Test
    public void argsPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, null);
        final List<Object> inputArgs = asList(new Object[] { (Integer)5, "hello"});
        final Command command = new Command("foo", inputArgs);
        pipeline.addCommand(command);
        final List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.get(0);
        final List<Object> args = commandHandler.getArgs();
        assertThat(args.size(), equalTo(2));
        assertThat((Integer) args.get(0), equalTo(5));
        assertThat((String) args.get(1), equalTo("hello"));
    }
    
    @Test(expected = CommandNotFoundException.class)
    public void commandNotFound() throws CommandNotFoundException {
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        wirer.wire(pipeline);
    }
}
