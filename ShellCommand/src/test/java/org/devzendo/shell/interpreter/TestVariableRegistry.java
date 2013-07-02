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
package org.devzendo.shell.interpreter;

import org.devzendo.shell.ast.VariableReference;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;


public class TestVariableRegistry {
    private static final scala.Option<VariableRegistry> noneVariableRegistry = scala.Option.apply(null);
    private final VariableRegistry globalRegistry = new VariableRegistry(noneVariableRegistry);
    private static final scala.Option<scala.collection.immutable.List<Object>> none = scala.Option.apply(null);

    @Test
    public void getUnregisteredVariableCreates() {
        final VariableReference vR = new VariableReference("var");
        assertFalse(globalRegistry.exists(vR));
        final Variable v = globalRegistry.getVariable(vR);
        assertTrue(globalRegistry.exists(vR));
        assertThat(v.size(), equalTo(0));
    }
    
    @Test
    public void setVariableRegistryCreates() {
        final VariableReference vR = new VariableReference("var");
        assertFalse(globalRegistry.exists(vR));
        final Variable var = new Variable();
        var.add("hello");
        globalRegistry.setVariable(vR, var);
        assertTrue(globalRegistry.exists(vR));
        final Variable varRetrieved = globalRegistry.getVariable(vR);
        assertThat(varRetrieved.size(), equalTo(1));
        assertThat(varRetrieved.get(0).toString(), equalTo("hello"));
    }

    @Test
    public void getVariablesReturnsValues() {
        final VariableReference vr1 = new VariableReference("v1");
        final Variable val1 = new Variable();
        val1.add("hello");
        globalRegistry.setVariable(vr1, val1);

        final VariableReference vr2 = new VariableReference("v2");
        final Variable val2 = new Variable();
        val2.add(10);
        val2.add(20);
        globalRegistry.setVariable(vr2, val2);

        final scala.collection.immutable.Map<String, scala.collection.immutable.List<Object>> vars = globalRegistry.getVariables();
        assertTrue(vars.contains("v1"));
        assertThat(vars.get("v1").get().size(), equalTo(1));
        assertThat(vars.get("v1").get().apply(0).toString(), equalTo("hello"));

        assertTrue(vars.contains("v2"));
        assertThat(vars.get("v2").get().size(), equalTo(2));
        assertThat((Integer) vars.get("v2").get().apply(0), equalTo(10));
        assertThat((Integer) vars.get("v2").get().apply(1), equalTo(20));

        assertFalse(vars.contains("vnone"));
        assertThat(vars.get("vnone"), equalTo(none));
    }

    @Test
    public void closeRemovesAllVariables() {
        final VariableReference vr1 = new VariableReference("v1");
        final Variable val1 = new Variable();
        val1.add("hello");
        globalRegistry.setVariable(vr1, val1);
        assertTrue(globalRegistry.exists(vr1));
        assertThat(globalRegistry.getVariables().size(), equalTo(1));

        globalRegistry.close();

        assertFalse(globalRegistry.exists(vr1));
        assertThat(globalRegistry.getVariables().size(), equalTo(0));
    }

    @Test
    public void globalVariableRegistryHasNoParent() {
        assertTrue(globalRegistry.getParentScope().isEmpty());
    }

    @Test
    public void parentOfChildVariableRegistryIsParent() {
        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        assertTrue(localRegistry.getParentScope().isDefined());
        assertThat(localRegistry.getParentScope().get(), equalTo(globalRegistry));
    }

    @Test
    public void variableInParentRegistryIsVisibleInChildren() {
        final VariableReference vr1 = new VariableReference("v1");
        final Variable val1 = new Variable();
        val1.add("global");
        globalRegistry.setVariable(vr1, val1);

        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        assertTrue(localRegistry.exists(vr1));
        assertThat(localRegistry.getVariable(vr1), equalTo(val1));
        assertThat(localRegistry.getVariables().size(), equalTo(1));
    }

