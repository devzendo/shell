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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.devzendo.commoncode.logging.CapturingAppender;
import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.interpreter.CommandRegistry;
import org.devzendo.shell.interpreter.DefaultPluginRegistry;
import org.devzendo.shell.interpreter.PluginRegistry;
import org.devzendo.shell.pipe.InputPipe;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.Option;

import java.util.List;

@RunWith(JMock.class)
public class TestLoggingShellPlugin {
    private CapturingAppender mCapturingAppender;
    private final Mockery context = new JUnit4Mockery();

    public void setupLogging() {
        BasicConfigurator.resetConfiguration();
        mCapturingAppender = new CapturingAppender();
        BasicConfigurator.configure(mCapturingAppender);
        Assert.assertEquals(0, mCapturingAppender.getEvents().size());
    }

    @Test
    public void logInfoLogsAtInfoLevel() throws ShellPluginException {
        final ShellPlugin plugin = new LoggingShellPlugin();
        @SuppressWarnings("unchecked")
        final scala.collection.immutable.List<String> noArgs = ScalaListHelper.createList();
        final PluginRegistry pluginRegistry = new DefaultPluginRegistry("irrelevant", new CommandRegistry(), null, noArgs);
        pluginRegistry.loadAndRegisterPluginMethods(ScalaListHelper.createList(plugin));

        setupLogging();
        final InputPipe inputPipe = context.mock(InputPipe.class);
        context.checking(new Expectations() { {
            oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply("one")));
            oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply("two")));
            oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply(null)));
        } });

        ((LoggingShellPlugin)plugin).logInfo(inputPipe);
        
        final List<LoggingEvent> events = mCapturingAppender.getEvents();
        Assert.assertEquals(2, events.size());
        final LoggingEvent loggingEventOne = events.get(0);
        Assert.assertEquals(Level.INFO, loggingEventOne.getLevel());
        Assert.assertEquals("one", loggingEventOne.getMessage().toString());
        final LoggingEvent loggingEventTwo = events.get(1);
        Assert.assertEquals(Level.INFO, loggingEventTwo.getLevel());
        Assert.assertEquals("two", loggingEventTwo.getMessage().toString());
    }
}