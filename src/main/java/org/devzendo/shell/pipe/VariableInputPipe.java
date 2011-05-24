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

public class VariableInputPipe implements InputPipe {
    private final Variable mVariable;
    private int mIndex;

    public VariableInputPipe(final Variable var) {
        mVariable = var;
        mIndex = 0;
    }

    @Override
    public void setTerminated() {
        mIndex = mVariable.size();
    }

    @Override
    public boolean hasNext() {
        return mIndex < mVariable.size();
    }

    @Override
    public Object getNext() {
        return mVariable.get(mIndex++);
    }
}
