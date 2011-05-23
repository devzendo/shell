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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;


public class TestCommandRegistry {
    final CommandRegistry registry = new CommandRegistry();

    final ShellPlugin shellPluginOne = new ShellPlugin() {
        @Override
        public void processCommandLine(List<String> args) {
        }

        @Override
        public String getName() {
            return "plugin one";
        }
    };

    final ShellPlugin shellPluginTwo = new ShellPlugin() {
        @Override
        public void processCommandLine(List<String> args) {
        }

        @Override
        public String getName() {
            return "plugin two";
        }
    };

    @Test
    public void getUnregisteredCommandYieldsException() {
        try {
            registry.getHandler("doesntexist");
            fail("Should have thrown on nonexistant comamnd");
        } catch (CommandNotFoundException cnfe) {
            assertThat(cnfe.getMessage(), equalTo("'doesntexist' not found"));
        }
    }

    @Test
    public void getRegisteredCommandYieldsHandler() throws SecurityException, NoSuchMethodException, CommandNotFoundException {
        final Method method = this.getClass().getMethod("getRegisteredCommandYieldsHandler");    
        try {
            registry.registerCommand("foo", shellPluginOne, method);
            CommandHandler handler = registry.getHandler("foo");
            assertThat(handler, notNullValue());
        } catch (DuplicateCommandException e) {
            fail("Should not have thrown on initial command registration");
        }
    }

    @Test
    public void duplicateRegistration() throws SecurityException, NoSuchMethodException {
        final Method method = this.getClass().getMethod("duplicateRegistration");    
        try {
            registry.registerCommand("foo", shellPluginOne, method);
        } catch (DuplicateCommandException e) {
            fail("Should not have thrown on initial command registration");
        }
        try {
            registry.registerCommand("foo", shellPluginTwo, method);
            fail("Should have thrown on duplicate comamnd");
        } catch (DuplicateCommandException de) {
            assertThat(de.getMessage(), 
                equalTo("Command 'foo' from plugin 'plugin two' is duplicated; initially declared in plugin 'plugin one'"));
        }
    }
}
