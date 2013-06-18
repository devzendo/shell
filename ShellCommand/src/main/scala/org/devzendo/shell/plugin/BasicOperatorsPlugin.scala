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

    val integerArgumentTypes = Seq(
        classOf[java.lang.Integer],
        classOf[Variable], classOf[VariableReference]
    )

    val integerBooleanArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Boolean],
        classOf[Variable], classOf[VariableReference]
    )

    val numericArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Double],
        classOf[Variable], classOf[VariableReference]
    )

    val booleanArgumentTypes = Seq(
        classOf[java.lang.Boolean], classOf[Variable], classOf[VariableReference]
    )

    val numericAndStringArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Double],
        classOf[Variable], classOf[VariableReference], classOf[String]
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

    /*
     * Addition is defined for Integers, Doubles, Strings and Booleans.
     * Differing numerics are converted to the type that loses less.
     * If Strings are involved, addition is concatenation.
     * Numerics concatenated to Strings are first converted to Strings.
     * Boolean addition is disjunction. Booleans can only be ored with Booleans.
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

    /*
     * Subtraction is defined for Integers and Doubles.
     * Differing numerics are converted to the type that loses less.
     * Unary minus negates the inputs.
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


    // times -------------------------------------------------------------------
    private def replicate(n: Integer, str: String): String = {
        if (n < 0) {
            throw new CommandExecutionException("Cannot replicate the String '" + str + "' by the negative Integer '" + n + "'")
        } else {
            str * n
        }
    }

    private def timesElem(a: AnyRef, b: AnyRef): AnyRef = {
        (a, b) match {
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt * bInt)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => timesElem(new java.lang.Double(aInt.doubleValue()), bDbl)
            case (aInt: java.lang.Integer, bStr: java.lang.String) => replicate(aInt, bStr)
            case (aStr: String, bInt: java.lang.Integer) => replicate(bInt, aStr)

            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => timesElem(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl * bDbl)
            case (aDbl: java.lang.Double, bStr: java.lang.String) => throw new CommandExecutionException("Cannot replicate the String '" + bStr + "' by the Double '" + aDbl + "'")
            case (aStr: String, bDbl: java.lang.Double) => throw new CommandExecutionException("Cannot replicate the String '" + aStr + "' by the Double '" + bDbl + "'")
        }
    }

    /*
     * Multiplication is defined for Integers, Doubles and Strings.
     * Differing numerics are converted to the type that loses less.
     * Integer and String combinations give string replication.
     */
    @CommandName(name = "*")
    @throws(classOf[CommandExecutionException])
    def times(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("multiply", numericAndStringArgumentTypes)(_)
        reduceArgsThenPipeOut(outputPipe, args, new Integer(1), timesElem, validator)
    }


    // divide ------------------------------------------------------------------
    private def divideElem(a: AnyRef, b: AnyRef): AnyRef = {
        (a, b) match {
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt / bInt)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => divideElem(new java.lang.Double(aInt.doubleValue()), bDbl)

            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => divideElem(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl / bDbl)
        }
    }

    /*
     * Division is defined for Integers and Doubles.
     * Differing numerics are converted to the type that loses less.
     */
    @CommandName(name = "/")
    @throws(classOf[CommandExecutionException])
    def divide(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("divide", numericArgumentTypes)(_)
        reduceArgsThenPipeOut(outputPipe, args, new Integer(1), divideElem, validator)
    }

    // logical not -------------------------------------------------------------

    /*
     * Negation is only defined for Booleans, and is a unary operation.
     */
    @CommandName(name = "!")
    @CommandAlias(alias = "¬")
    @throws(classOf[CommandExecutionException])
    def logicalNot(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("negate", booleanArgumentTypes)(_)
        if (args.size == 1) {
            def negate(a: AnyRef): AnyRef = a match {
                case b: java.lang.Boolean => new java.lang.Boolean(!b)
            }
            mapArgThenPipeOut(outputPipe, args(0), negate, validator)
        } else {
            throw new CommandExecutionException("Boolean negation is a unary operation")
        }
    }

    // modulus -----------------------------------------------------------------

    /*
     * Modulus is defined for Integers
     */
    @CommandName(name = "%")
    @throws(classOf[CommandExecutionException])
    def mod(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("take the modulus of", integerArgumentTypes)(_)
        def modElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt % bInt)
            }
        }
        reduceArgsThenPipeOut(outputPipe, args, new Integer(1), modElem, validator)
    }

    // bitwise exclusive or ----------------------------------------------------
    private def boolean2Integer(b: java.lang.Boolean): Integer = {
        if (b) new Integer(1) else new Integer(0)
    }

    /*
     * Xor is defined for Integers and Booleans.
     */
    @CommandName(name = "^")
    @throws(classOf[CommandExecutionException])
    def bitwiseXor(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("bitwise xor", integerBooleanArgumentTypes)(_)
        def xorElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bBoo: java.lang.Boolean) => xorElem(aInt, boolean2Integer(bBoo))
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo ^ bBoo)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt ^ bInt)
                case (aBoo: java.lang.Boolean, bInt: java.lang.Integer) => xorElem(boolean2Integer(aBoo), bInt)
            }
        }
        reduceArgsThenPipeOut(outputPipe, args, new Integer(0), xorElem, validator)
    }

    // bitwise or --------------------------------------------------------------
    /*
     * Or is defined for Integers and Booleans.
     */
    @CommandName(name = "|") // hmmm, parser might not like that...
    @throws(classOf[CommandExecutionException])
    def bitwiseOr(inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("bitwise or", integerBooleanArgumentTypes)(_)
        def orElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bBoo: java.lang.Boolean) => orElem(aInt, boolean2Integer(bBoo))
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo | bBoo)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt | bInt)
                case (aBoo: java.lang.Boolean, bInt: java.lang.Integer) => orElem(boolean2Integer(aBoo), bInt)
            }
        }
        reduceArgsThenPipeOut(outputPipe, args, new Integer(0), orElem, validator)
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

