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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.devzendo.shell.PluginVariations.AbstractShellPlugin;
import org.devzendo.shell.PluginVariations.VoidReturnInputPipeOutputPipeListArgs;
import org.devzendo.shell.PluginVariations.VoidReturnListArgs;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsInputPipeOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsOutputPipeInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgs;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsInputPipeOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsOutputPipeInputPipe;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class TestCommandHandlerFactory {
    private final Mockery context = new JUnit4Mockery();
    private final PluginMethodScanner scanner = new PluginMethodScanner();
    private final CommandHandlerFactory factory = new CommandHandlerFactory();
    @SuppressWarnings("unchecked")
    private final List<Object> args = context.mock(List.class);
    private final InputPipe inputPipe = context.mock(InputPipe.class);
    private final OutputPipe outputPipe = context.mock(OutputPipe.class);
    
    @BeforeClass
    public static void setupLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    @Test
    public void voidReturnListArgs() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgs();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, null, null);
    }
    
    @Test
    public void voidReturnListArgsInputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgsInputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, inputPipe, null);
    }

    @Test
    public void voidReturnListArgsInputPipeOutputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgsInputPipeOutputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, inputPipe, outputPipe);
    }

    @Test
    public void voidReturnListArgsOutputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgsOutputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, null, outputPipe);
    }

    @Test
    public void voidReturnListArgsOutputPipeInputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnListArgsOutputPipeInputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, inputPipe, outputPipe);
    }

    @Test
    public void voidReturnNoArgs() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgs();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, null, null);
    }

    @Test
    public void voidReturnNoArgsInputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgsInputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, inputPipe, null);
    }

    @Test
    public void voidReturnNoArgsInputPipeOutputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgsInputPipeOutputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, inputPipe, outputPipe);
    }

    @Test
    public void voidReturnNoArgsOutputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgsOutputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, null, outputPipe);
    }

    @Test
    public void voidReturnNoArgsOutputPipeInputPipe() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgsOutputPipeInputPipe();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, null, inputPipe, outputPipe);
    }
    
    @Test
    public void voidReturnInputPipeOutputPipeListArgs() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnInputPipeOutputPipeListArgs();
        setupAndExecuteHandler(plugin);
        assertPluginHasBeenPassed(plugin, args, inputPipe, outputPipe);
    }

    private void setupAndExecuteHandler(final AbstractShellPlugin plugin)
            throws CommandExecutionException {
        final Map<String, AnalysedMethod> methods = scanner.scanPluginMethods(plugin);
        assertThat(methods.size(), equalTo(1));
        final AnalysedMethod analysedMethod = methods.values().iterator().next();
        final CommandHandler handler = factory.createHandler(plugin, analysedMethod);
        handler.setArgs(args);
        handler.setInputPipe(inputPipe);
        handler.setOutputPipe(outputPipe);
        assertPluginHasBeenPassed(plugin, null, null, null);
        handler.execute();
    }
    
    private void assertPluginHasBeenPassed(final AbstractShellPlugin plugin,
            final List<Object> expectedArgs,
            final InputPipe expectedInputPipe,
            final OutputPipe expectedOutputPipe) {
        assertThat(plugin.getArgs(), equalTo(expectedArgs));
        assertThat(plugin.getInputPipe(), equalTo(expectedInputPipe));
        assertThat(plugin.getOutputPipe(), equalTo(expectedOutputPipe));
    }
}
