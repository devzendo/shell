package org.devzendo.shell.plugin;

import org.apache.log4j.BasicConfigurator;
import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.ast.BlockStatements;
import org.devzendo.shell.ast.Switch;
import org.devzendo.shell.ast.VariableReference;
import org.devzendo.shell.interpreter.CommandExecutionException;
import org.devzendo.shell.interpreter.DefaultVariableRegistry;
import org.devzendo.shell.interpreter.Variable;
import org.devzendo.shell.interpreter.VariableRegistry;
import org.devzendo.shell.pipe.VariableOutputPipe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.collection.immutable.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class TestControlShellPlugin {
    private static final scala.Option<VariableRegistry> noneVariableRegistry = scala.Option.apply(null);
    final VariableRegistry varReg = new DefaultVariableRegistry(noneVariableRegistry);
    final ControlShellPlugin plugin = new ControlShellPlugin();
    final Variable outputVariable = new Variable();
    final VariableOutputPipe outputPipe = new VariableOutputPipe(outputVariable);
    final BlockStatements thenBlock = new BlockStatements();
    final BlockStatements elseBlock = new BlockStatements();
    final BlockStatements spuriousBlock = new BlockStatements();


    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
    }

    private void assertConditional(List<Object> args, List<Object> outputs) throws CommandExecutionException {
        callConditional(args);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void callConditional(List<Object> args) throws CommandExecutionException {
        plugin.conditionalBlockExecution(varReg, outputPipe, args);
    }

    private void assertConditionalFails(List<Object> args, String message) {
        try {
            callConditional(args);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void conditionalFirstArgOfBooleanOk() throws CommandExecutionException {
        callConditional(ScalaListHelper.createObjectList(Boolean.FALSE));
        // doesn't throw
    }

    private List<Object> createArgsContainingVariableReferenceContaining(Object arg) {
        final Variable variable = new Variable();
        variable.add(arg);
        final VariableReference varRef = new VariableReference("v");
        varReg.setVariable(varRef, variable);
        return ScalaListHelper.createObjectList(varRef);
    }

    private List<Object> createArgsContainingVariableContaining(Object arg) {
        final Variable variable = new Variable();
        variable.add(arg);
        return ScalaListHelper.createObjectList(variable);
    }

    @Test
    public void conditionalFirstArgOfVariableContainingBooleanOk() throws CommandExecutionException {
        callConditional(createArgsContainingVariableContaining(Boolean.FALSE));
        // doesn't throw
    }

    @Test
    public void conditionalFirstArgOfVariableReferenceContainingBooleanOk() throws CommandExecutionException {
        callConditional(createArgsContainingVariableReferenceContaining(Boolean.FALSE));
        // doesn't throw
    }

    @Test
    public void conditionalFirstArgOfVariableContainingIntegerNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableContaining(5));
    }

    @Test
    public void conditionalFirstArgOfVariableReferenceContainingIntegerNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableReferenceContaining(5));
    }

    @Test
    public void conditionalFirstArgOfVariableContainingDoubleNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableContaining(5.3));
    }

    @Test
    public void conditionalFirstArgOfVariableReferenceContainingDoubleNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableReferenceContaining(5.3));
    }

    @Test
    public void conditionalFirstArgOfVariableContainingSwitchNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableContaining(new Switch("ss")));
    }

    @Test
    public void conditionalFirstArgOfVariableReferenceContainingSwitchNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(createArgsContainingVariableReferenceContaining(new Switch("ww")));
    }

    private void assertConditionalFirstArgMustYieldBoolean(List<Object> args) {
        assertConditionalFails(args, "Argument to if must yield Boolean");
    }

    @Test
    public void conditionalFirstArgOfSwitchNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(ScalaListHelper.createObjectList(new Switch("sw")));
    }

    @Test
    public void conditionalFirstArgOfIntegerNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(ScalaListHelper.createObjectList(5));
    }

    @Test
    public void conditionalFirstArgOfDoubleNotOk() throws CommandExecutionException {
        assertConditionalFirstArgMustYieldBoolean(ScalaListHelper.createObjectList(5.3));
    }

    @Test
    public void conditionalBooleanThenBlockOk() throws CommandExecutionException {
        callConditional(ScalaListHelper.createObjectList(Boolean.FALSE, thenBlock));
        // doesn't throw
    }

    @Test
    public void conditionalBooleanThenElseBlockOk() throws CommandExecutionException {
        callConditional(ScalaListHelper.createObjectList(Boolean.FALSE, thenBlock, elseBlock));
        // doesn't throw
    }

    // first arg must be Boolean; this tests the rest of the args
    private void assertConditionalArgsMustBeBlocks(List<Object> args) {
        assertConditionalFails(args, "Arguments to if must be a Boolean, and up to two blocks");
    }

    @Test
    public void conditionalBooleanThenElseSpuriousBlockOk() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, thenBlock, elseBlock, spuriousBlock));
    }

    @Test
    public void conditionalBooleanAndNonBlockNotOk1() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, Boolean.TRUE));
    }

    @Test
    public void conditionalBooleanAndNonBlockNotOk2() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, 1));
    }

    @Test
    public void conditionalBooleanAndNonBlockNotOk3() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, 8.223));
    }

    @Test
    public void conditionalBooleanAndNonBlockNotOk4() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, "howdy"));
    }

    @Test
    public void conditionalBooleanAndNonBlockNotOk5() throws CommandExecutionException {
        assertConditionalArgsMustBeBlocks(ScalaListHelper.createObjectList(Boolean.FALSE, new Switch("xyz")));
    }

}
