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

import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class TestCommandHandler {
    private static final scala.Option<Integer> none = scala.Option.apply(null);
    
    private final Mockery context = new JUnit4Mockery();
    private final InputPipe inputPipe = context.mock(InputPipe.class);
    private final OutputPipe outputPipe = context.mock(OutputPipe.class);

    @Test
    public void pipesTerminatedAfterExecuteAndTerminatePipes() throws CommandExecutionException {
        final CommandHandler handler = new CommandHandler(
            "foo", none, none, none /*Option.<Integer>apply(0), Option.<Integer>apply(1) */ ) {

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
}
