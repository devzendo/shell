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

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


public class TestVariableRegistry {
    final VariableRegistry registry = new VariableRegistry();
    private static final scala.Option<scala.collection.immutable.List<Object>> none = scala.Option.apply(null);


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

    @Test
    public void getVariablesReturnsValues() {
        final VariableReference vr1 = new VariableReference("v1");
        final Variable val1 = new Variable();
        val1.add("hello");
        registry.setVariable(vr1, val1);

        final VariableReference vr2 = new VariableReference("v2");
        final Variable val2 = new Variable();
        val2.add(new Integer(10));
        val2.add(new Integer(20));
        registry.setVariable(vr2, val2);

        final scala.collection.immutable.Map<String, scala.collection.immutable.List<Object>> vars = registry.getVariables();
        assertThat(vars.contains("v1"), equalTo(true));
        assertThat(vars.get("v1").get().size(), equalTo(1));
        assertThat(vars.get("v1").get().apply(0).toString(), equalTo("hello"));

        assertThat(vars.contains("v2"), equalTo(true));
        assertThat(vars.get("v2").get().size(), equalTo(2));
        assertThat((Integer) vars.get("v2").get().apply(0), equalTo(new Integer(10)));
        assertThat((Integer) vars.get("v2").get().apply(1), equalTo(new Integer(20)));

        assertThat(vars.contains("vnone"), equalTo(false));
        assertThat(vars.get("vnone"), equalTo(none));
    }
}
