package org.devzendo.shell.plugin;

import org.devzendo.shell.ast.VariableReference;
import org.devzendo.shell.interpreter.*;
import org.devzendo.shell.pipe.NullInputPipe;
import org.devzendo.shell.pipe.VariableOutputPipe;
import org.junit.Before;
import org.junit.Test;
import scala.collection.immutable.List;

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

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void additionOfIntegers() {
        plugin.plus(inputPipe, outputPipe, createObjectList(1, 2));

        assertThat(outputVariable.get(), equalTo(createObjectList(3)));
    }

    @Test
    public void additionVariablesExpandedAndOtherArgsConvertedToListsPaddedWithZero() {
        final Variable argVar = new Variable();
        argVar.add(1);
        argVar.add(2);
        argVar.add(3);
        plugin.plus(inputPipe, outputPipe, createObjectList(argVar, 4));
        // the 1 of 1,2,3 is added to 4, and the 2 and 3 of 2,3 are added to
        // zeroes:
        //   +   =
        // 1   4   5
        // 2   0   2
        // 3   0   3

        assertThat(outputVariable.get(), equalTo(createObjectList(5, 2, 3)));
    }

    @Test
    public void additionVariableReferencesExpandedAndOtherArgsConvertedToListsPaddedWithZero() {
        final Variable argVar = new Variable();
        argVar.add(1);
        argVar.add(2);
        argVar.add(3);
        final VariableReference argVarRef = new VariableReference("myvar");
        varReg.setVariable(argVarRef, argVar);
        plugin.plus(inputPipe, outputPipe, createObjectList(argVarRef, 4));
        // the 1 of 1,2,3 is added to 4, and the 2 and 3 of 2,3 are added to
        // zeroes:
        //   +   =
        // 1   4   5
        // 2   0   2
        // 3   0   3

        assertThat(outputVariable.get(), equalTo(createObjectList(5, 2, 3)));
    }

}
