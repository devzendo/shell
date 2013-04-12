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

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.devzendo.shell.plugin.ShellPlugin;
import org.devzendo.shell.plugin.ShellPluginException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

public class TestPluginLoader {
    final PluginLoader pluginLoader = new PluginLoader();
    
    @BeforeClass
    public static void setUpLogging() {
        LoggingUnittestHelper.setupLogging();
    }

    @Test
    public void canLoadWellDefinedPlugin() throws ShellPluginException {
        final List<ShellPlugin> plugins = pluginLoader.loadPluginsFromClasspath("org/devzendo/shell/testpluginloader-good-plugins.properties");
        assertThat(plugins.size(), equalTo(1));
    }

    @Test
    public void doesNotLoadNonexistantClassPlugin() {
        try {
            pluginLoader.loadPluginsFromClasspath("org/devzendo/shell/testpluginloader-nonexistant-plugins.properties");
            fail("Should have thrown ShellPluginException when loading a plugin that does not exist");
        } catch (final ShellPluginException e) {
            assertThat(e.getMessage(), equalTo("Failure loading plugins: Cannot load class 'org.devzendo.shell.NonexistantTestPlugin': ClassNotFoundException: org.devzendo.shell.NonexistantTestPlugin"));
        }
    }

    @Test
    public void doesNotLoadEmptyClassPlugin() {
        try {
            pluginLoader.loadPluginsFromClasspath("org/devzendo/shell/testpluginloader-empty-plugins.properties");
            fail("Should have thrown ShellPluginException when loading a plugin that does not exist");
        } catch (final ShellPluginException e) {
            assertThat(e.getMessage(), equalTo("Failure loading plugins: Cannot load a Plugin from null or empty class name"));
        }
    }

    @Test
    public void doesNotLoadNonShellPlugin() {
        try {
            pluginLoader.loadPluginsFromClasspath("org/devzendo/shell/testpluginloader-notshellplugin.properties");
            fail("Should have thrown ShellPluginException when loading a class that is not a ShellPlugin");
        } catch (final ShellPluginException e) {
            assertThat(e.getMessage(), equalTo("Failure loading plugins: Cannot load class 'org.devzendo.shell.NotAShellPlugin': ClassCastException: org.devzendo.shell.NotAShellPlugin cannot be cast to org.devzendo.shell.plugin.ShellPlugin"));
        }
    }

}
