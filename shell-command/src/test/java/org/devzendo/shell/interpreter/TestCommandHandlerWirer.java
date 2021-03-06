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
package org.devzendo.shell.interpreter;

import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.ast.*;
import org.devzendo.shell.pipe.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Option;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

public class TestCommandHandlerWirer {
    private static final scala.Option<Integer> noneInteger = scala.Option.apply(null);
    private static final scala.Option<VariableRegistry> noneVariableRegistry = scala.Option.apply(null);
    private static final scala.Option<CommandHandler> noneCommandHandler = scala.Option.apply(null);
    final CommandRegistry commandRegistry = new CommandRegistry();
    final VariableRegistry variableRegistry = new DefaultVariableRegistry(noneVariableRegistry);
    final CommandHandlerWirer wirer = new CommandHandlerWirer(commandRegistry);
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
    public void emptyPipeLineYieldsSaneCommandHandlers() throws DuplicateCommandException, CommandNotFoundException {
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(0));
    }

    @Test
    public void verboseFlagDisabledByDefault() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getVerbose(), equalTo(false));
        assertThat(commandHandler.getLog().isVerboseEnabled(), equalTo(false));
    }

    @Test
    public void verboseFlagEnabledAndVerboseSwitchRemovedFromArgs() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", Arrays.<Object>asList("One", new Switch("Two"), new Switch("Verbose"), new Switch("Three")));
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getVerbose(), equalTo(true));
        assertThat(commandHandler.getLog().isVerboseEnabled(), equalTo(true));

        final scala.collection.immutable.List<Object> args = commandHandler.getArgs();
        assertThat(args.size(), equalTo(3));
        assertThat(args.apply(0), instanceOf(String.class));
        assertThat(args.apply(0).toString(), equalTo("One"));
        assertThat(args.apply(1), instanceOf(Switch.class));
        assertThat((Switch) args.apply(1), equalTo(new Switch("Two")));
        assertThat(args.apply(2), instanceOf(Switch.class));
        assertThat((Switch) args.apply(2), equalTo(new Switch("Three")));

        final scala.collection.immutable.List<Option<CommandHandler>> subCommandHandlers = commandHandler.getSubCommandHandlers();
        assertThat(subCommandHandlers.size(), equalTo(3));
    }

    @Test
    public void noPipelineInputOrOutput() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(commandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pipeBetweenCommandHandlers() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        commandRegistry.registerCommand("bar", null, mAnalysedMethod);

        final Command fooCommand = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(fooCommand);
        final Command barCommand = new Command("bar", EMPTY_LIST);
        pipeline.addCommand(barCommand);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler fooCommandHandler = handlers.apply(0);
        assertThat(fooCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(fooCommandHandler.getOutputPipe(), instanceOf(RendezvousPipe.class));
        
        CommandHandler barCommandHandler = handlers.apply(1);
        assertThat(barCommandHandler.getInputPipe(), instanceOf(RendezvousPipe.class));
        assertThat(barCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }
    
    @Test
    public void inputToPipelineFromVariable() throws DuplicateCommandException, CommandNotFoundException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setInputVariable(new VariableReference("var")); // variable registry autocreates
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getInputPipe(), instanceOf(VariableInputPipe.class));
    }

    @Test
    public void outputFromPipelineToVariable() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        pipeline.setOutputVariable(new VariableReference("var")); // variable registry autocreates
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.apply(0);
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

        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler leftCommandHandler = handlers.apply(0);
        assertThat(leftCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(leftCommandHandler.getOutputPipe(), instanceOf(NullOutputPipe.class));
        
        CommandHandler rightCommandHandler = handlers.apply(1);
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

        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(2));

        CommandHandler leftCommandHandler = handlers.apply(0);
        assertThat(leftCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(leftCommandHandler.getOutputPipe(), instanceOf(NullOutputPipe.class));
        
        CommandHandler rightCommandHandler = handlers.apply(1);
        assertThat(rightCommandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(rightCommandHandler.getOutputPipe(), instanceOf(LogInfoOutputPipe.class));
    }

    @Test
    public void noArgsInCommandYieldsEmptyListPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getArgs().size(), equalTo(0));
        assertThat(commandHandler.getSubCommandHandlers().size(), equalTo(0));
    }


    private CommandHandler addCommandWithSimpleArgs() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        final List<Object> inputArgs = asList(new Object[] { 5, "hello"});
        final Command command = new Command("foo", inputArgs);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        return handlers.apply(0);
    }

    @Test
    public void argsPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final CommandHandler commandHandler = addCommandWithSimpleArgs();

        final scala.collection.immutable.List<Object> args = commandHandler.getArgs();
        assertThat(args.size(), equalTo(2));
        assertThat((Integer) args.apply(0), equalTo(5));
        assertThat((String) args.apply(1), equalTo("hello"));
    }

    @Test
    public void nonCommandArgsGenerateNoneSubCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final CommandHandler commandHandler = addCommandWithSimpleArgs();

        final scala.collection.immutable.List<Option<CommandHandler>> subCommandHandlers = commandHandler.getSubCommandHandlers();
        assertThat(subCommandHandlers.size(), equalTo(2));
        assertThat(subCommandHandlers.apply(0), equalTo(noneCommandHandler));
        assertThat(subCommandHandlers.apply(1), equalTo(noneCommandHandler));
    }


    @Test
    public void subCommandArgsGenerateSomeSubCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("+", null, mAnalysedMethod);
        commandRegistry.registerCommand("main", null, mAnalysedMethod);
        final Command subCommand = new Command("+", Arrays.<Object>asList(1, 2));
        final Command mainCommand = new Command("main", Arrays.<Object>asList(subCommand));
        pipeline.addCommand(mainCommand);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler handler = handlers.apply(0);
        final scala.collection.immutable.List<Object> handlerArgs = handler.getArgs();
        assertThat(handlerArgs.size(), equalTo(1));
        assertThat(handlerArgs.apply(0), instanceOf(Variable.class));

        final scala.collection.immutable.List<Option<CommandHandler>> subCommandHandlers = handler.getSubCommandHandlers();
        assertThat(subCommandHandlers.size(), equalTo(1));
        final Option<CommandHandler> plusHandlerOption = subCommandHandlers.apply(0);
        assertThat(plusHandlerOption, not(equalTo(noneCommandHandler)));

        final CommandHandler plusHandler = plusHandlerOption.get();
        assertThat(plusHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(plusHandler.getOutputPipe(), instanceOf(AnonymousVariablePipe.class));

    }


    @Test(expected = CommandNotFoundException.class)
    public void commandNotFound() throws CommandNotFoundException {
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        wirer.wireCommandPipeline(variableRegistry, pipeline);
    }

    @Test
    public void variableRegistryPassedToCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        commandRegistry.registerCommand("foo", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command command = new Command("foo", EMPTY_LIST);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getVariableRegistry(), equalTo(variableRegistry));
        assertTrue(commandHandler.getVariableRegistry().getParentScope().isEmpty());
    }

    @Test
    public void childVariableRegistryPassedToBlockStatementsCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();
        final CommandHandler commandHandler = wirer.wireBlockStatements(variableRegistry, blockStatements);
        assertThat(commandHandler.getVariableRegistry(), not(equalTo(variableRegistry))); // some new child
        assertTrue(commandHandler.getVariableRegistry().getParentScope().isDefined());
        assertThat(commandHandler.getVariableRegistry().getParentScope().get(), equalTo(variableRegistry));
    }

    @Test
    public void nullPipesAttachedToBlockStatementsCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();
        final CommandHandler commandHandler = wirer.wireBlockStatements(variableRegistry, blockStatements);
        assertThat(commandHandler.getInputPipe(), instanceOf(NullInputPipe.class));
        assertThat(commandHandler.getOutputPipe(), instanceOf(NullOutputPipe.class));
    }

    @Test
    public void noArgumentsPassedToBlockStatementsCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();
        final CommandHandler commandHandler = wirer.wireBlockStatements(variableRegistry, blockStatements);
        assertThat(commandHandler.getArgs().size(), equalTo(0));
    }

    @Test
    public void nonVerboseLogPassedToBlockStatementsCommandHandlers() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();
        final CommandHandler commandHandler = wirer.wireBlockStatements(variableRegistry, blockStatements);
        assertThat(commandHandler.getLog().isVerboseEnabled(), equalTo(false));
    }

    @Test
    public void internalCommandHandlerUsedToExecuteBlockStatements() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();
        final CommandHandler commandHandler = wirer.wireBlockStatements(variableRegistry, blockStatements);
        assertThat(commandHandler.getName(), equalTo("<block>"));
        // weak, < > used to indicate internal command handlers,
        // rather than "proper" command handlers.
        assertThat(commandHandler.argumentsPos(), equalTo(noneInteger));
        assertThat(commandHandler.inputPipePos(), equalTo(noneInteger));
        assertThat(commandHandler.outputPipePos(), equalTo(noneInteger));
        assertThat(commandHandler.logPos(), equalTo(noneInteger));
    }

    private BlockStatements createSampleBlockStatements() throws DuplicateCommandException {
        commandRegistry.registerCommand("block", null, mAnalysedMethod);
        @SuppressWarnings("unchecked")
        final Command blockCommand = new Command("block", EMPTY_LIST);
        final CommandPipeline blockCommandPipeline = new CommandPipeline();
        blockCommandPipeline.addCommand(blockCommand);
        final BlockStatements blockStatements = new BlockStatements();
        blockStatements.setStatements(ScalaListHelper.createList((Statement) blockCommandPipeline));
        return blockStatements;
    }

    @Test
    public void blockStatementsExecuteOnCurrentThread() throws CommandExecutionException {
        final TestExecutionContainer.TestCommandHandler testCommandHandler = new TestExecutionContainer.TestCommandHandler("one");
        final scala.collection.immutable.List<CommandHandler> handlers = ScalaListHelper.createList((CommandHandler) testCommandHandler);
        final scala.collection.immutable.List<scala.collection.immutable.List<CommandHandler>> handlerLists = ScalaListHelper.createList(handlers);
        final SequentialCommandHandler sequentialCommandHandler = new SequentialCommandHandler(handlerLists);

        sequentialCommandHandler.execute();

        assertThat(testCommandHandler.getExecuteThread(), equalTo(Thread.currentThread()));
    }

    @Test
    public void blockStatementsCanBeArgumentsAndHaveChildVariableRegistries() throws CommandNotFoundException, DuplicateCommandException {
        final BlockStatements blockStatements = createSampleBlockStatements();

        commandRegistry.registerCommand("command", null, mAnalysedMethod);
        final List<Object> blockStatementsList = asList(new Object[] {blockStatements});
        @SuppressWarnings("unchecked")
        final Command command = new Command("command", blockStatementsList);
        pipeline.addCommand(command);
        scala.collection.immutable.List<CommandHandler> handlers = wirer.wireCommandPipeline(variableRegistry, pipeline);
        assertThat(handlers.size(), equalTo(1));
        final CommandHandler commandHandler = handlers.apply(0);
        assertThat(commandHandler.getVariableRegistry(), equalTo(variableRegistry)); // global

        final CommandHandler blockCommandHandler = (CommandHandler) commandHandler.getArgs().apply(0);
        assertThat(blockCommandHandler.getName(), equalTo("<block>"));
        // weak, < > used to indicate internal command handlers,
        // rather than "proper" command handlers.
        assertThat(blockCommandHandler.getVariableRegistry(), not(equalTo(variableRegistry)));
        assertThat(blockCommandHandler.getVariableRegistry().getParentScope().get(), equalTo(variableRegistry));

        assertThat(blockCommandHandler.argumentsPos(), equalTo(noneInteger));
        assertThat(blockCommandHandler.inputPipePos(), equalTo(noneInteger));
        assertThat(blockCommandHandler.outputPipePos(), equalTo(noneInteger));
        assertThat(blockCommandHandler.logPos(), equalTo(noneInteger));
    }


    @SuppressWarnings("unchecked")
    private void registerMethodAsCommandAndAddToPipeline(final String methodName)
            throws DuplicateCommandException {
        commandRegistry.registerCommand(methodName, null, analyseMethodNamed(methodName));
        pipeline.addCommand(new Command(methodName, EMPTY_LIST));
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
        return new MethodAnalyser().analyseMethod(method).get();
    }
}