    @Test
    public void variableNotInParentRegistryIsNotVisibleInChildren() {
        final VariableReference vr1 = new VariableReference("v1");

        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        assertFalse(localRegistry.exists(vr1));
        assertThat(localRegistry.getVariable(vr1), not(nullValue())); // some new var created
    }

    @Test
    public void variableInParentRegistryIsStillVisibleInChildAfterChildCloses() {
        final VariableReference globalVr1 = new VariableReference("v1");
        final Variable globalVal1 = new Variable();
        globalVal1.add("global");
        globalRegistry.setVariable(globalVr1, globalVal1);

        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        final VariableReference localVr2 = new VariableReference("v2");
        final Variable localVal2 = new Variable();
        localVal2.add("local");
        localRegistry.setVariable(localVr2, localVal2);

        assertTrue(localRegistry.exists(globalVr1));
        assertTrue(localRegistry.exists(localVr2));

        localRegistry.close();

        assertTrue(localRegistry.exists(globalVr1));
        assertFalse(localRegistry.exists(localVr2));
        assertThat(localRegistry.getVariable(globalVr1), equalTo(globalVal1));
        assertThat(localRegistry.getVariables().size(), equalTo(1));
    }

    @Test
    public void variableWithSameNameInChildAsParentShadowsParentAndUnshadowsAfterChildCloses() {
        final VariableReference globalRef = new VariableReference("fool");
        final Variable shadowedContents = new Variable();
        shadowedContents.add("shadowed");
        globalRegistry.setVariable(globalRef, shadowedContents);

        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        final VariableReference localRef = new VariableReference("fool");
        final Variable localContents = new Variable();
        localContents.add("local");
        localRegistry.setVariable(localRef, localContents);

        assertTrue(localRegistry.exists(globalRef));
        assertTrue(localRegistry.exists(localRef));

        assertThat(localRegistry.getVariable(localRef), equalTo(localContents));
        assertThat(localRegistry.getVariable(localRef).get().head().toString(), equalTo("local"));
        assertThat(localRegistry.getVariables().size(), equalTo(1));
        assertThat(localRegistry.getVariables().valuesIterator().next().head().toString(), equalTo("local"));

        localRegistry.close();

        assertTrue(localRegistry.exists(globalRef));
        assertTrue(localRegistry.exists(localRef)); // exists in global, seen through local registry
        assertThat(localRegistry.getVariable(localRef), equalTo(shadowedContents));
        assertThat(localRegistry.getVariable(localRef).get().head().toString(), equalTo("shadowed"));
        assertThat(localRegistry.getVariables().size(), equalTo(1));
        assertThat(localRegistry.getVariables().valuesIterator().next().head().toString(), equalTo("shadowed"));
    }

    @Test
    public void automaticCloseHappensWhenUsageCountDecrementsToZero() {
        final VariableRegistry localRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        final VariableReference localRef = new VariableReference("localvar");
        final Variable localContents = new Variable();
        localContents.add("local");
        localRegistry.setVariable(localRef, localContents);

        assertTrue(localRegistry.exists(localRef));
        // instead of ...
        // localRegistry.close();
        // increment the usage count, and then decrement it
        localRegistry.incrementUsage();
        // still exists?
        assertTrue(localRegistry.exists(localRef));
        localRegistry.decrementUsage();
        // now it doesn't.
        assertFalse(localRegistry.exists(localRef));
    }

    @Test
    public void childRegistriesGetNewId() {
        final int count = VariableRegistry.getRegistryCount();
        assertThat(globalRegistry.toString(), equalTo("'parent " + count + "' #vars 0 #usage 0"));

        final VariableRegistry childLocalRegistry = new VariableRegistry(scala.Option.apply(globalRegistry));
        assertThat(childLocalRegistry.toString(), equalTo("'child " + (count + 1)  + "' #vars 0 #usage 0"));

        final VariableRegistry grandChildLocalRegistry = new VariableRegistry(scala.Option.apply(childLocalRegistry));
        assertThat(grandChildLocalRegistry.toString(), equalTo("'child " + (count + 2) + "' #vars 0 #usage 0"));
    }
}
