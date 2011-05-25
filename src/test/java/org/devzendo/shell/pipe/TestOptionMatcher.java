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
package org.devzendo.shell.pipe;

import static org.devzendo.shell.pipe.OptionMatcher.isSome;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import scala.Option;

public class TestOptionMatcher {
    private static final Option<String> none = scala.Option.apply(null);
    private static final Option<String> some = scala.Option.apply("hello");
    
    @Test
    public void matchesNone() {
        MatcherAssert.assertThat(none, OptionMatcher.<String>isNone());
    }

    @Test
    public void matchesSome() {
        MatcherAssert.assertThat(some, isSome("hello"));
    }

    @Test
    public void someIsNotNone() {
        MatcherAssert.assertThat(some, not(OptionMatcher.<String>isNone()));
    }

    @Test
    public void noneIsNotSome() {
        MatcherAssert.assertThat(none, not(isSome("hello")));
    }

    @Test
    public void allNoneAreEqual() {
        final Option<String> anotherNone = scala.Option.<String>apply(null);
        MatcherAssert.assertThat(none.equals(anotherNone), equalTo(true));
    }

    @Test
    public void someMustMatch() {
        MatcherAssert.assertThat(some, not(isSome("world")));
    }
}
