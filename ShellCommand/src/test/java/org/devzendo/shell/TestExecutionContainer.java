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

import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.devzendo.shell.pipe.Pipe;
import org.junit.Test;
import scala.Option;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

public class TestExecutionContainer {
    private static final Option<Integer> none = Option.apply(null);

    final class TestCommandHandler extends CommandHandler {
        private Thread mCurrentThread;
        private CommandExecutionException mCommandExecutionException;

        public TestCommandHandler(String name) {
            super(name, none, none, none);
        }

        @Override
        public void execute() throws CommandExecutionException {
            if (mCommandExecutionException != null) {
                throw mCommandExecutionException;
            }
                
            mCurrentThread = Thread.currentThread();
        }

        public final Thread getExecuteThread() {
            return mCurrentThread;
        }

        public void injectCommandFailure() {
            mCommandExecutionException = new CommandExecutionException("fail!!");
        }
    }
    
    @Test
    public void singleCommandExecutesOnCurrentThread() throws CommandExecutionException {
        final TestCommandHandler testCommandHandler = new TestCommandHandler("one");
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandler);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        executionContainer.execute();
        
        assertThat(testCommandHandler.getExecuteThread(), equalTo(Thread.currentThread()));
    }

    private class TerminationRecordingPipe implements Pipe {
        private boolean mTerminated = false;

        @Override
        public void setTerminated() {
            mTerminated = true;
        }

        public final boolean isTerminated() {
            return mTerminated;
        }
    }
 
    private class TerminationRecordingOutputPipe extends TerminationRecordingPipe implements OutputPipe {
        @Override
        public void push(final Object object) {
        }
    }

    private class TerminationRecordingInputPipe extends TerminationRecordingPipe implements InputPipe {
        @Override
        public Option<Object> next() {
            return scala.Option.apply(null);
        }
    }
    
    @Test
    public void pipesAreTerminatedAfterExecutionOfSingleCommand() throws CommandExecutionException {
        final TestCommandHandler testCommandHandler = new TestCommandHandler("one");
        final TerminationRecordingInputPipe terminationRecordingInputPipe = new TerminationRecordingInputPipe();
        final TerminationRecordingOutputPipe terminationRecordingOutputPipe = new TerminationRecordingOutputPipe();
        testCommandHandler.setInputPipe(terminationRecordingInputPipe);
        testCommandHandler.setOutputPipe(terminationRecordingOutputPipe);
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandler);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        executionContainer.execute();
        
        assertThat(terminationRecordingInputPipe.isTerminated(), equalTo(true));
        assertThat(terminationRecordingOutputPipe.isTerminated(), equalTo(true));
    }

    @Test
    public void multipleCommandsExecuteOnSeparateThreads() throws CommandExecutionException {
        final TestCommandHandler testCommandHandlerOne = new TestCommandHandler("one");
        final TestCommandHandler testCommandHandlerTwo = new TestCommandHandler("two");
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandlerOne, testCommandHandlerTwo);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        executionContainer.execute();
        
        assertThat(testCommandHandlerOne.getExecuteThread(), not(equalTo(Thread.currentThread())));
        assertThat(testCommandHandlerTwo.getExecuteThread(), not(equalTo(Thread.currentThread())));
        assertThat(testCommandHandlerOne.getExecuteThread(), not(equalTo(testCommandHandlerTwo.getExecuteThread())));
    }
    
    @Test
    public void pipesAreTerminatedAfterExecutionOfMultipleCommands() throws CommandExecutionException {
        final TestCommandHandler testCommandHandlerOne = new TestCommandHandler("one");
        final TestCommandHandler testCommandHandlerTwo = new TestCommandHandler("two");
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandlerOne, testCommandHandlerTwo);
        final TerminationRecordingInputPipe terminationRecordingInputPipeOne = new TerminationRecordingInputPipe();
        final TerminationRecordingOutputPipe terminationRecordingOutputPipeOne = new TerminationRecordingOutputPipe();
        testCommandHandlerOne.setInputPipe(terminationRecordingInputPipeOne);
        testCommandHandlerOne.setOutputPipe(terminationRecordingOutputPipeOne);
        final TerminationRecordingInputPipe terminationRecordingInputPipeTwo = new TerminationRecordingInputPipe();
        final TerminationRecordingOutputPipe terminationRecordingOutputPipeTwo = new TerminationRecordingOutputPipe();
        testCommandHandlerTwo.setInputPipe(terminationRecordingInputPipeTwo);
        testCommandHandlerTwo.setOutputPipe(terminationRecordingOutputPipeTwo);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        executionContainer.execute();
        
        assertThat(terminationRecordingInputPipeOne.isTerminated(), equalTo(true));
        assertThat(terminationRecordingOutputPipeOne.isTerminated(), equalTo(true));
        assertThat(terminationRecordingInputPipeTwo.isTerminated(), equalTo(true));
        assertThat(terminationRecordingOutputPipeTwo.isTerminated(), equalTo(true));
    }
    
    @Test
    public void execeptionMessagesChainedOnFailureOfMultipleCommands() {
        final TestCommandHandler testCommandHandlerOne = new TestCommandHandler("one");
        testCommandHandlerOne.injectCommandFailure();
        final TestCommandHandler testCommandHandlerTwo = new TestCommandHandler("two");
        testCommandHandlerTwo.injectCommandFailure();
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandlerOne, testCommandHandlerTwo);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        try {
            executionContainer.execute();
            fail("Should have throw a CommandFailureException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo("Multiple commands failed: fail!!; fail!!"));
        }
    }
    
    @Test
    public void exceptionThrownOnFailureOfSingleCommand() {
        final TestCommandHandler testCommandHandler = new TestCommandHandler("one");
        testCommandHandler.injectCommandFailure();
        final List<CommandHandler> handlers = Arrays.<CommandHandler>asList(testCommandHandler);
        final ExecutionContainer executionContainer = new ExecutionContainer(handlers);
        
        try {
            executionContainer.execute();
            fail("Should have throw a CommandFailureException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo("fail!!"));
        }
    }
}
