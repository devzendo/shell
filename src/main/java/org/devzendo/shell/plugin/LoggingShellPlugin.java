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
package org.devzendo.shell.plugin;

import org.apache.log4j.Logger;
import org.devzendo.shell.pipe.InputPipe;

import scala.Option;

public class LoggingShellPlugin extends AbstractShellPlugin {
    private static final Option<Integer> none = Option.apply(null);
    private static final Logger LOGGER = Logger
            .getLogger(LoggingShellPlugin.class);

    @Override
    public String getName() {
        return "Logging";
    }
    
    public void logInfo(final InputPipe inputPipe) {
        Option<Object> obj;
        while (!(obj = inputPipe.next()).equals(none)) {
            LOGGER.info(obj.get());
        }
    }
}
