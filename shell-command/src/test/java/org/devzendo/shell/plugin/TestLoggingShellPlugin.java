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
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.devzendo.commoncode.logging.CapturingAppender;
import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.interpreter.CommandRegistry;
import org.devzendo.shell.interpreter.DefaultPluginRegistry;
import org.devzendo.shell.interpreter.PluginRegistry;
import org.devzendo.shell.pipe.InputPipe;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.*;
import scala.Option;

import java.util.List;

public class TestLoggingShellPlugin {
    private CapturingAppender mCapturingAppender = new CapturingAppender();
    private Logger root = Logger.getRootLogger();

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    @BeforeClass
    public static void resetLogging() {
        BasicConfigurator.resetConfiguration();
    }

    // Note - NOT a @Before, it gets set up at a particular place in the test,
    // if it were set up before the test runs, many events would be logged that
    // I'm not interested in.
    public void setupLogging() {
        root.addAppender(mCapturingAppender);
        Assert.assertEquals(0, mCapturingAppender.getEvents().size());
    }

    // Similarly
    public void teardownLogging() {
        root.removeAppender(mCapturingAppender);
    }

    @Test
    public void logDebugLogsAtDebugLevel() throws ShellPluginException {
        final LoggingShellPlugin plugin = createLoggingShellPlugin();
        try {
            setupLogging();
            plugin.logDebug(pipeWithSomeData());
        } finally {
            teardownLogging();
        }

        checkEvents(Level.DEBUG);
    }

    @Test
    public void logInfoLogsAtInfoLevel() throws ShellPluginException {
        final LoggingShellPlugin plugin = createLoggingShellPlugin();
        try {
            setupLogging();
            plugin.logInfo(pipeWithSomeData());
        } finally {
            teardownLogging();
        }

        checkEvents(Level.INFO);
    }

    @Test
    public void logWarnLogsAtWarnLevel() throws ShellPluginException {
        final LoggingShellPlugin plugin = createLoggingShellPlugin();
        try {
            setupLogging();
            plugin.logWarn(pipeWithSomeData());
        } finally {
            teardownLogging();
        }

        checkEvents(Level.WARN);
    }

    private void checkEvents(Level level) {
        final List<LoggingEvent> events = mCapturingAppender.getEvents();
        Assert.assertEquals(2, events.size());
        final LoggingEvent loggingEventOne = events.get(0);
        Assert.assertEquals(level, loggingEventOne.getLevel());
        Assert.assertEquals("one", loggingEventOne.getMessage().toString());
        final LoggingEvent loggingEventTwo = events.get(1);
        Assert.assertEquals(level, loggingEventTwo.getLevel());
        Assert.assertEquals("two", loggingEventTwo.getMessage().toString());
    }

    private LoggingShellPlugin createLoggingShellPlugin() throws ShellPluginException {
        final LoggingShellPlugin plugin = new LoggingShellPlugin();
        @SuppressWarnings("unchecked")
        final scala.collection.immutable.List<String> noArgs = ScalaListHelper.createList();
        final PluginRegistry pluginRegistry = new DefaultPluginRegistry("irrelevant", new CommandRegistry(), null, noArgs);
        pluginRegistry.loadAndRegisterPluginMethods(ScalaListHelper.createList((ShellPlugin) plugin));

        return plugin;
    }

    private InputPipe pipeWithSomeData() {
        final InputPipe inputPipe = context.mock(InputPipe.class);
        context.checking(new Expectations() {
            {
                oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply("one")));
                oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply("two")));
                oneOf(inputPipe).next();
                will(returnValue(Option.<Object>apply(null)));
            }
        });
        return inputPipe;
    }
}
