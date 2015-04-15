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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import scala.Option;

public class OptionMatcher<T> extends TypeSafeMatcher<Option<T>> {
    private static final Option<Object> none = scala.Option.apply(null); // a.k.a. None
    private final Option<T> mMatchingT;

    public OptionMatcher(Option<T> matchingT) {
        mMatchingT = matchingT;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("an Option matching ");
        desc.appendValue(mMatchingT);
    }

    @Override
    public boolean matchesSafely(Option<T> item) {
        boolean itemIsNone = item.equals(none);
        boolean matchingIsNone = mMatchingT.equals(none);
        if (itemIsNone && matchingIsNone) {
            return true;
        }
        if (itemIsNone != matchingIsNone) {
            return false;
        }
        return mMatchingT.get().equals(item.get());
    }
    
    public static <T> OptionMatcher<T> isSome(T t) {
        return new OptionMatcher<T>(scala.Option.apply(t));
    }

    public static <T> OptionMatcher<T> isNone() {
        Option<T> none = scala.Option.apply(null); // a.k.a. None
        return new OptionMatcher<T>(none);
    }

}