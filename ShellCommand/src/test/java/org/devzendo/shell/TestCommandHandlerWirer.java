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

import org.devzendo.shell.pipe.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class TestCommandHandlerWirer {
    final CommandRegistry commandRegistry = new CommandRegistry();
    final VariableRegistry variableRegistry = new VariableRegistry();
    final CommandHandlerWirer wirer = new CommandHandlerWirer(commandRegistry, variableRegistry);
    final CommandPipeline pipeline = new CommandPipeline();
    private AnalysedMethod mAnalysedMethod;
    
    @Before
    public void setUpAnalysedMethod() throws SecurityException, NoSuchMethodException {
        mAnalysedMethod = analyseMethodNamed("commandHandlerWithBothPipes");
    }
    
    public void commandHandlerWithBothPipes(final InputPipe inputPipe, final OutputPipe outputPipe) {
        // do nothing
    }

    @Test
    public void noPipelineInputOrOutput() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
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
    public void pipeBetweenCommandHandlers() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        commandRegistry.registerCommand("bar", null, mAnalysedMethod);

        final Command fooCommand = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(fooCommand);
        final Command barCommand = new Command("bar", Collections.EMPTY_LIST);
        pipeline.addCommand(barCommand);
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler fooCommandHandler = handlers.get(0);
        assertThat(fooCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(fooCommandHandler.getOutputPipe(), instanceOf(RendezvousPipe.class));
        
        CommandHandler barCommandHandler = handlers.get(1);
        assertThat(barCommandHandler.getInputPipe(), instanceOf(RendezvousPipe.class));
        assertThat(barCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }
    
    @Test
    public void inputToPipelineFromVariable() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setInputVariable(new VariableReference("var")); // variable registry autocreates
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.get(0);
        assertThat(commandHandler.getInputPipe(), instanceOf(VariableInputPipe.class));
    }

    @Test
    public void outputFromPipelineToVariable() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Collections.EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setOutputVariable(new VariableReference("var")); // variable registry autocreates
        final List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.get(0);
        assertThat(commandHandler.getOutputPipe(), instanceOf(VariableOutputPipe.class));
    }
    
    public void rightNoInputPipe(final OutputPipe outputPipe) {
        // do nothing
    }

    /**
     * left | right
     * right has no input pipe in its command method, so can't receive left's
     * output. Therefore, left's output must be discarded.
     * @throws DuplicateCommandException 
     * @throws CommandNotFoundException 
     */
    @Test
    public void rightCommandWithNoInputConnectedToLeftCommandViaDiscardPipe() throws DuplicateCommandException, CommandNotFoundException {
        registerMethodAsCommandAndAddToPipeline("commandHandlerWithBothPipes");
        registerMethodAsCommandAndAddToPipeline("rightNoInputPipe");
        
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler leftCommandHandler = handlers.get(0);
        assertThat(leftCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(leftCommandHandler.getOutputPipe(), instanceOf(NullOutputPipe.class));
        
        CommandHandler rightCommandHandler = handlers.get(1);
        assertThat(rightCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(rightCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }

    public void leftNoOutputPipe(final InputPipe inputPipe) {
        // do nothing
    }

    /**
     * left | right
     * left has no output pipe in its command method, so can't send left's
     * input. Therefore, right's input must be immediately empty.
     * @throws DuplicateCommandException 
     * @throws CommandNotFoundException 
     */
    @Test
    public void leftCommandWithNoOutputConnectedToRightCommandViaEmptyPipe() throws DuplicateCommandException, CommandNotFoundException {
        registerMethodAsCommandAndAddToPipeline("leftNoOutputPipe");
        registerMethodAsCommandAndAddToPipeline("commandHandlerWithBothPipes");
        
        List<CommandHandler> handlers = wirer.wire(pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler leftCommandHandler = handlers.get(0);
        assertThat(leftCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(leftCommandHandler.getOutputPipe(), instanceOf(NullOutputPipe.class));
        
        CommandHandler rightCommandHandler = handlers.get(1);
        assertThat(rightCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(rightCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }

    @Test
    public void noArgsInCommandYieldsEmptyListPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
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
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
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

    @SuppressWarnings("unchecked")
    private void registerMethodAsCommandAndAddToPipeline(final String methodName)
            throws DuplicateCommandException {
        commandRegistry.registerCommand(methodName, null, analyseMethodNamed(methodName));
        pipeline.addCommand(new Command(methodName, Collections.EMPTY_LIST));
    }

    private AnalysedMethod analyseMethodNamed(final String methodName) {
        for (Method method : this.getClass().getMethods()) {
            if (method.getName().equals(methodName)) {
                return analyseMethod(method);
            }
        }
        Assert.fail("Could not find a command handler method called " + methodName);
        return null;
    }
    
    private AnalysedMethod analyseMethod(Method method) {
        return (AnalysedMethod) new MethodAnalyser().analyseMethod(method).get();
    }
}
