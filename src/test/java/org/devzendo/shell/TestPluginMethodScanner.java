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

import java.util.Iterator;
import java.util.Map;

import org.devzendo.commoncode.logging.LoggingUnittestHelper;
import org.devzendo.shell.PluginMethodScanner.AnalysedMethod;
import org.devzendo.shell.PluginVariations.VoidReturnListArgs;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsInputPipeOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnListArgsOutputPipeInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgs;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsBadPluginMethodsNotScanned;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsInputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsInputPipeOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsOutputPipe;
import org.devzendo.shell.PluginVariations.VoidReturnNoArgsOutputPipeInputPipe;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPluginMethodScanner {
    
    @BeforeClass
    public static void setUpLogging() {
        LoggingUnittestHelper.setupLogging();
    }
    
    final PluginMethodScanner scanner = new PluginMethodScanner();
    
    @Test
    public void voidReturnNoArgsOkBadInitialiseNotScanned() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsBadPluginMethodsNotScanned()));
    }

    @Test
    public void voidReturnNoArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgs()));
    }

    @Test
    public void voidReturnNoArgsInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsInputPipe()));
    }

    @Test
    public void voidReturnNoArgsOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsOutputPipe()));
    }

    @Test
    public void voidReturnNoArgsOutputPipeInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsOutputPipeInputPipe()));
    }

    @Test
    public void voidReturnNoArgsInputPipeOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnNoArgsInputPipeOutputPipe()));
    }

    @Test
    public void voidReturnListArgsOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgs()));
    }

    @Test
    public void voidReturnListArgsInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsInputPipe()));
    }

    @Test
    public void voidReturnListArgsOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsOutputPipe()));
    }
    
    @Test
    public void voidReturnListArgsInputPipeOutputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsInputPipeOutputPipe()));
    }

    @Test
    public void voidReturnListArgsOutputPipeInputPipeOk() {
        gotFunk(scanner.scanPluginMethods(new VoidReturnListArgsOutputPipeInputPipe()));
    }

    private void gotFunk(final Map<String, AnalysedMethod> map) {
        assertThat(map.size(), equalTo(1));
        final Iterator<String> it = map.keySet().iterator();
        assertThat(it.hasNext(), equalTo(true));
        assertThat(it.next(), equalTo("funk"));
    }
}
