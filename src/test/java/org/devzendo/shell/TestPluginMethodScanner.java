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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPluginMethodScanner {
    @BeforeClass
    public static void setUpLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    final PluginMethodScanner scanner = new PluginMethodScanner();
    private abstract static class AbstractShellPlugin implements ShellPlugin {
        @Override
        public void initialise(final ExecutionEnvironment env) {
            // do nothing
        }

        @Override
        public String getName() {
            return "test";
        }
    }

    private static class VoidReturnNoArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk() {
            // do nothing
        }
    }
    
    @Test
    public void voidReturnNoArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgs()));
    }

    private static class VoidReturnListArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<String> args) {
            // do nothing
        }
    }
    
    @Test
    public void voidReturnStringArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgs()));
    }

    private static class VoidReturnListIteratorArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<String> args, final Iterator<Object> input) {
            // do nothing
        }
    }
    
    @Test
    public void voidReturnListIteratorArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListIteratorArgs()));
    }

    private static class IteratorReturnNoArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public Iterator<Object> funk() {
            // do nothing
            return null;
        }
    }
    
    @Test
    public void iteratorReturnNoArgsOk() {
        gotFunk(scanner.scanPluginMethods(new IteratorReturnNoArgs()));
    }

    private static class IteratorReturnListArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public Iterator<Object> funk(final List<String> args) {
            // do nothing
            return null;
        }
    }
    
    @Test
    public void iteratorReturnStringArgsOk() {
        gotFunk(scanner.scanPluginMethods(new IteratorReturnListArgs()));
    }

    private static class IteratorReturnListIteratorArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public Iterator<Object> funk(final List<String> args, final Iterator<Object> input) {
            // do nothing
            return null;
        }
    }
    
    @Test
    public void iteratorReturnListIteratorArgsOk() {
        gotFunk(scanner.scanPluginMethods(new IteratorReturnListIteratorArgs()));
    }

    private void gotFunk(final Map<String, Method> map) {
        assertThat(map.size(), equalTo(1));
        final Iterator<String> it = map.keySet().iterator();
        assertThat(it.hasNext(), equalTo(true));
        assertThat(it.next(), equalTo("funk"));
    }
}
