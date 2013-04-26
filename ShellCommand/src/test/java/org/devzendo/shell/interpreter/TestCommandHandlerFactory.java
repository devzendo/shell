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

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.devzendo.shell.PluginVariations.*;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(JMock.class)
public class TestCommandHandlerFactory {
    private final Mockery context = new JUnit4Mockery();
    private final PluginMethodScanner scanner = new PluginMethodScanner();
    private final CommandHandlerFactory factory = new CommandHandlerFactory();
    @SuppressWarnings("unchecked")
    private final List<Object> args = context.mock(List.class);
    private final InputPipe inputPipe = context.mock(InputPipe.class);
    private final OutputPipe outputPipe = context.mock(OutputPipe.class);
    private final Log log = context.mock(Log.class);
    
    @BeforeClass
    public static void setupLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    @Test
    public void voidReturnListArgsInputPipeOutputPipeLog() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgsInputPipeOutputPipeLog();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, inputPipe, outputPipe, log);
    }

    @Test
    public void voidReturnNoArgs() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgs();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, null, null, null);
    }

    private void setupAndExecuteHandler(final AbstractShellPlugin plugin)
            throws CommandExecutionException {
        final scala.collection.immutable.Map<String, AnalysedMethod> methods = scanner.scanPluginMethods(plugin);
        assertThat(methods.size(), equalTo(1));
        final AnalysedMethod analysedMethod = methods.values().iterator().next();
        final CommandHandler handler = factory.createHandler(plugin, analysedMethod);
        handler.setArgs(args);
        handler.setInputPipe(inputPipe);
        handler.setOutputPipe(outputPipe);
        handler.setLog(log);
        assertPluginHasBeenPassed(plugin, null, null, null, null);
        handler.execute();
    }
    
    private void assertPluginHasBeenPassed(final AbstractShellPlugin plugin,
            final List<Object> expectedArgs,
            final InputPipe expectedInputPipe,
            final OutputPipe expectedOutputPipe,
            final Log expectedLog) {
        assertThat(plugin.getArgs(), equalTo(expectedArgs));
        assertThat(plugin.getInputPipe(), equalTo(expectedInputPipe));
        assertThat(plugin.getOutputPipe(), equalTo(expectedOutputPipe));
        assertThat(plugin.getLog(), equalTo(expectedLog));
    }
}
