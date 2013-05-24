package org.devzendo.shell.pipe;

import org.devzendo.shell.interpreter.Variable;
import org.junit.Test;

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
public class TestAnonymousVariablePipe {
    @Test
    public void anonVarInitiallyEmpty() {
        final AnonymousVariablePipe var = new AnonymousVariablePipe();
        assertThat(var.contents().size(), equalTo(0));
    }

    @Test
    public void anonVarCanPushAndGet() {
        final AnonymousVariablePipe var = new AnonymousVariablePipe();
        var.push(1);
        var.push(2);

        final Variable contents = var.contents();
        assertThat(contents.size(), equalTo(2));
        assertThat((Integer) contents.get(0), equalTo(1));
        assertThat((Integer) contents.get(1), equalTo(2));
        assertThat((Integer) contents.get().apply(0), equalTo(1));
        assertThat((Integer) contents.get().apply(1), equalTo(2));
    }
}
