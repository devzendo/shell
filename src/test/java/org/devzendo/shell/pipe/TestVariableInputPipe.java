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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.devzendo.shell.Variable;
import org.junit.Test;


public class TestVariableInputPipe {
    private Variable mVar = new Variable();

    @Test
    public void variableInputPipeReadsFromVariable() {
        mVar.add("hello");
        mVar.add("world");
        
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        
        assertThat(pipe.hasNext(), equalTo(true));
        assertThat(pipe.getNext().toString(), equalTo("hello"));
        assertThat(pipe.hasNext(), equalTo(true));
        assertThat(pipe.getNext().toString(), equalTo("world"));
        assertThat(pipe.hasNext(), equalTo(false));
    }

    @Test
    public void variableInputPipeGivesEmptinesOnEmptyVariable() {
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        
        assertThat(pipe.hasNext(), equalTo(false));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void cannotGetNextWhenThereIsNone() {
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        pipe.getNext();
    }
    
    @Test
    public void nothingReturnedWhenTerminated() {
        mVar.add("hello");
        mVar.add("world");
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        assertThat(pipe.hasNext(), equalTo(true));
        
        pipe.setTerminated();
        
        assertThat(pipe.hasNext(), equalTo(false));
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void cannotGetNextWhenTerminated() {
        mVar.add("hello");
        mVar.add("world");
        final VariableInputPipe pipe = new VariableInputPipe(mVar);
        assertThat(pipe.hasNext(), equalTo(true));
        
        pipe.setTerminated();

        pipe.getNext();
    }
}
