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
package org.devzendo.shell.parser;

import org.devzendo.shell.ast.Command;
import org.devzendo.shell.ast.CommandPipeline;
import org.devzendo.shell.ast.Switch;
import org.devzendo.shell.ast.VariableReference;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class TestCommandParser {
    final CommandParser parser = new CommandParser();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void nullCommands() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(null);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(0));
        assertThat(pipeline.isEmpty(), equalTo(true));
    }

    @Test
    public void emptyCommands() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse("");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(0));
        assertThat(pipeline.isEmpty(), equalTo(true));
    }
    
    @Test
    public void singleWordCommand() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse("foo");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertNoArgs(cmd);
        assertThat(pipeline.isEmpty(), equalTo(false));
    }

    @Test
    public void singleWordCommandWithSwitches() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse("foo -Minus /Slash");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        final List<Object> args = cmd.getArgs();
        assertThat(args.size(), equalTo(2));
        assertThat(args.get(0), instanceOf(Switch.class));
        assertThat(((Switch) args.get(0)).switchName(), equalTo("Minus"));
        assertThat(args.get(1), instanceOf(Switch.class));
        assertThat( ((Switch)args.get(1)).switchName(), equalTo("Slash"));
    }

    @Test
    public void takeFromVariable() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(" foo < var ");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertNoArgs(cmd);
        assertThat(pipeline.getInputVariable().variableName(), equalTo("var"));
        assertThat(pipeline.getOutputVariable(), nullValue());
    }

    @Test
    public void takeFromVariableAndPipe() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(" foo < var | bar");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(2));
        final Command foo = cmds.apply(0);
        assertThat(foo.getName(), equalTo("foo"));
        assertNoArgs(foo);
        final Command bar = cmds.apply(1);
        assertThat(bar.getName(), equalTo("bar"));
        assertNoArgs(bar);

        assertThat(pipeline.getInputVariable().variableName(), equalTo("var"));
        assertThat(pipeline.getOutputVariable(), nullValue());
    }

    private void checkVariableStoring(CommandPipeline pipeline) {
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertNoArgs(cmd);
        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable().variableName(), equalTo("var"));
    }

    @Test
    public void storeIntoVariableWithDirectTo() throws CommandParserException {
        checkVariableStoring(parser.parse("foo > var"));
    }

    @Test
    public void storeIntoVariableWithAssignment() throws CommandParserException {
        checkVariableStoring(parser.parse("var = foo"));
    }

    @Test
    public void storeIntoVariableWithAssignmentAndDirectToFails() throws CommandParserException {
        exception.expect(CommandParserException.class);
        exception.expectMessage("Use one of = and >, but not both");

        checkVariableStoring(parser.parse("var = foo > var"));
    }

    @Test
    public void complexCommand() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(
            "cmd1 2.0 \"string 'hello' \" 2.3e5 6.8 ident < invar| cmd2 | cmd3 5 true false > outvar");

        assertThat(pipeline.getInputVariable().variableName(), equalTo("invar"));
        assertThat(pipeline.getOutputVariable().variableName(), equalTo("outvar"));

        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(3));
        
        final Command command1 = cmds.apply(0);
        assertThat(command1.getName(), equalTo("cmd1"));
        final List<Object> cmd1args = command1.getArgs();
        assertThat(cmd1args.size(), equalTo(5));
        assertThat(((Double)cmd1args.get(0)), closeTo(2.0, 0.001));
        assertThat(((String)cmd1args.get(1)), Matchers.equalTo("string 'hello' "));
        assertThat(((Double)cmd1args.get(2)), closeTo(230000.0, 1));
        assertThat(((Double)cmd1args.get(3)), closeTo(6.8, 0.001));
        assertThat(((VariableReference)cmd1args.get(4)).variableName(), equalTo("ident"));
        
        final Command command2 = cmds.apply(1);
        assertThat(command2.getName(), equalTo("cmd2"));
        assertNoArgs(command2);
        
        final Command command3 = cmds.apply(2);
        assertThat(command3.getName(), equalTo("cmd3"));
        final List<Object> cmd3args = command3.getArgs();
        assertThat(cmd3args.size(), equalTo(3));
        assertThat(((Integer)cmd3args.get(0)), equalTo(5));
        assertThat(((Boolean)cmd3args.get(1)), equalTo(true));
        assertThat(((Boolean)cmd3args.get(2)), equalTo(false));
    }

    @SuppressWarnings("unused")
    private void dumpArgs(final List<Object> args) {
        for (int i = 0; i < args.size(); i++) {
            final Object arg = args.get(i);
            System.out.println("#" + i + " [" + arg.getClass().getSimpleName() + "] = '" + arg + "' ");
        }
    }

    @Test
    public void parsingIntegerArguments() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(
            "cmd1 3 2.5 5");

        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        
        final Command command1 = cmds.apply(0);
        final List<Object> cmd1args = command1.getArgs();
        assertThat(cmd1args.size(), equalTo(3));
        assertThat(((Integer)cmd1args.get(0)), equalTo(3));
        assertThat(((Double)cmd1args.get(1)), closeTo(2.5, 0.1));
        assertThat(((Integer)cmd1args.get(2)), equalTo(5));
    }

    @Test
    public void simpleCommandList() throws CommandParserException {
        final CommandPipeline pipeline = parser.parse(
            "zero | one | two");
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        
        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable(), nullValue());
        
        assertThat(cmds.size(), equalTo(3));
        
        final Command command1 = cmds.apply(0);
        assertThat(command1.getName(), equalTo("zero"));
        assertNoArgs(command1);
        
        final Command command2 = cmds.apply(1);
        assertThat(command2.getName(), equalTo("one"));
        assertNoArgs(command2);
        
        final Command command3 = cmds.apply(2);
        assertThat(command3.getName(), equalTo("two"));
        assertNoArgs(command3);
    }

    private void assertNoArgs(final Command cmd) {
        assertThat(cmd.getArgs().size(), equalTo(0));
    }
}
