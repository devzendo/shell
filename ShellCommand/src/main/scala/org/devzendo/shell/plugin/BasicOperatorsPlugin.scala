/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.shell.plugin

import org.devzendo.shell.pipe.{OutputPipe, InputPipe}
import org.devzendo.shell.interpreter.{CommandExecutionException, Variable}
import org.devzendo.shell.ast.VariableReference
import org.apache.log4j.Logger
import scala.annotation.tailrec
import scala.throws
import org.devzendo.shell.parser.CommandParserException

object BasicOperatorsPlugin {
    private val LOGGER = Logger.getLogger(classOf[BasicOperatorsPlugin])
}

import BasicOperatorsPlugin._

class BasicOperatorsPlugin extends AbstractShellPlugin with PluginHelper {
    def getName = "Operators"

    // Convert an arg to a list, expanding variable references.
    private def wrapArgAsList(arg: AnyRef): List[AnyRef] = arg match {
        case v: Variable =>
            v.asList()
        case vr: VariableReference =>
            executionEnvironment().variableRegistry().getVariable(vr).asList()
        case null => // unsure...
            List[AnyRef]()
        case x: AnyRef =>
            List(x)
    }

    // Convert all args to lists, expanding variable references.
    private def wrapAsList(args: List[AnyRef]): List[List[AnyRef]] = {
        LOGGER.debug("Wrapping args as lists: " + args)
        val out = args map wrapArgAsList
        LOGGER.debug("Wrapped args as lists: " + out)
        out
    }

    // Pad all lists out to the same length with some identity.
    private def padLists(args: List[List[AnyRef]], fill: AnyRef) = {
        val maxLen = (0 /: args) ((curLen: Int, list: List[AnyRef]) =>  Math.max(curLen, list.size))
        LOGGER.debug("Padding args with fill(" + fill + ") to maxlen(" + maxLen + ") args: " + args)
        val out = args.map( (argList: List[AnyRef]) => {
            argList.padTo(maxLen, fill)
        } )
        LOGGER.debug("Padded args: " + out)
        out
    }

    // Reduce two lists by applying an operation to elements of them, pairwise, producing one output list.
    // e.g. applying + to ((1,2), (3,4)) => (4,6)
    private def reducePair(first: List[AnyRef], second: List[AnyRef], op: ((AnyRef, AnyRef) => AnyRef)): List[AnyRef] = {
        (first zip second).map( (t: (AnyRef, AnyRef)) => { op(t._1, t._2) } )
    }

    // Repeatedly reduce a list with an operation until it is empty or a single list.
    @tailrec
    private def reduce(args: List[List[AnyRef]], op: ((AnyRef, AnyRef) => AnyRef)): List[AnyRef] = {
        args.size match {
            case 0 => List.empty
            case 1 => args.apply(0)
            case _ => {
                val first = args.apply(0)
                val second = args.apply(1)
                val rest = args.drop(2)
                val reduced = reducePair(first, second, op) :: rest
                reduce(reduced, op)
            }
        }
    }

    // Reduce expanded arguments to a single list transformed by an operation and identity, and pipe the results out.
    private def reduceArgsThenPipeOut(
             outputPipe: OutputPipe,
             args: List[AnyRef],
             identity: AnyRef,
             op: ((AnyRef, AnyRef) => AnyRef),
             validate: (List[AnyRef]) => Unit) {
        LOGGER.debug("Wrapping args: " + args)
        val argsAsLists = wrapAsList(args)
        LOGGER.debug("Validating wrapped args: " + argsAsLists)
        argsAsLists foreach validate
        LOGGER.debug("Padding validated wrapped args")
        val argsAsPaddedLists = padLists(argsAsLists, identity)
        LOGGER.debug("Reducing args: " + argsAsPaddedLists)
        val reduced = reduce(argsAsPaddedLists, op)
        LOGGER.debug("Reduced args: " + reduced)

        reduced.foreach( outputPipe.push(_) )
    }

    // Map expanded argument to a single list transformed by an operation, and pipe the results out.
    private def mapArgThenPipeOut(
             outputPipe: OutputPipe,
             arg: AnyRef,
             op: ((AnyRef) => AnyRef),
             validate: (List[AnyRef]) => Unit) {
        LOGGER.debug("Wrapping arg: " + arg)
        val argList = wrapArgAsList(arg)
        LOGGER.debug("Validating wrapped arg: " + argList)
        validate(argList)
        LOGGER.debug("Mapping arg")
        val mapped = argList map op
        LOGGER.debug("Mapped arg: " + mapped)
        mapped.foreach( outputPipe.push(_) )
    }

    val allArgumentTypes = Seq(
        classOf[String], classOf[java.lang.Integer], classOf[java.lang.Double], classOf[java.lang.Boolean],
        classOf[Variable], classOf[VariableReference]
    )

