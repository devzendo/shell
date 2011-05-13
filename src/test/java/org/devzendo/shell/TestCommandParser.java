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

import java.util.List;

import org.junit.Test;


public class TestCommandParser {
    final CommandParserS parser = new CommandParserS();

    @Test
    public void nullCommands() {
        final List<Command> cmds = parser.parse(null);
        assertThat(cmds.size(), equalTo(0));
    }

    @Test
    public void emptyCommands() {
        final List<Command> cmds = parser.parse("");
        assertThat(cmds.size(), equalTo(0));
    }
    
    @Test
    public void singleWordCommand() {
        final List<Command> cmds = parser.parse("foo");
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.get(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertThat(cmd.getArgs().size(), equalTo(0));
    }
}
