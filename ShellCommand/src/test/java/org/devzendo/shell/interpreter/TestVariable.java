package org.devzendo.shell.interpreter;

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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestVariable {
    final Variable v = new Variable();
    final Integer i = 10;

    @Test
    public void emptyInitially() {
        assertThat(v.size(), equalTo(0));
    }

    @Test
    public void canBeAddedTo() {
        v.add(i);

        assertThat(v.size(), equalTo(1));
        assertThat((Integer)v.get(0), equalTo(i));
        assertThat(v.get().size(), equalTo(1));
        assertThat((Integer)v.get().apply(0), equalTo(i));
    }

    @Test
    public void closeRemovesContents() {
        v.add(i);

        v.close();

        assertThat(v.size(), equalTo(0));
        assertThat(v.get().size(), equalTo(0));
    }
}
