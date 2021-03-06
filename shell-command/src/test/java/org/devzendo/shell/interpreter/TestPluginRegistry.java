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
import org.devzendo.shell.RecordingShellPlugin;
import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.plugin.ShellPlugin;
import org.devzendo.shell.plugin.ShellPluginException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

public class TestPluginRegistry {
    private static final scala.Option<VariableRegistry> noneVariableRegistry = scala.Option.apply(null);
    private CommandRegistry mCommandRegistry = new CommandRegistry();
    private VariableRegistry mVariableRegistry = new DefaultVariableRegistry(noneVariableRegistry);
    
    @BeforeClass
    public static void setupLogging() {
        LoggingUnittestHelper.setupLogging();
    }

    final ShellPlugin shellPluginOne = new ShellPlugin() {
        @SuppressWarnings("unused")
        public void foo() {
        }

        @Override
        public String getName() {
            return "plugin one";
        }

        @Override
        public void initialise(final ExecutionEnvironment env) {
        }
    };

    final ShellPlugin shellPluginTwo = new ShellPlugin() {
        @SuppressWarnings("unused")
        public void foo() {
        }

        @Override
        public String getName() {
            return "plugin two";
        }

        @Override
        public void initialise(final ExecutionEnvironment env) {
        }
    };


    private PluginRegistry getPluginRegistry(final String propertiesName) {
        return new DefaultPluginRegistry(propertiesName, mCommandRegistry, mVariableRegistry, ScalaListHelper.createList("one", "two"));
    }

    @Test
    public void pluginLoadedAndReceivesExecutionEnvironment() throws ShellPluginException {
        final PluginRegistry pluginRegistry = getPluginRegistry("org/devzendo/shell/testpluginregistrar-recording-plugin.properties");
        pluginRegistry.loadAndRegisterPluginMethods(ScalaListHelper.<ShellPlugin>createList());
        final scala.collection.immutable.Set<ShellPlugin> plugins = pluginRegistry.getPlugins();
        assertThat(plugins.size(), equalTo(1));
        final RecordingShellPlugin recordingPlugin = (RecordingShellPlugin) plugins.iterator().next();
        assertThat(recordingPlugin.getCommandRegistry(), equalTo(mCommandRegistry));
        assertThat(recordingPlugin.getArgs().size(), equalTo(2));
        assertThat(recordingPlugin.getArgs().apply(0), equalTo("one"));
        assertThat(recordingPlugin.getArgs().apply(1), equalTo("two"));
    }

    @Test
    public void staticPluginsPopulatedInRegistry() throws ShellPluginException {
        final PluginRegistry pluginRegistry = getPluginRegistry("org/devzendo/shell/testpluginregistrar-recording-plugin.properties");
        pluginRegistry.loadAndRegisterPluginMethods(ScalaListHelper.createList(shellPluginOne));
        final scala.collection.immutable.Set<ShellPlugin> plugins = pluginRegistry.getPlugins();
        assertThat(plugins.size(), equalTo(2));
    }
    
    @Test
    public void duplicateCommandThrows() {
        final PluginRegistry pluginRegistry = getPluginRegistry("org/devzendo/shell/testpluginregistrar-no-plugins.properties");
        try {
            pluginRegistry.loadAndRegisterPluginMethods(ScalaListHelper.createList(shellPluginOne, shellPluginTwo));
            fail("Should have thrown a ShellPluginexception registering plugins with duplicate command names");
        } catch (final ShellPluginException e) {
            assertThat(e.getMessage(), equalTo("Command 'foo' from plugin 'plugin two' is duplicated; initially declared in plugin 'plugin one'"));
        }
    }
}
