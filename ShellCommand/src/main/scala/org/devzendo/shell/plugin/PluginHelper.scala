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
 
package org.devzendo.shell.plugin

import org.devzendo.shell.interpreter.CommandExecutionException

trait PluginHelper {
    def streamForeach(producer: => Option[Object], processor: (Object) => Unit) {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.foreach(processor)
    }

    def streamMap(producer: => Option[Object], processor: (Object) => Object): Stream[Object] = {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.map(processor)
    }

    def filterString(objects: Seq[Object]):Seq[String] = objects.filter(_.isInstanceOf[String]).asInstanceOf[Seq[String]] 

    def filterInt(objects: Seq[Object]):Seq[Integer] = objects.filter(_.isInstanceOf[Integer]).asInstanceOf[Seq[Integer]] 

    def filterBoolean(objects: Seq[Object]):Seq[Boolean] = objects.filter(_.isInstanceOf[Boolean]).asInstanceOf[Seq[Boolean]]

    @throws(classOf[CommandExecutionException])
    def onlyAllowArgumentTypes(commandNameAsVerb: String, args: List[AnyRef], allowedClasses: Seq[Class[_]]) {
        val argsAndTheirClasses = args.map( (arg: AnyRef) => {
            val argClass = arg match {
                case null => classOf[Null].asInstanceOf[Class[_]]
                case x: AnyRef => x.getClass.asInstanceOf[Class[_]]
            }
            (arg, argClass)
        })
        val allowedClassesSet = allowedClasses.toSet
        val disallowedArgsAndTheirClasses = argsAndTheirClasses.filterNot( (aatc: (AnyRef, Class[_])) => {
            allowedClassesSet.contains(aatc._2)
        })
        if (disallowedArgsAndTheirClasses.size > 0)
        {
            // (Switch("foo"), classOf[Switch]) => "Switch 'foo'"
            val argDescriptions = disallowedArgsAndTheirClasses.map((aatc: (AnyRef, Class[_])) =>
                aatc._2.getSimpleName + " '" + aatc._1 + "'"
            )
            throw new CommandExecutionException("Cannot " + commandNameAsVerb + " the " + argDescriptions.mkString(", "))
        }
    }
}