    val numericArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Double],
        classOf[Variable], classOf[VariableReference]
    )

    def curriedAllowArgumentTypes(verb: String, allowed: Seq[Class[_]])(args: List[AnyRef])
    {
        onlyAllowArgumentTypes(verb, args, allowed)
    }


    // plus --------------------------------------------------------------------

    private def plusElem(a: AnyRef, b: AnyRef): AnyRef = {
        (a, b) match {
            case (aStr: String, bStr: String) => aStr + bStr
            case (aStr: String, bInt: java.lang.Integer) => plusElem(aStr, bInt.toString)
            case (aStr: String, bDbl: java.lang.Double) => plusElem(aStr, new java.lang.Double(bDbl).toString)
            case (aStr: String, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add String '" + aStr + "' to Boolean '" + bBoo + "'")

            case (aInt: java.lang.Integer, bStr: String) => plusElem(aInt.toString, bStr)
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt + bInt)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => plusElem(new java.lang.Double(aInt.doubleValue()), bDbl)
            case (aInt: java.lang.Integer, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add Integer '" + aInt + "' to Boolean '" + bBoo + "'")
                // what about truthiness of integers? addition of booleans means disjunction

            case (aDbl: java.lang.Double, bStr: String) => plusElem(new java.lang.Double(aDbl).toString, bStr)
            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => plusElem(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl + bDbl)
            case (aDbl: java.lang.Double, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add Double '" + aDbl + "' to Boolean '" + bBoo + "'")
                // what about truthiness of doubles? addition of booleans means disjunction

            case (aBoo: java.lang.Boolean, bStr: String) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to String '" + bStr + "'")
            case (aBoo: java.lang.Boolean, bInt: java.lang.Integer) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to Integer '" + bInt + "'")
            case (aBoo: java.lang.Boolean, bDbl: java.lang.Double) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to Double '" + bDbl + "'")
            case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo || bBoo)
        }
    }

    /**
     * Addition is defined for Integers, Doubles, Strings and Booleans.
     * Differing numerics are converted to the type that loses less.
     * If Strings are involved, addition is concatenation.
     * Numerics concatenated to Strings are first converted to Strings.
     * Boolean addition is disjunction. Booleans can only be ored with Booleans.
     * @param inputPipe
     * @param outputPipe
     * @param args
     * @throws org.devzendo.shell.interpreter.CommandExecutionException
     */
    @CommandName(name = "+")
    @throws(classOf[CommandExecutionException])
    def plus(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[AnyRef]) {
        val validator = curriedAllowArgumentTypes("add", allArgumentTypes)(_)
        reduceArgsThenPipeOut(outputPipe, args, new Integer(0), plusElem, validator)
    }

    // minus -------------------------------------------------------------------

    private def minusElem(a: AnyRef, b: AnyRef): AnyRef = {
        (a, b) match {
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt - bInt)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => minusElem(new java.lang.Double(aInt.doubleValue()), bDbl)

            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => minusElem(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl - bDbl)
        }
    }

    /**
     * Subtraction is defined for Integers and Doubles.
     * Differing numerics are converted to the type that loses less.
     * Unary minus negates the inputs.
     * @param inputPipe
     * @param outputPipe
     * @param args
     * @throws org.devzendo.shell.interpreter.CommandExecutionException
     */
    @CommandName(name = "-")
    @throws(classOf[CommandExecutionException])
    def minus(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[AnyRef]) {
        val validator = curriedAllowArgumentTypes("subtract", numericArgumentTypes)(_)
        if (args.size == 1) {
            def negate(a: AnyRef): AnyRef = { minusElem(new java.lang.Integer(0), a) }
            mapArgThenPipeOut(outputPipe, args(0), negate, validator)
        } else {
            reduceArgsThenPipeOut(outputPipe, args, new Integer(0), minusElem, validator)
        }
    }


    @CommandName(name = "*")
    def times(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "/")
    def divide(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "!")
    @CommandAlias(alias = "¬")
    def logicalNot(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "%")
    def mod(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "^")
    def bitwiseXor(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "|") // hmmm, parser might not like that...
    def bitwiseOr(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "&")
    def bitwiseAnd(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "~")
    def bitwiseComplement(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "&&")
    def logicalAnd(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "||")
    def logicalOr(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "<") // hmmm parser?
    def lessThan(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = ">") // hmmm parser?
    def greaterThan(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "<=") // hmmm parser?
    @CommandAlias(alias = "≤")
    def lessThanOrEqual(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = ">=") // hmmm parser?
    @CommandAlias(alias = "≥")
    def greaterThanOrEqual(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "!=") // hmmm parser?
    @CommandAlias(alias = "<>")
//    @CommandAlias(alias = "≠")
    def notEqual(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = ">>") // hmmm parser?
    def shiftRight(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = ">>>") // hmmm parser?
    def shiftRightUnsigned(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "<<") // hmmm parser?
    def shiftLeft(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }


}

