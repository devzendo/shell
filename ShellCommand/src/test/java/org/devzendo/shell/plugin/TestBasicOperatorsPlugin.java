package org.devzendo.shell.plugin;

import org.apache.log4j.BasicConfigurator;
import org.devzendo.shell.ast.Switch;
import org.devzendo.shell.ast.VariableReference;
import org.devzendo.shell.interpreter.*;
import org.devzendo.shell.pipe.NullInputPipe;
import org.devzendo.shell.pipe.VariableOutputPipe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.collection.immutable.List;
import scala.util.matching.Regex;

import static org.devzendo.shell.ScalaListHelper.createList;
import static org.devzendo.shell.ScalaListHelper.createObjectList;
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
public class TestBasicOperatorsPlugin {
    private static final scala.Option<VariableRegistry> noneVariableRegistry = scala.Option.apply(null);
    final VariableRegistry varReg = new VariableRegistry(noneVariableRegistry);
    final BasicOperatorsPlugin plugin = new BasicOperatorsPlugin();

    final NullInputPipe inputPipe = new NullInputPipe();
    final Variable outputVariable = new Variable();
    final VariableOutputPipe outputPipe = new VariableOutputPipe(outputVariable);

    // test data ---------------------------------------------------------------
    // This list is used for validating argument types. The framework validates
    // each arg early, not after zip, so can't report all validation failures
    // together.
    final List<Object> whateverSwitchRegexStringList = createObjectList(
            new Switch("Whatever"),
            new Regex("/foo/", createList(new String[0])),
            "String",
            true
    );

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        final ExecutionEnvironment execEnv = new ExecutionEnvironment() {
            @Override
            public List<String> argList() {
                return null;
            }

            @Override
            public CommandRegistry commandRegistry() {
                return null;
            }

            @Override
            public VariableRegistry variableRegistry() {
                return varReg;
            }

            @Override
            public PluginRegistry pluginRegistry() {
                return null;
            }
        };
        plugin.initialise(execEnv);
    }


    // operator framework ------------------------------------------------------

    private void assertAddition(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.plus(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertAdditionFails(List<Object> inputs, String message) {
        try {
            plugin.plus(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    // these two tests test the basic operator framework via addition; the
    // actual addition of different types is covered below...
    @Test
    public void additionVariablesExpandedAndOtherArgsConvertedToListsPaddedWithZero() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(1);
        argVar.add(2);
        argVar.add(3);
        // the 1 of 1,2,3 is added to 4, and the 2 and 3 of 2,3 are added to
        // zeroes:
        //   +   =
        // 1   4   5
        // 2   0   2
        // 3   0   3
        assertAddition(createObjectList(argVar, 4), createObjectList(5, 2, 3));
    }

    @Test
    public void additionVariableReferencesExpandedAndOtherArgsConvertedToListsPaddedWithZero() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(1);
        argVar.add(2);
        argVar.add(3);
        final VariableReference argVarRef = new VariableReference("myvar");
        varReg.setVariable(argVarRef, argVar);
        // the 1 of 1,2,3 is added to 4, and the 2 and 3 of 2,3 are added to
        // zeroes:
        //   +   =
        // 1   4   5
        // 2   0   2
        // 3   0   3
        assertAddition(createObjectList(argVarRef, 4), createObjectList(5, 2, 3));
    }

    // plus --------------------------------------------------------------------

    @Test
    public void additionOfStrings() throws CommandExecutionException {
        assertAddition(createObjectList("one", "two"), createObjectList("onetwo"));
    }

    @Test
    public void additionOfStringAndInteger() throws CommandExecutionException {
        assertAddition(createObjectList("one", 1), createObjectList("one1"));
    }

    @Test
    public void additionOfStringAndDouble() throws CommandExecutionException {
        assertAddition(createObjectList("one", 1.3), createObjectList("one1.3"));
    }

    @Test
    public void additionOfStringAndBoolean() {
        assertAdditionFails(createObjectList("one", false), "Cannot add String 'one' to Boolean 'false'");
    }

    @Test
    public void additionOfIntegerAndString() throws CommandExecutionException {
        assertAddition(createObjectList(1, "one"), createObjectList("1one"));
    }

    @Test
    public void additionOfIntegers() throws CommandExecutionException {
        assertAddition(createObjectList(1, 2), createObjectList(3));
    }

    @Test
    public void additionOfIntegerAndDouble() throws CommandExecutionException {
        assertAddition(createObjectList(1, 3.2), createObjectList(4.2));
    }

    @Test
    public void additionOfIntegerAndBoolean() {
        assertAdditionFails(createObjectList(1, true), "Cannot add Integer '1' to Boolean 'true'");
    }

    @Test
    public void additionOfDoubleAndString() throws CommandExecutionException {
        assertAddition(createObjectList(1.3, "one"), createObjectList("1.3one"));
    }

    @Test
    public void additionOfDoubleAndInteger() throws CommandExecutionException {
        assertAddition(createObjectList(4.1, 1), createObjectList(5.1));
    }

    @Test
    public void additionOfDoubles() throws CommandExecutionException {
        assertAddition(createObjectList(4.1, 3.5), createObjectList(7.6));
    }

    @Test
    public void additionOfDoubleAndBoolean() {
        assertAdditionFails(createObjectList(1.5, true), "Cannot add Double '1.5' to Boolean 'true'");
    }

    @Test
    public void additionOfBooleanAndString() throws CommandExecutionException {
        assertAdditionFails(createObjectList(false, "food"), "Cannot add Boolean 'false' to String 'food'");
    }

    @Test
    public void additionOfBooleanAndInteger() {
        assertAdditionFails(createObjectList(true, 1), "Cannot add Boolean 'true' to Integer '1'");
    }

    @Test
    public void additionOfBooleanAndDouble() {
        assertAdditionFails(createObjectList(true, 1.5), "Cannot add Boolean 'true' to Double '1.5'");
    }

    @Test
    public void additionOfBooleans1() throws CommandExecutionException { // it's disjunction
        assertAddition(createObjectList(false, false), createObjectList(false));
    }
    @Test
    public void additionOfBooleans2() throws CommandExecutionException { // it's disjunction
        assertAddition(createObjectList(false, true), createObjectList(true));
    }
    @Test
    public void additionOfBooleans3() throws CommandExecutionException { // it's disjunction
        assertAddition(createObjectList(true, false), createObjectList(true));
    }
    @Test
    public void additionOfBooleans4() throws CommandExecutionException { // it's disjunction
        assertAddition(createObjectList(true, true), createObjectList(true));
    }

    @Test
    public void additionDoesNotAllowSwitches() {
        assertAdditionFails(
                whateverSwitchRegexStringList,
                "Cannot add the Switch 'Switch(Whatever)'");
    }

    @Test
    public void additionOfDisallowedTypesInVariablesIsDisallowed() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(new Switch("baloney"));
        assertAdditionFails(createObjectList(argVar), "Cannot add the Switch 'Switch(baloney)'");
    }


    // minus -------------------------------------------------------------------
    private void assertSubtraction(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.minus(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertSubtractionFails(List<Object> inputs, String message) {
        try {
            plugin.minus(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void subtractionOfIntegers() throws CommandExecutionException {
        assertSubtraction(createObjectList(1, 2), createObjectList(-1));
    }

    @Test
    public void subtractionOfIntegerAndDouble() throws CommandExecutionException {
        assertSubtraction(createObjectList(1, 3.2), createObjectList(-2.2));
    }

    @Test
    public void subtractionOfDoubleAndInteger() throws CommandExecutionException {
        assertSubtraction(createObjectList(4.5, 1), createObjectList(3.5));
    }

    @Test
    public void subtractionOfDoubles() throws CommandExecutionException {
        assertSubtraction(createObjectList(4.8, 3.4), createObjectList(1.4));
    }

    @Test
    public void subtractionDoesNotAllowSwitchesStringsOrBooleans() {
        assertSubtractionFails(
                whateverSwitchRegexStringList,
                "Cannot subtract the Switch 'Switch(Whatever)'");
    }

    @Test
    public void negationOfDouble() throws CommandExecutionException {
        assertSubtraction(createObjectList(3.5), createObjectList(-3.5));
    }

    @Test
    public void negationOfInteger() throws CommandExecutionException {
        assertSubtraction(createObjectList(9), createObjectList(-9));
    }

    @Test
    public void subtractionOfDisallowedTypesInVariablesIsDisallowed() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(new Switch("baloney"));
        assertSubtractionFails(createObjectList(argVar), "Cannot subtract the Switch 'Switch(baloney)'");
    }

    // times -------------------------------------------------------------------
    private void assertMultiplication(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.times(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertMultiplicationFails(List<Object> inputs, String message) {
        try {
            plugin.times(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void multiplicationOfIntegers() throws CommandExecutionException {
        assertMultiplication(createObjectList(4, 5), createObjectList(20));
    }

    @Test
    public void multiplicationOfIntegerAndDouble() throws CommandExecutionException {
        assertMultiplication(createObjectList(4, 5.2), createObjectList(20.8));
    }

    @Test
    public void multiplicationOfDoubleAndInteger() throws CommandExecutionException {
        assertMultiplication(createObjectList(4.5, 3), createObjectList(13.5));
    }

    @Test
    public void multiplicationOfDoubles() throws CommandExecutionException {
        assertMultiplication(createObjectList(2.5, 4.7), createObjectList(11.75));
    }

    @Test
    public void multiplicationDoesNotAllowSwitchesStringsOrBooleans() {
        assertMultiplicationFails(
                whateverSwitchRegexStringList,
                "Cannot multiply the Switch 'Switch(Whatever)'");
    }

    @Test
    public void multiplicationOfStringAndIntegerReplicates() throws CommandExecutionException {
        assertMultiplication(createObjectList("abc", 3), createObjectList("abcabcabc"));
    }

    @Test
    public void multiplicationOfIntegerAndStringReplicates() throws CommandExecutionException {
        assertMultiplication(createObjectList(4, "ab"), createObjectList("abababab"));
    }

    @Test
    public void multiplicationOfZeroIntegerAndStringReplicatesToEmpty() throws CommandExecutionException {
        assertMultiplication(createObjectList(0, "ab"), createObjectList(""));
    }

    @Test
    public void multiplicationOfNegativeIntegerAndStringFails() throws CommandExecutionException {
        assertMultiplicationFails(createObjectList(-1, "ab"), "Cannot replicate the String 'ab' by the negative Integer '-1'");
    }

    @Test
    public void multiplicationOfDoubleAndStringFails() throws CommandExecutionException {
        assertMultiplicationFails(createObjectList(2.3, "ab"), "Cannot replicate the String 'ab' by the Double '2.3'");
    }

    @Test
    public void multiplicationOfStringAndDoubleFails() throws CommandExecutionException {
        assertMultiplicationFails(createObjectList("ab", 2.3), "Cannot replicate the String 'ab' by the Double '2.3'");
    }

    @Test
    public void multiplicationOfDisallowedTypesInVariablesIsDisallowed() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(new Switch("baloney"));
        assertMultiplicationFails(createObjectList(argVar), "Cannot multiply the Switch 'Switch(baloney)'");
    }

    // divide ------------------------------------------------------------------
    private void assertDivision(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.divide(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertDivisionFails(List<Object> inputs, String message) {
        try {
            plugin.divide(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void divisionOfIntegers() throws CommandExecutionException {
        assertDivision(createObjectList(10, 5), createObjectList(2));
    }

    @Test
    public void divisionOfIntegerAndDouble() throws CommandExecutionException {
        assertDivision(createObjectList(10, 5.0), createObjectList(2.0));
    }

    @Test
    public void divisionOfDoubleAndInteger() throws CommandExecutionException {
        assertDivision(createObjectList(4.5, 3), createObjectList(1.5));
    }

    @Test
    public void divisionOfDoubles() throws CommandExecutionException {
        assertDivision(createObjectList(11.25, 2.5), createObjectList(4.5));
    }

    @Test
    public void divisionDoesNotAllowSwitchesStringsOrBooleans() {
        assertDivisionFails(
                whateverSwitchRegexStringList,
                "Cannot divide the Switch 'Switch(Whatever)'");
        // validates each arg early, not after zip, so can't report all validation failures together
    }

    @Test
    public void divisionOfDisallowedTypesInVariablesIsDisallowed() throws CommandExecutionException {
        final Variable argVar = new Variable();
        argVar.add(new Switch("baloney"));
        assertDivisionFails(createObjectList(argVar), "Cannot divide the Switch 'Switch(baloney)'");
    }

    // logical not -------------------------------------------------------------
    private void assertLogicalNot(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.logicalNot(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertLogicalNotFails(List<Object> inputs, String message) {
        try {
            plugin.logicalNot(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void logicalNotOfBooleans1() throws CommandExecutionException {
        assertLogicalNot(createObjectList(true), createObjectList(false));
    }

    @Test
    public void logicalNotOfBooleans2() throws CommandExecutionException {
        assertLogicalNot(createObjectList(false), createObjectList(true));
    }

    @Test
    public void logicalNotDoesNotAllowSwitches() {
        assertLogicalNotFails(
                createObjectList(new Switch("Whatever")),
                "Cannot negate the Switch 'Switch(Whatever)'");
    }

    @Test
    public void logicalNotDoesNotAllowIntegers() {
        assertLogicalNotFails(
                createObjectList(3),
                "Cannot negate the Integer '3'");
    }

    @Test
    public void logicalNotDoesNotAllowDoubles() {
        assertLogicalNotFails(
                createObjectList(3.7),
                "Cannot negate the Double '3.7'");
    }

    @Test
    public void logicalNotIsUnary() {
        assertLogicalNotFails(
                createObjectList(true, false),
                "Boolean negation is a unary operation");
    }

    // modulus -----------------------------------------------------------------
    private void assertModulus(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.mod(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertModulusFails(List<Object> inputs, String message) {
        try {
            plugin.mod(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void modulusOfIntegers() throws CommandExecutionException {
        assertModulus(createObjectList(11, 4), createObjectList(3));
    }

    @Test
    public void modulusDoesNotAllowDoubles() {
        assertModulusFails(
                createObjectList(3.7),
                "Cannot take the modulus of the Double '3.7'");
    }

    @Test
    public void modulusDoesNotAllowSwitches() {
        assertModulusFails(
                createObjectList(new Switch("Whatever")),
                "Cannot take the modulus of the Switch 'Switch(Whatever)'");
    }

    // bitwise xor -------------------------------------------------------------
    private void assertXor(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.bitwiseXor(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertXorFails(List<Object> inputs, String message) {
        try {
            plugin.bitwiseXor(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void bitwiseXorOfIntegers() throws CommandExecutionException {
        // 1011 xor 1100 = 0111
        assertXor(createObjectList(11, 12), createObjectList(7));
    }

    @Test
    public void bitwiseXorOfIntegerAndBoolean1() throws CommandExecutionException {
        // 1011 xor 0001 = 1010
        assertXor(createObjectList(11, true), createObjectList(10));
    }

    @Test
    public void bitwiseXorOfIntegerAndBoolean2() throws CommandExecutionException {
        // 1011 xor 0000 = 1011
        assertXor(createObjectList(11, false), createObjectList(11));
    }

    @Test
    public void bitwiseXorOfBooleanAndInteger1() throws CommandExecutionException {
        // 0001 xor 1011 = 1010
        assertXor(createObjectList(true, 11), createObjectList(10));
    }

    @Test
    public void bitwiseXorOfBooleanAndInteger2() throws CommandExecutionException {
        // 0000 xor 1011 = 1011
        assertXor(createObjectList(false, 11), createObjectList(11));
    }

    @Test
    public void bitwiseXorOfBooleans1() throws CommandExecutionException {
        assertXor(createObjectList(false, false), createObjectList(false));
    }

    @Test
    public void bitwiseXorOfBooleans2() throws CommandExecutionException {
        assertXor(createObjectList(false, true), createObjectList(true));
    }

    @Test
    public void bitwiseXorOfBooleans3() throws CommandExecutionException {
        assertXor(createObjectList(true, false), createObjectList(true));
    }

    @Test
    public void bitwiseXorOfBooleans4() throws CommandExecutionException {
        assertXor(createObjectList(true, true), createObjectList(false));
    }

    @Test
    public void xorDoesNotAllowDoubles() {
        assertXorFails(
                createObjectList(3.7),
                "Cannot bitwise xor the Double '3.7'");
    }

    @Test
    public void xorDoesNotAllowSwitches() {
        assertXorFails(
                createObjectList(new Switch("Whatever")),
                "Cannot bitwise xor the Switch 'Switch(Whatever)'");
    }


    // bitwise or --------------------------------------------------------------
    private void assertOr(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.bitwiseOr(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertOrFails(List<Object> inputs, String message) {
        try {
            plugin.bitwiseOr(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void bitwiseOrOfIntegers() throws CommandExecutionException {
        // 1011 or 1100 = 1111
        assertOr(createObjectList(11, 12), createObjectList(15));
    }

    @Test
    public void bitwiseOrOfIntegerAndBoolean1() throws CommandExecutionException {
        // 1011 or 0001 = 1011
        assertOr(createObjectList(11, true), createObjectList(11));
    }

    @Test
    public void bitwiseOrOfIntegerAndBoolean2() throws CommandExecutionException {
        // 1011 or 0000 = 1011
        assertOr(createObjectList(11, false), createObjectList(11));
    }

    @Test
    public void bitwiseOrOfBooleanAndInteger1() throws CommandExecutionException {
        // 0001 or 1011 = 1011
        assertOr(createObjectList(true, 11), createObjectList(11));
    }

    @Test
    public void bitwiseOrOfBooleanAndInteger2() throws CommandExecutionException {
        // 0000 or 1011 = 1011
        assertOr(createObjectList(false, 11), createObjectList(11));
    }

    @Test
    public void bitwiseOrOfBooleans1() throws CommandExecutionException {
        assertOr(createObjectList(false, false), createObjectList(false));
    }

    @Test
    public void bitwiseOrOfBooleans2() throws CommandExecutionException {
        assertOr(createObjectList(false, true), createObjectList(true));
    }

    @Test
    public void bitwiseOrOfBooleans3() throws CommandExecutionException {
        assertOr(createObjectList(true, false), createObjectList(true));
    }

    @Test
    public void bitwiseOrOfBooleans4() throws CommandExecutionException {
        assertOr(createObjectList(true, true), createObjectList(true));
    }

    @Test
    public void orDoesNotAllowDoubles() {
        assertOrFails(
                createObjectList(3.7),
                "Cannot bitwise or the Double '3.7'");
    }

    @Test
    public void orDoesNotAllowSwitches() {
        assertOrFails(
                createObjectList(new Switch("Whatever")),
                "Cannot bitwise or the Switch 'Switch(Whatever)'");
    }

    // bitwise and -------------------------------------------------------------
    private void assertAnd(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.bitwiseAnd(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertAndFails(List<Object> inputs, String message) {
        try {
            plugin.bitwiseAnd(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void bitwiseAndOfIntegers() throws CommandExecutionException {
        // 1011 and 1100 = 1000
        assertAnd(createObjectList(11, 12), createObjectList(8));
    }

    @Test
    public void bitwiseAndOfIntegerAndBoolean1() throws CommandExecutionException {
        // 1011 and 0001 = 0001
        assertAnd(createObjectList(11, true), createObjectList(1));
    }

    @Test
    public void bitwiseAndOfIntegerAndBoolean2() throws CommandExecutionException {
        // 1011 and 0000 = 0000
        assertAnd(createObjectList(11, false), createObjectList(0));
    }

    @Test
    public void bitwiseAndOfBooleanAndInteger1() throws CommandExecutionException {
        // 0001 and 1011 = 0001
        assertAnd(createObjectList(true, 11), createObjectList(1));
    }

    @Test
    public void bitwiseAndOfBooleanAndInteger2() throws CommandExecutionException {
        // 0000 and 1011 = 0000
        assertAnd(createObjectList(false, 11), createObjectList(0));
    }

    @Test
    public void bitwiseAndOfBooleans1() throws CommandExecutionException {
        assertAnd(createObjectList(false, false), createObjectList(false));
    }

    @Test
    public void bitwiseAndOfBooleans2() throws CommandExecutionException {
        assertAnd(createObjectList(false, true), createObjectList(false));
    }

    @Test
    public void bitwiseAndOfBooleans3() throws CommandExecutionException {
        assertAnd(createObjectList(true, false), createObjectList(false));
    }

    @Test
    public void bitwiseAndOfBooleans4() throws CommandExecutionException {
        assertAnd(createObjectList(true, true), createObjectList(true));
    }

    @Test
    public void andDoesNotAllowDoubles() {
        assertAndFails(
                createObjectList(3.7),
                "Cannot bitwise and the Double '3.7'");
    }

    @Test
    public void andDoesNotAllowSwitches() {
        assertAndFails(
                createObjectList(new Switch("Whatever")),
                "Cannot bitwise and the Switch 'Switch(Whatever)'");
    }

    // bitwise complement ------------------------------------------------------
    private void assertComplement(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.bitwiseComplement(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertComplementFails(List<Object> inputs, String message) {
        try {
            plugin.bitwiseComplement(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void complementOfBooleans1() throws CommandExecutionException {
        assertComplement(createObjectList(true), createObjectList(false));
    }

    @Test
    public void complementOfBooleans2() throws CommandExecutionException {
        assertComplement(createObjectList(false), createObjectList(true));
    }

    @Test
    public void complementOfInteger() throws CommandExecutionException {
        assertComplement(createObjectList(1), createObjectList(-2));
    }

    @Test
    public void complementDoesNotAllowSwitches() {
        assertComplementFails(
                createObjectList(new Switch("Whatever")),
                "Cannot complement the Switch 'Switch(Whatever)'");
    }

    @Test
    public void complementDoesNotAllowDoubles() {
        assertComplementFails(
                createObjectList(3.7),
                "Cannot complement the Double '3.7'");
    }

    @Test
    public void complementIsUnary() {
        assertComplementFails(
                createObjectList(true, false),
                "Bitwise complement is a unary operation");
    }

    // logical and -------------------------------------------------------------
    private void assertLAnd(List<Object> inputs, List<Object> outputs) throws CommandExecutionException {
        plugin.logicalAnd(inputPipe, outputPipe, inputs);
        assertThat(outputVariable.get(), equalTo(outputs));
    }

    private void assertLAndFails(List<Object> inputs, String message) {
        try {
            plugin.logicalAnd(inputPipe, outputPipe, inputs);
            Assert.fail("Expected a CommandExecutionException");
        } catch (CommandExecutionException e) {
            assertThat(e.getMessage(), equalTo(message));
        }
    }

    @Test
    public void logicalAndOfBooleans1() throws CommandExecutionException {
        assertLAnd(createObjectList(false, false), createObjectList(false));
    }

    @Test
    public void logicalAndOfBooleans2() throws CommandExecutionException {
        assertLAnd(createObjectList(false, true), createObjectList(false));
    }

    @Test
    public void logicalAndOfBooleans3() throws CommandExecutionException {
        assertLAnd(createObjectList(true, false), createObjectList(false));
    }

    @Test
    public void logicalAndOfBooleans4() throws CommandExecutionException {
        assertLAnd(createObjectList(true, true), createObjectList(true));
    }

    @Test
    public void logicalAndDoesNotAllowDoubles() {
        assertLAndFails(
                createObjectList(3.7),
                "Cannot logically and the Double '3.7'");
    }

    @Test
    public void logicalAndDoesNotAllowIntegers() {
        assertLAndFails(
                createObjectList(3),
                "Cannot logically and the Integer '3'");
    }

    @Test
    public void logicalAndDoesNotAllowSwitches() {
        assertLAndFails(
                createObjectList(new Switch("Whatever")),
                "Cannot logically and the Switch 'Switch(Whatever)'");
    }
}
