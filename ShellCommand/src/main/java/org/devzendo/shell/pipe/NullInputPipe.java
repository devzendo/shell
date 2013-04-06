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

import scala.Option;

/**
 * An input pipe that's immediately empty.
 * @author matt
 *
 */
public class NullInputPipe implements InputPipe {
    @Override
    public void setTerminated() {
        // do nothing
    }

    @Override
    public Option<Object> next() {
        return scala.Option.apply(null); // a.k.a. None
    }
}
