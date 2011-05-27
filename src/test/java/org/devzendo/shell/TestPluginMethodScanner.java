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
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPluginMethodScanner {
    
    @BeforeClass
    public static void setUpLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    final PluginMethodScanner scanner = new PluginMethodScanner();
    
    private abstract static class AbstractShellPlugin implements ShellPlugin {
        protected boolean mExecuted = false;
        
        public final boolean isExecuted() {
            return mExecuted;
        }

        @Override
        public void initialise(final ExecutionEnvironment env) {
            // do nothing
        }

        @Override
        public String getName() {
            return "test";
        }
    }

    // -------------------------------------------------------------------------
    
    private static class VoidReturnNoArgsBadPluginMethodsNotScanned extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk() {
            mExecuted = true;
        }
        
        @SuppressWarnings("unused")
        public void initialise() {
            // do nothing
        }
    }
    
    @Test
    public void voidReturnNoArgsOkBadInitialiseNotScanned() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsBadPluginMethodsNotScanned()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnNoArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk() {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnNoArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgs()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnNoArgsInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final InputPipe input) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnNoArgsInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsInputPipe()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnNoArgsOutputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final OutputPipe output) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnNoArgsOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsOutputPipe()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnNoArgsOutputPipeInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final OutputPipe output, final InputPipe input) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnNoArgsOutputPipeInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsOutputPipeInputPipe()));
    }


    // -------------------------------------------------------------------------

    private static class VoidReturnNoArgsInputPipeOutputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final InputPipe input, final OutputPipe output) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnNoArgsInputPipeOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsInputPipeOutputPipe()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnListArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnListArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgs()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnListArgsInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final InputPipe input) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnListArgsInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsInputPipe()));
    }


    // -------------------------------------------------------------------------

    private static class VoidReturnListArgsOutputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final OutputPipe output) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnListArgsOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsOutputPipe()));
    }

    // -------------------------------------------------------------------------
    
    private static class VoidReturnListArgsInputPipeOutputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final InputPipe input, final OutputPipe output) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnListArgsInputPipeOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsInputPipeOutputPipe()));
    }

    // -------------------------------------------------------------------------

    private static class VoidReturnListArgsOutputPipeInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final OutputPipe output, final InputPipe input) {
            mExecuted = true;
        }
    }
    
    @Test
    public void voidReturnListArgsOutputPipeInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsOutputPipeInputPipe()));
    }

    // -------------------------------------------------------------------------

    private void gotFunk(final Map<String, Method> map) {
        assertThat(map.size(), equalTo(1));
        final Iterator<String> it = map.keySet().iterator();
        assertThat(it.hasNext(), equalTo(true));
        assertThat(it.next(), equalTo("funk"));
    }
}
