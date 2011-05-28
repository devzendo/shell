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
        protected InputPipe mInputPipe = null;
        protected OutputPipe mOutputPipe = null;
        protected List<Object> mArgs = null;
        
        public final List<Object> getArgs() {
            return mArgs;
        }

        public final InputPipe getInputPipe() {
            return mInputPipe;
        }

        public final OutputPipe getOutputPipe() {
            return mOutputPipe;
        }

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
        public void funk(final List<Object> args) {
            mExecuted = true;
            mArgs = args;
        }
    }

    public static class VoidReturnListArgsInputPipe extends AbstractShellPlugin {
        public void funk(final List<Object> args, final InputPipe input) {
            mExecuted = true;
            mArgs = args;
            mInputPipe = input;
        }
    }

    public static class VoidReturnListArgsInputPipeOutputPipe extends
            AbstractShellPlugin {
        public void funk(
                final List<Object> args,
                final InputPipe input,
                final OutputPipe output) {
            mExecuted = true;
            mArgs = args;
            mInputPipe = input;
            mOutputPipe = output;
        }
    }

    public static class VoidReturnListArgsOutputPipe extends
            AbstractShellPlugin {
        public void funk(final List<Object> args, final OutputPipe output) {
            mExecuted = true;
            mArgs = args;
            mOutputPipe = output;
        }
    }

    public static class VoidReturnListArgsOutputPipeInputPipe extends
            AbstractShellPlugin {
        public void funk(
                final List<Object> args,
                final OutputPipe output,
                final InputPipe input) {
            mExecuted = true;
            mArgs = args;
            mInputPipe = input;
            mOutputPipe = output;
        }
    }

    public static class VoidReturnNoArgs extends AbstractShellPlugin {
        public void funk() {
            mExecuted = true;
        }
    }

    public static class VoidReturnNoArgsBadPluginMethodsNotScanned extends
            AbstractShellPlugin {
        public void funk() {
            mExecuted = true;
        }

        public void initialise() {
            // do nothing
        }
    }

    public static class VoidReturnNoArgsInputPipe extends AbstractShellPlugin {
        public void funk(final InputPipe input) {
            mExecuted = true;
            mInputPipe = input;
        }
    }

    public static class VoidReturnNoArgsInputPipeOutputPipe extends
            AbstractShellPlugin {
        public void funk(final InputPipe input, final OutputPipe output) {
            mExecuted = true;
            mInputPipe = input;
            mOutputPipe = output;
        }
    }

    public static class VoidReturnNoArgsOutputPipe extends AbstractShellPlugin {
        public void funk(final OutputPipe output) {
            mExecuted = true;
            mOutputPipe = output;
        }
    }

    public static class VoidReturnNoArgsOutputPipeInputPipe extends
            AbstractShellPlugin {
        public void funk(final OutputPipe output, final InputPipe input) {
            mExecuted = true;
            mInputPipe = input;
            mOutputPipe = output;
        }
    }
}
