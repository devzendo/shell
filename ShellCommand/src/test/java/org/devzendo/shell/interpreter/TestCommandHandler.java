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

import org.apache.log4j.BasicConfigurator;
import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.Option;
import scala.Option$;
import scala.collection.immutable.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(JMock.class)
public class TestCommandHandler {
    private static final scala.Option<Integer> none = scala.Option.apply(null);
    
    private final Mockery context = new JUnit4Mockery();
    private final InputPipe inputPipe = context.mock(InputPipe.class);
    private final OutputPipe outputPipe = context.mock(OutputPipe.class);

    @Test
    public void pipesTerminatedAfterExecuteAndTerminatePipes() throws CommandExecutionException {
        final CommandHandler handler = new CommandHandler(
            "foo", none, none, none, none ) {

            @Override
            public void execute() throws CommandExecutionException {
                // do nothing
                
            }
            
        };
        handler.setInputPipe(inputPipe);
        handler.setOutputPipe(outputPipe);
        context.checking(new Expectations() { {
            oneOf(inputPipe).setTerminated();
            oneOf(outputPipe).setTerminated();
        } });

        handler.executeAndTerminatePipes();
    }

    private static int sequence = 0;
    private static class SequencedCommandHandler extends CommandHandler {
        private int executionSequence = -1;

        public SequencedCommandHandler(final String name) {
            super(name, none, none, none, none);
        }

        @Override
        public void execute() {
            executionSequence = sequence++;
        }
    }

    @Test
    public void subCommandsAreExecutedLeftToRight() throws CommandExecutionException {
        BasicConfigurator.configure();
        final SequencedCommandHandler main = new SequencedCommandHandler("main");
        final SequencedCommandHandler first = new SequencedCommandHandler("first");
        final SequencedCommandHandler second = new SequencedCommandHandler("second");
        main.setSubCommandHandlers(
                ScalaListHelper.createList(
                        Option$.MODULE$.apply((CommandHandler)first),
                        Option$.MODULE$.apply((CommandHandler)second)));

        main.executeAndTerminatePipes();

        // subcommands executed first...
        assertThat(first.executionSequence, equalTo(0));
        assertThat(second.executionSequence, equalTo(1));
        // then...
        assertThat(main.executionSequence, equalTo(2));
    }
}
