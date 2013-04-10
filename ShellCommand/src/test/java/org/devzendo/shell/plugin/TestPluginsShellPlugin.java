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
package org.devzendo.shell.plugin;

import java.util.Arrays;
import java.util.Collections;

import org.devzendo.shell.*;
import org.devzendo.shell.pipe.OutputPipe;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class TestPluginsShellPlugin {
    private final Mockery context = new JUnit4Mockery();
    @Test
    public void listPluginsListsPlugins() throws ShellPluginException {
        final ShellPlugin plugin = new PluginsShellPlugin();
        @SuppressWarnings("unchecked")
        final IPluginRegistry pluginRegistry = new PluginRegistry("irrelevant", new CommandRegistry(), null, Collections.EMPTY_LIST);
        pluginRegistry.loadAndRegisterPluginMethods(Arrays.asList(plugin));

        final OutputPipe outputPipe = context.mock(OutputPipe.class);
        context.checking(new Expectations() { {
            oneOf(outputPipe).push("Plugins");
        } });

        ((PluginsShellPlugin)plugin).listPlugins(outputPipe);
    }
}
