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

import org.devzendo.shell.interpreter.Variable;
import org.junit.Test;

import static org.devzendo.shell.pipe.OptionMatcher.isSome;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestVariableInputPipe {
    private Variable mVar = new Variable();

    @Test
    public void variableInputPipeReadsFromVariable() {
        mVar.add("hello");
        mVar.add("world");
        
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        assertThat(pipe.next(), isSome((Object)"hello"));
        assertThat(pipe.next(), isSome((Object)"world"));
        assertThat(pipe.next(), OptionMatcher.isNone());
    }

    @Test
    public void variableInputPipeGivesEmptinesOnEmptyVariable() {
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        
        assertThat(pipe.next(), OptionMatcher.isNone());
    }

    @Test
    public void nothingReturnedWhenTerminated() {
        mVar.add("hello");
        mVar.add("world");
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        
        pipe.setTerminated();
        
        assertThat(pipe.next(), OptionMatcher.isNone());
    }
}
