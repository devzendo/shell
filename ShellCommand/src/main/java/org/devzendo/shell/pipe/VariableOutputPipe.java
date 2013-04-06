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
package org.devzendo.shell.pipe;

import org.devzendo.shell.Variable;

public class VariableOutputPipe implements OutputPipe {
    private final Variable mVariable;
    private boolean mTerminated;

    public VariableOutputPipe(final Variable var) {
        mVariable = var;
        mTerminated = false;
    }

    @Override
    public void setTerminated() {
        mTerminated = true;
    }

    @Override
    public void push(final Object object) {
        if (mTerminated) {
            throw new IllegalStateException("Cannot push into a terminated pipe");
        }
        mVariable.add(object);
    }

    public final Variable getVariable() {
        return mVariable;
    }
}
