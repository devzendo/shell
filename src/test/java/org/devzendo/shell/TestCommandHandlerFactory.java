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

import java.lang.reflect.Method;
import java.util.Map;

import org.devzendo.shell.PluginVariations.AbstractShellPlugin;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgs;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class TestCommandHandlerFactory {
    private final Mockery context = new JUnit4Mockery();
    private final PluginMethodScanner scanner = new PluginMethodScanner();
    private final CommandHandlerFactory factory = new CommandHandlerFactory();
    private final InputPipe inputPipe = context.mock(InputPipe.class);
    private final OutputPipe outputPipe = context.mock(OutputPipe.class);
    
    @Test
    public void voidReturnNoArgs() throws CommandExecutionException {
        final AbstractShellPlugin plugin = new VoidReturnNoArgs();
        final Method method = getMethod(plugin);
        final CommandHandler handler = factory.createHandler(plugin, method);
        assertPipes(plugin, null, null);
        handler.execute();
        assertPipes(plugin, null, null);
    }

    private void assertPipes(final AbstractShellPlugin plugin,
            final InputPipe expectedInputPipe,
            final OutputPipe expectedOutputPipe) {
        assertThat(plugin.getInputPipe(), equalTo(expectedInputPipe));
        assertThat(plugin.getOutputPipe(), equalTo(expectedOutputPipe));
    }

    private Method getMethod(ShellPlugin plugin) {
        final Map<String, Method> methods = scanner.scanPluginMethods(plugin);
        assertThat(methods.size(), equalTo(1));
        return methods.values().iterator().next();
    }
}
