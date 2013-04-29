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

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.devzendo.shell.PluginVariations.*;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.collection.immutable.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestPluginMethodScanner {
    private static final scala.Option<Integer> none = scala.Option.apply(null);
    
    @BeforeClass
    public static void setUpLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    final PluginMethodScanner scanner = new PluginMethodScanner();
    
    @Test
    public void voidReturnNoArgsOkBadInitialiseNotScanned() {
        final AnalysedMethod analysedMethod = getAnalysedMethod(scanner.scanPluginMethods(new VoidReturnNoArgsBadPluginMethodsNotScanned()));
        positionsAre(analysedMethod, none, none, none, none);
    }
    
    @Test
    public void voidReturnNoArgsBadSignature() {
        final Map<String, AnalysedMethod> map = scanner.scanPluginMethods(new VoidReturnNoArgsBadSignature());
        noMethods(map);
    }
    
    @Test
    public void voidReturnNoArgsOk() {
        final AnalysedMethod analysedMethod = getAnalysedMethod(scanner.scanPluginMethods(new VoidReturnNoArgs()));
        positionsAre(analysedMethod, none, none, none, none);
    }

    @Test
    public void voidReturnArrayListArgsNotOk() { // Must be exactly a List
        noMethods(scanner.scanPluginMethods(new VoidReturnArrayListArgs()));
    }
    
    @Test
    public void voidReturnJavaListArgsInputPipeOutputPipeLogOk() {
        final AnalysedMethod analysedMethod = getAnalysedMethod(scanner.scanPluginMethods(new VoidReturnListArgsInputPipeOutputPipeLog()));
        positionsAre(analysedMethod, scala.Option.apply(0), scala.Option.apply(1), scala.Option.apply(2), scala.Option.apply(3));
        assertThat(analysedMethod.isScalaArgumentsList(), equalTo(false));
    }

    @Test
    public void voidReturnScalaListArgsInputPipeOutputPipeLogOk() {
        final AnalysedMethod analysedMethod = getAnalysedMethod(scanner.scanPluginMethods(new VoidReturnScalaListArgsInputPipeOutputPipeLog()));
        positionsAre(analysedMethod, scala.Option.apply(0), scala.Option.apply(1), scala.Option.apply(2), scala.Option.apply(3));
        assertThat(analysedMethod.isScalaArgumentsList(), equalTo(true));
    }

    @Test
    public void aliasOk() {
        final Map<String,AnalysedMethod> map = scanner.scanPluginMethods(new Alias());
        assertThat(map.size(), equalTo(2));
        assertThat(map.contains("funk"), equalTo(true));
        assertThat(map.contains("jazz"), equalTo(true));
        assertThat(map.get("funk").get(), equalTo(map.get("jazz").get()));
    }

    @Test
    public void nameAnnotationCanOverrideOk() {
        final Map<String,AnalysedMethod> map = scanner.scanPluginMethods(new NameOverride());
        assertThat(map.size(), equalTo(1));
        assertThat(map.contains("Command-Me-O-Master"), equalTo(true));
    }

    private AnalysedMethod getAnalysedMethod(final scala.collection.immutable.Map<String, AnalysedMethod> map) {
        assertThat(map.size(), equalTo(1));
        final scala.collection.Iterator<String> it = map.keysIterator();
        assertThat(it.hasNext(), equalTo(true));
        assertThat(it.next(), equalTo("funk"));
        return map.apply("funk");
    }

    private void positionsAre(final AnalysedMethod analysedMethod,
            final scala.Option<Integer> argPos,
            final scala.Option<Integer> inputPipePos,
            final scala.Option<Integer> outputPipePos,
            final scala.Option<Integer> logPos) {
        assertThat(analysedMethod.getArgumentsPosition(), equalTo(argPos));
        assertThat(analysedMethod.getInputPipePosition(), equalTo(inputPipePos));
        assertThat(analysedMethod.getOutputPipePosition(), equalTo(outputPipePos));
        assertThat(analysedMethod.getLogPosition(), equalTo(logPos));
    }

    private void noMethods(scala.collection.immutable.Map<String, AnalysedMethod> map) {
        assertThat(map.size(), equalTo(0));
    }
}
