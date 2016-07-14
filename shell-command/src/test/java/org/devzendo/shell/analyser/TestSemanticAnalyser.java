package org.devzendo.shell.analyser;

import org.devzendo.shell.ScalaListHelper;
import org.devzendo.shell.ast.*;
import org.devzendo.shell.interpreter.CommandHandler;
import org.devzendo.shell.parser.CommandParserException;
import org.devzendo.shell.parser.ExistenceChecker;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static java.util.Arrays.asList;

public class TestSemanticAnalyser {
    private Set<String> validCommands = new HashSet<String>();
    private void addValidCommands(String ... commands) {
        validCommands.addAll(asList(commands));
    }

    final ExistenceChecker commandExistenceChecker = new ExistenceChecker() {

        @Override
        public boolean exists(String name) {
            return validCommands.contains(name);
        }
    };

    final SemanticAnalyser analyser = new SemanticAnalyser(commandExistenceChecker);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void commandPipelineInputVariableWithSameNameAsCommand() throws CommandParserException {
        setupFooCommandForVariableWithThatName();

        CommandPipeline cp = new CommandPipeline();
        cp.setInputVariable(new VariableReference("foo"));

        analyseStatements(cp);
    }

    @Test
    public void commandPipelineOutputVariableWithSameNameAsCommand() throws CommandParserException {
        setupFooCommandForVariableWithThatName();

        CommandPipeline cp = new CommandPipeline();
        cp.setOutputVariable(new VariableReference("foo"));

        analyseStatements(cp);
    }

    @Test
    public void commandPipelineVariableReferenceWithSameNameAsCommand() throws CommandParserException {
        setupFooCommandForVariableWithThatName();

        CommandPipeline cp = new CommandPipeline();
        Command cmd = new Command("cmd", asList((Object) new VariableReference("bar"), 5,
                new VariableReference("foo"), "blah"));
        cp.addCommand(cmd);

        analyseStatements(cp);
    }

    private void setupFooCommandForVariableWithThatName() {
        addValidCommands("foo");
        exception.expect(CommandParserException.class);
        exception.expectMessage("Variable 'foo' cannot have the same name as a command");
    }

    private void analyseStatements(Statement ... cp) throws CommandParserException {
        final scala.collection.immutable.List<Statement> statements = ScalaListHelper.createList(cp);
        analyser.analyse("irrelevant", statements);
    }
}
