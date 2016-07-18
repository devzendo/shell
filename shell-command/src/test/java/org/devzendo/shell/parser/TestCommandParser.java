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

import org.apache.log4j.BasicConfigurator;
import org.devzendo.shell.analyser.SemanticAnalyser;
import org.devzendo.shell.ast.*;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;


public class TestCommandParser {
    private Set<String> validCommands = new HashSet<String>();

    private void addValidCommands(String... commands) {
        validCommands.addAll(asList(commands));
    }

    final ExistenceChecker commandExistenceChecker = new ExistenceChecker() {

        @Override
        public boolean exists(String name) {
            return validCommands.contains(name);
        }
    };

    final boolean debugParser = true;
    final SemanticAnalyser analyser = new SemanticAnalyser(commandExistenceChecker);
    final CommandParser parser = new CommandParser(commandExistenceChecker, debugParser, analyser);

    @BeforeClass
    public static void resetLogging() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void nullCommands() throws CommandParserException {
        final CommandPipeline pipeline = (CommandPipeline) parser.parse(null).apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(0));
        assertThat(pipeline.isEmpty(), equalTo(true));
    }

    @Test
    public void emptyCommands() throws CommandParserException {
        final CommandPipeline pipeline = (CommandPipeline) parser.parse("").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(0));
        assertThat(pipeline.isEmpty(), equalTo(true));
    }

    @Test
    public void commandThatIsNotDefinedIsTreatedAsAnImplicitEvalVariableReference() throws CommandParserException {
        // .. and will be failed at execution by eval
        // This used to detect that the command foo is not defined, but the language has become more complex
        // so that can't so easily be detected by the parser.
//        exception.expect(CommandParserException.class);
//        exception.expectMessage("Command 'foo' is not defined");
        final CommandPipeline pipeline = (CommandPipeline) parser.parse("foo").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));

        final Command evalCommand = (Command) cmds.apply(0);
        assertThat(evalCommand.getName(), equalTo("eval"));
        final List<Object> evalArgs = evalCommand.getArgs();
        assertThat(evalArgs.size(), equalTo(1));
        assertThat(((VariableReference) evalArgs.get(0)), equalTo(new VariableReference("foo")));
    }

    @Test
    public void variableCannotHaveSameNameAsCommandWhenRedirected() throws CommandParserException {
        addValidCommands("foo", "+");

        exception.expect(CommandParserException.class);
        exception.expectMessage("Variable 'foo' cannot have the same name as a command");
        final scala.collection.immutable.List<Statement> statementList = parser.parse("2 + 3 > foo");
        System.out.println(statementList);
    }

    @Test
    public void variableCannotHaveSameNameAsCommandWhenAssigned() throws CommandParserException {
        addValidCommands("foo", "+");

        exception.expect(CommandParserException.class);
        exception.expectMessage("Variable 'foo' cannot have the same name as a command");
        final scala.collection.immutable.List<Statement> statementList = parser.parse("foo = 2 + 3");
        System.out.println(statementList);
    }

    @Test
    public void singleWordCommand() throws CommandParserException {
        addValidCommands("foo");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("foo").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertNoArgs(cmd);
        assertThat(pipeline.isEmpty(), equalTo(false));
    }


    @Test
    public void singleWordCommandWithSwitches() throws CommandParserException {
        addValidCommands("foo");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("foo -Minus /Slash").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        final List<Object> args = cmd.getArgs();
        assertThat(args.size(), equalTo(2));
        assertThat(args.get(0), instanceOf(Switch.class));
        assertThat(((Switch) args.get(0)).switchName(), equalTo("Minus"));
        assertThat(args.get(1), instanceOf(Switch.class));
        assertThat(((Switch) args.get(1)).switchName(), equalTo("Slash"));
    }

    @Test
    public void singleWordCommandWithSuperfluousParentheses() throws CommandParserException {
        addValidCommands("foo");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("(foo)").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("foo"));
        assertNoArgs(cmd);
        assertThat(pipeline.isEmpty(), equalTo(false));
    }

    @Test
    public void takeFromVariable() throws CommandParserException {
        addValidCommands("foo");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(" foo < var ").apply(0);
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
        addValidCommands("foo", "bar");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(" foo < var | bar").apply(0);
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
        addValidCommands("foo");

        checkVariableStoring((CommandPipeline) parser.parse("foo > var").apply(0));
    }

    @Test
    public void storeIntoVariableWithAssignment() throws CommandParserException {
        addValidCommands("foo");

        checkVariableStoring((CommandPipeline) parser.parse("var = foo").apply(0));
    }

    @Test
    public void literalSingleAssignment() throws CommandParserException {
        CommandPipeline pipeline = (CommandPipeline) parser.parse("var = 5").apply(0);
        checkLiteralStoring(pipeline, equalTo(5));
    }

    @Test
    public void literalSingleRedirection() throws CommandParserException {
        CommandPipeline pipeline = (CommandPipeline) parser.parse("5 > var").apply(0);
        checkLiteralStoring(pipeline, equalTo(5));
    }

    @Test
    public void literalMultipleAssignment() throws CommandParserException {
        CommandPipeline pipeline = (CommandPipeline) parser.parse("var = true 2.0 5 \"foo\"").apply(0);
        checkLiteralStoring(pipeline, equalTo(Boolean.TRUE), closeTo(2.0, 0.001), equalTo(5), equalTo("foo"));
    }

    @Test
    public void literalMultipleRedirection() throws CommandParserException {
        CommandPipeline pipeline = (CommandPipeline) parser.parse("true 2.0 5 \"foo\" > var").apply(0);
        checkLiteralStoring(pipeline, equalTo(Boolean.TRUE), closeTo(2.0, 0.001), equalTo(5), equalTo("foo"));
    }

    @Test
    public void literalMultipleAssignmentAndRedirectionFails() throws CommandParserException {
        exception.expect(CommandParserException.class);
        exception.expectMessage("Use one of = and >, but not both");

        parser.parse("var = true 2.0 5 \"foo\" > var");
    }

    private void checkLiteralStoring(CommandPipeline pipeline, Matcher... matchers) {
        assertThat(pipeline.getOutputVariable().variableName(), equalTo("var"));

        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));
        final Command cmd = cmds.apply(0);
        assertThat(cmd.getName(), equalTo("eval"));

        final List<Object> literals = cmd.args();
        assertThat(literals.size(), equalTo(matchers.length));
        for (int i = 0; i < matchers.length; i++) {
            assertThat(literals.get(i), matchers[i]);
        }
    }

    @Test
    public void storeIntoVariableWithAssignmentAndDirectToFails() throws CommandParserException {
        addValidCommands("foo");

        exception.expect(CommandParserException.class);
        exception.expectMessage("Use one of = and >, but not both");

        checkVariableStoring((CommandPipeline) parser.parse("var = foo > var").apply(0));
    }

    @Test
    public void complexCommand() throws CommandParserException {
        addValidCommands("cmd1", "cmd2", "cmd3");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "cmd1 2.0 \"string 'hello' \" 2.3e5 6.8 ident < invar| cmd2 | cmd3 5 true false > outvar").apply(0);

        checkComplex(pipeline);
    }

    @Test
    public void complexInfixCommand() throws CommandParserException {
        addValidCommands("cmd1", "cmd2", "cmd3");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "2.0 cmd1 \"string 'hello' \" 2.3e5 6.8 ident < invar| cmd2 | 5 cmd3 true false > outvar").apply(0);

        checkComplex(pipeline);
    }

    private void checkComplex(final CommandPipeline pipeline) {
        assertThat(pipeline.getInputVariable().variableName(), equalTo("invar"));
        assertThat(pipeline.getOutputVariable().variableName(), equalTo("outvar"));

        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(3));

        final Command command1 = cmds.apply(0);
        assertThat(command1.getName(), equalTo("cmd1"));
        final List<Object> cmd1args = command1.getArgs();
        assertThat(cmd1args.size(), equalTo(5));
        assertThat(((java.lang.Double) cmd1args.get(0)), closeTo(2.0, 0.001));
        assertThat(((String) cmd1args.get(1)), Matchers.equalTo("string 'hello' "));
        assertThat(((java.lang.Double) cmd1args.get(2)), closeTo(230000.0, 1));
        assertThat(((java.lang.Double) cmd1args.get(3)), closeTo(6.8, 0.001));
        assertThat(((VariableReference) cmd1args.get(4)).variableName(), equalTo("ident"));

        final Command command2 = cmds.apply(1);
        assertThat(command2.getName(), equalTo("cmd2"));
        assertNoArgs(command2);

        final Command command3 = cmds.apply(2);
        assertThat(command3.getName(), equalTo("cmd3"));
        final List<Object> cmd3args = command3.getArgs();
        assertThat(cmd3args.size(), equalTo(3));
        assertThat(((Integer) cmd3args.get(0)), equalTo(5));
        assertThat(((Boolean) cmd3args.get(1)), equalTo(true));
        assertThat(((Boolean) cmd3args.get(2)), equalTo(false));
    }

    @Test
    public void evalCommand() throws CommandParserException {
        addValidCommands("eval");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("eval true 2.0 5 \"foo\"").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));

        final Command evalCommand = cmds.apply(0);
        assertThat(evalCommand.getName(), equalTo("eval"));
        final List<Object> evalArgs = evalCommand.getArgs();
        assertThat(evalArgs.size(), equalTo(4));
        assertThat(((java.lang.Boolean) evalArgs.get(0)), equalTo(Boolean.TRUE));
        assertThat(((java.lang.Double) evalArgs.get(1)), closeTo(2.0, 0.001));
        assertThat(((java.lang.Integer) evalArgs.get(2)), equalTo(5));
        assertThat(((String) evalArgs.get(3)), equalTo("foo"));

        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable(), nullValue());
    }

    @Test
    public void implicitEvalCommand() throws CommandParserException {
        // not necessary to addValidCommands("eval"), since there's no search for the eval command
        // it's added specifically to the Command

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("true 2.0 5 \"foo\"").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));

        final Command evalCommand = cmds.apply(0);
        assertThat(evalCommand.getName(), equalTo("eval"));
        final List<Object> evalArgs = evalCommand.getArgs();
        assertThat(evalArgs.size(), equalTo(4));
        assertThat(((java.lang.Boolean) evalArgs.get(0)), equalTo(Boolean.TRUE));
        assertThat(((java.lang.Double) evalArgs.get(1)), closeTo(2.0, 0.001));
        assertThat(((java.lang.Integer) evalArgs.get(2)), equalTo(5));
        assertThat(((String) evalArgs.get(3)), equalTo("foo"));

        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable(), nullValue());
    }

    @Test
    public void evalAssignmentCommand() throws CommandParserException {
        addValidCommands("eval");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("myvar = eval true 2.0 5 \"foo\"").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));

        final Command evalCommand = cmds.apply(0);
        assertThat(evalCommand.getName(), equalTo("eval"));
        final List<Object> evalArgs = evalCommand.getArgs();
        assertThat(evalArgs.size(), equalTo(4));
        assertThat(((java.lang.Boolean) evalArgs.get(0)), equalTo(Boolean.TRUE));
        assertThat(((java.lang.Double) evalArgs.get(1)), closeTo(2.0, 0.001));
        assertThat(((java.lang.Integer) evalArgs.get(2)), equalTo(5));
        assertThat(((String) evalArgs.get(3)), equalTo("foo"));

        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable().variableName(), equalTo("myvar"));
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
        addValidCommands("cmd1");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "cmd1 3 2.5 5").apply(0);

        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();
        assertThat(cmds.size(), equalTo(1));

        final Command command1 = cmds.apply(0);
        final List<Object> cmd1args = command1.getArgs();
        assertThat(cmd1args.size(), equalTo(3));
        assertThat(((Integer) cmd1args.get(0)), equalTo(3));
        assertThat(((Double) cmd1args.get(1)), closeTo(2.5, 0.1));
        assertThat(((Integer) cmd1args.get(2)), equalTo(5));
    }

    @Test
    public void simpleCommandList() throws CommandParserException {
        addValidCommands("zero", "one", "two");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "zero | one | two").apply(0);
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

    @Test
    public void statementCanEndInSemicolon() throws CommandParserException {
        addValidCommands("foo");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("foo;").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();

        assertThat(pipeline.getInputVariable(), nullValue());
        assertThat(pipeline.getOutputVariable(), nullValue());

        assertThat(cmds.size(), equalTo(1));

        final Command command1 = cmds.apply(0);
        assertThat(command1.getName(), equalTo("foo"));
        assertNoArgs(command1);
    }

    @Test
    public void singleCommandInsideAScopeBlock() throws CommandParserException {
        addValidCommands("command");

        final Statement pipeline = parser.parse("{ command }").apply(0);
        assertThat(pipeline, instanceOf(BlockStatements.class));
    }

    private void assertNoArgs(final Command cmd) {
        assertThat(cmd.getArgs().size(), equalTo(0));
    }

    @Test
    public void validOperatorIdentifiers() throws CommandParserException {
        final String[] valids = new String[]{
                "*",
                "+",
                "++",
                ":::",
                "<?>",
                ":->",
                "!",
                "/",
                ":",
                "@",
                "_",
                "|" // but must be contained in (sub-commands)
        };
        addValidCommands(valids);
        for (String valid : valids) {
            final CommandPipeline pipeline = (CommandPipeline) parser.parse(valid).apply(0);
            assertThat(pipeline.getCommands().size(), equalTo(1));
            assertThat(pipeline.getCommands().apply(0).getName(), equalTo(valid));
        }
    }

    @Test
    public void invalidOperatorIdentifiers() {
        final String[] invalids = new String[]{
                "\"",
                "'",
                "(",
                ")",
                "[",
                "]",
                "{",
                "}",
                ".",
                ";",
                "`"
        };
        addValidCommands(invalids); // so they won't be rejected immediately, but parsed
        for (String invalid : invalids) {
            try {
                parser.parse(invalid);
                fail("Invalid operator identifier '" + invalid + "' was not parsed as invalid");
            } catch (CommandParserException cpe) {
                assertThat(cpe.getMessage(), containsString(invalid)); // bit of a weak test...
            }
        }
    }

    @Test
    public void subCommandsAreEmbeddedInArguments() throws CommandParserException {
        addValidCommands("*", "+", "/");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "(2 * 3) + (y / 7)").apply(0);

        checkSubCommandsAreEmbeddedInArguments(pipeline);
    }

    @Test
    public void subCommandsAreEmbeddedInArgumentsAllowingSuperfluousParenthesesAroundCommmand() throws CommandParserException {
        addValidCommands("*", "+", "/");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "(((2 * 3) + (y / 7)))").apply(0);

        checkSubCommandsAreEmbeddedInArguments(pipeline);
    }

    @Test
    public void subCommandsAreEmbeddedInArgumentsAllowingSuperfluousParenthesesAroundArguments() throws CommandParserException {
        addValidCommands("*", "+", "/");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "((2 * 3) + (((y / 7))))").apply(0);

        checkSubCommandsAreEmbeddedInArguments(pipeline);
    }

    private void checkSubCommandsAreEmbeddedInArguments(final CommandPipeline pipeline) {
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();

        assertThat(cmds.size(), equalTo(1));
        final Command plus = cmds.apply(0);
        assertThat(plus.getName(), equalTo("+"));

        final List<Object> args = plus.getArgs();
        assertThat(args.size(), equalTo(2));

        final Command times = (Command) args.get(0);
        assertThat(times.getName(), equalTo("*"));
        final List<Object> timesExpectedArgs = new ArrayList<Object>();
        timesExpectedArgs.addAll(asList(2, 3));
        assertThat(times.getArgs(), equalTo(timesExpectedArgs));

        final Command div = (Command) args.get(1);
        assertThat(div.getName(), equalTo("/"));
        final List<Object> divExpectedArgs = new ArrayList<Object>();
        divExpectedArgs.addAll(asList(new VariableReference("y"), 7));
        assertThat(div.getArgs(), equalTo(divExpectedArgs));
    }

    @Test
    public void subCommandsAreArbitrarilyNestedInArguments() throws CommandParserException {
        addValidCommands("*", "+", "/");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse(
                "(2 * (3 + (8 / var))) + 1").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();

        assertThat(cmds.size(), equalTo(1));
        final Command plusCommand = cmds.apply(0);
        assertThat(plusCommand.getName(), equalTo("+"));

        final List<Object> plusCommandArgs = plusCommand.getArgs();
        assertThat(plusCommandArgs.size(), equalTo(2));

        final Command plusLhs = (Command) plusCommandArgs.get(0);
        assertThat(plusLhs.getName(), equalTo("*"));
        final Integer two = (Integer) plusLhs.getArgs().get(0);
        assertThat(two, equalTo(2));
        final Command innerPlus = (Command) plusLhs.getArgs().get(1);
        assertThat(innerPlus.getName(), equalTo("+"));
        assertThat(innerPlus.getArgs().size(), equalTo(2));
        final Integer three = (Integer) innerPlus.getArgs().get(0);
        assertThat(three, equalTo(3));
        final Command div = (Command) innerPlus.getArgs().get(1);
        assertThat(div.getName(), equalTo("/"));
        assertThat(div.getArgs().size(), equalTo(2));
        final Integer eight = (Integer) div.getArgs().get(0);
        assertThat(eight, equalTo(8));
        final VariableReference varRef = (VariableReference) div.getArgs().get(1);
        assertThat(varRef.variableName(), equalTo("var"));

        final Integer plusRhs = (Integer) plusCommandArgs.get(1);
        assertThat(plusRhs, equalTo(1));
    }

    @Test
    public void subCommandsCanContainPipeSymbolsAsOperators() throws CommandParserException {
        addValidCommands("echo", "|", "||");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("echo (2 | (true || false))").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();

        assertThat(cmds.size(), equalTo(1));
        final Command command = cmds.apply(0);
        assertThat(command.getName(), equalTo("echo"));

        final List<Object> echoArgs = command.getArgs();
        assertThat(echoArgs.size(), equalTo(1));

        final Command bitwiseOrCommand = (Command) echoArgs.get(0);
        assertThat(bitwiseOrCommand.getName(), equalTo("|"));
        final List<Object> bitwiseOrCommandArgs = bitwiseOrCommand.getArgs();

        final Command logicalOrCommand = (Command) bitwiseOrCommandArgs.get(1);
        assertThat(logicalOrCommand.getName(), equalTo("||"));
        final List<Object> logicalOrExpectedArgs = new ArrayList<Object>();
        logicalOrExpectedArgs.addAll(asList(true, false));
        assertThat(logicalOrCommand.getArgs(), equalTo(logicalOrExpectedArgs));

        final List<Object> bitwiseOrExpectedArgs = new ArrayList<Object>();
        bitwiseOrExpectedArgs.addAll(asList(2, logicalOrCommand));
        assertThat(bitwiseOrCommandArgs, equalTo(bitwiseOrExpectedArgs));
    }

    @Test
    public void blocksCanBeArguments() throws CommandParserException {
        addValidCommands("echo", "foo", "bar");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("echo { foo 2; bar 3; }").apply(0);
        final scala.collection.immutable.List<Command> cmds = pipeline.getCommands();

        assertThat(cmds.size(), equalTo(1));
        final Command mainEchoCommand = cmds.apply(0);
        assertThat(mainEchoCommand.getName(), equalTo("echo"));

        final List<Object> mainEchoCommandArgs = mainEchoCommand.getArgs();
        assertThat(mainEchoCommandArgs.size(), equalTo(1));

        final BlockStatements mainEchoBlockArg = (BlockStatements) mainEchoCommandArgs.get(0);
        final scala.collection.immutable.List<Statement> mainEchoBlockArgStatements = mainEchoBlockArg.getStatements();
        assertThat(mainEchoBlockArgStatements.size(), equalTo(2));


        final CommandPipeline fooPipeline = (CommandPipeline) mainEchoBlockArgStatements.apply(0);
        final scala.collection.immutable.List<Command> fooPipelineCommands = fooPipeline.getCommands();
        assertThat(fooPipelineCommands.size(), equalTo(1));
        final Command fooCommand = fooPipelineCommands.apply(0);
        assertThat(fooCommand.getName(), equalTo("foo"));
        final List<Object> fooCommandArgs = new ArrayList<Object>();
        fooCommandArgs.addAll(asList(2));
        assertThat(fooCommand.getArgs(), equalTo(fooCommandArgs));

        final CommandPipeline barPipeline = (CommandPipeline) mainEchoBlockArgStatements.apply(1);
        final scala.collection.immutable.List<Command> barPipelineCommands = barPipeline.getCommands();
        assertThat(barPipelineCommands.size(), equalTo(1));
        final Command barCommand = barPipelineCommands.apply(0);
        assertThat(barCommand.getName(), equalTo("bar"));
        final List<Object> barCommandArgs = new ArrayList<Object>();
        barCommandArgs.addAll(asList(3));
        assertThat(barCommand.getArgs(), equalTo(barCommandArgs));
    }

    @Test
    public void oddIfStatement() throws CommandParserException {
        addValidCommands("if", "||", "+", "==");

        final CommandPipeline pipeline = (CommandPipeline) parser.parse("if ((x + y) == 6) { 69 } ").apply(0);

    }
}

