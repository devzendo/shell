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
import static org.junit.Assert.fail;

import org.devzendo.shell.Variable;
import org.junit.Test;


public class TestVariableOutputPipe {
    private Variable mVar = new Variable();

    @Test
    public void variableOutputPipeWritesToVariable() {
        final VariableOutputPipe pipe = new VariableOutputPipe(mVar);
        assertThat(mVar.size(), equalTo(0));
        
        pipe.push("hello");
        
        assertThat(mVar.size(), equalTo(1));
        assertThat(mVar.get(0).toString(), equalTo("hello"));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotStoreWhenTerminated() {
        final VariableOutputPipe pipe = new VariableOutputPipe(mVar);
        pipe.setTerminated();
        pipe.push("hello");
    }

    @Test
    public void doesntChangeUponStoreWhenTerminated() {
        final VariableOutputPipe pipe = new VariableOutputPipe(mVar);
        pipe.push("hello");
        assertThat(mVar.size(), equalTo(1));
        pipe.setTerminated();
        try {
            pipe.push("world");
            fail("Should have thrown an IllegalStateException on push after setTerminated, but didn't");
        } catch (IllegalStateException ise) {
            assertThat(mVar.size(), equalTo(1));
            assertThat(mVar.get(0).toString(), equalTo("hello"));
        }
    }
}
