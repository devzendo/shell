/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.devzendo.shell;

import java.util.List;

import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;

public class PluginVariations {
    @SuppressWarnings("unchecked")
    public Class<? extends ShellPlugin>[] pluginClasses = new Class[] {
            VoidReturnNoArgsBadPluginMethodsNotScanned.class,
            VoidReturnNoArgs.class, VoidReturnNoArgsInputPipe.class,
            VoidReturnNoArgsOutputPipe.class,
            VoidReturnNoArgsOutputPipeInputPipe.class,
            VoidReturnNoArgsInputPipeOutputPipe.class,
            VoidReturnListArgs.class, VoidReturnListArgsInputPipe.class,
            VoidReturnListArgsOutputPipe.class,
            VoidReturnListArgsInputPipeOutputPipe.class,
            VoidReturnListArgsOutputPipeInputPipe.class,};

    public static abstract class AbstractShellPlugin implements ShellPlugin {
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

    public static class VoidReturnListArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args) {
            mExecuted = true;
        }
    }

    public static class VoidReturnListArgsInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final InputPipe input) {
            mExecuted = true;
        }
    }

    public static class VoidReturnListArgsInputPipeOutputPipe extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(
                final List<Object> args,
                final InputPipe input,
                final OutputPipe output) {
            mExecuted = true;
        }
    }

    public static class VoidReturnListArgsOutputPipe extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final List<Object> args, final OutputPipe output) {
            mExecuted = true;
        }
    }

    public static class VoidReturnListArgsOutputPipeInputPipe extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(
                final List<Object> args,
                final OutputPipe output,
                final InputPipe input) {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgs extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk() {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgsBadPluginMethodsNotScanned extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk() {
            mExecuted = true;
        }

        @SuppressWarnings("unused")
        public void initialise() {
            // do nothing
        }
    }

    public static class VoidReturnNoArgsInputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final InputPipe input) {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgsInputPipeOutputPipe extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final InputPipe input, final OutputPipe output) {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgsOutputPipe extends AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final OutputPipe output) {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgsOutputPipeInputPipe extends
            AbstractShellPlugin {
        @SuppressWarnings("unused")
        public void funk(final OutputPipe output, final InputPipe input) {
            mExecuted = true;
        }
    }
}
