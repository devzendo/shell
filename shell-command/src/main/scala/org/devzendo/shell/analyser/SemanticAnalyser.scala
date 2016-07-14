package org.devzendo.shell.analyser

import org.devzendo.shell.ast._
import org.devzendo.shell.parser.{CommandParserException, ExistenceChecker}

/**
  * Analyses a lexically-valid list of Statements, and performs additional validation.
  */
class SemanticAnalyser(commandExists: ExistenceChecker) {

    @throws(classOf[CommandParserException])
    def analyse(inputLine: String, statements: List[Statement]): Unit = {
        statements.foreach( s => analyse(inputLine, s))
        // it's all good!
    }

    private def analyse(inputLine: String, statement: Statement): Unit = {
        statement match {
            case bs: BlockStatements => {
                analyse(inputLine, bs.getStatements)
            }
            case cp: CommandPipeline => {
                variableMustNotHaveACommandName(inputLine, cp.getInputVariable)
                variableMustNotHaveACommandName(inputLine, cp.getOutputVariable)
                // TODO change args from a java list to a Scala one
                cp.getCommands.foreach( cmd => {
                    val args = cmd.args
                    for (i <- 0 until args.size()) {
                        val arg: AnyRef = args.get(i)
                        arg match {
                            case vr: VariableReference =>
                                variableMustNotHaveACommandName(inputLine, vr)
                            case _ => // only handle VariableReferences for now
                        }
                    }
                } )
            }
        }
    }

    def variableMustNotHaveACommandName(inputLine: String, v: VariableReference): Unit = {
        if (v != null && commandExists.exists(v.variableName)) {
            throw new CommandParserException("Variable '" + v.variableName + "' cannot have the same name as a command")
        }
    }


}
