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

import org.junit.Test;


public class TestVariableRegistry {
    final VariableRegistry registry = new VariableRegistry();
    
    @Test
    public void getUnregisteredVariableCreates() {
        final VariableReference vR = new VariableReference("var");
        assertThat(registry.exists(vR), equalTo(false));
        final Variable v = registry.getVariable(vR);
        assertThat(registry.exists(vR), equalTo(true));
        assertThat(v.size(), equalTo(0));
    }
    
    @Test
    public void setVariableRegistryCreates() {
        final VariableReference vR = new VariableReference("var");
        assertThat(registry.exists(vR), equalTo(false));
        final Variable var = new Variable();
        var.add("hello");
        registry.setVariable(vR, var);
        assertThat(registry.exists(vR), equalTo(true));
        final Variable varRetrieved = registry.getVariable(vR);
        assertThat(varRetrieved.size(), equalTo(1));
        assertThat(varRetrieved.get(0).toString(), equalTo("hello"));
    }
}
