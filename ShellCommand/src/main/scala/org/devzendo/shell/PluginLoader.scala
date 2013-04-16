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

package org.devzendo.shell

import org.apache.log4j.Logger
import collection.JavaConverters._
import org.devzendo.shell.plugin.{ShellPlugin, ShellPluginException}
import java.util
import java.io.{BufferedInputStream, InputStream, IOException}
import java.net.URL
import java.util.Properties
import scala.throws

object PluginLoader {
    private val LOGGER = Logger.getLogger(classOf[PluginLoader])
}
class PluginLoader {

    @throws[ShellPluginException]
    def loadPluginsFromClasspath(propertiesResourcePath: String): java.util.List[ShellPlugin] = {
        PluginLoader.LOGGER.debug("Loading plugins from properties at " + propertiesResourcePath)
        try {
            val propertiesURLs = getPluginDescriptorURLs(propertiesResourcePath)
            val plugins = new util.ArrayList[ShellPlugin]()
            while (propertiesURLs.hasMoreElements) {
                val propertiesURL = propertiesURLs.nextElement()
                val properties = loadProperties(propertiesURL)
                plugins.addAll(loadPlugins(properties))
            }
            PluginLoader.LOGGER.debug("Returning " + plugins.size() + " plugin(s)")
            plugins
        } catch {
            case e: IOException =>
                val warning = "Failure loading plugins: " + e.getMessage
                PluginLoader.LOGGER.warn(warning)
                PluginLoader.LOGGER.debug(warning, e)
                throw new ShellPluginException(warning)
        }
    }

    /**
     * Given a resource path, return all URLs pointing to this on the current
     * classpath
     *
     * @param resourcePath
     *        the resource path
     * @return an Enumeration<URL> of instances
     * @throws IOException
     *         on classpath scanning failure
     */
    @throws[IOException]
    private def getPluginDescriptorURLs(resourcePath: String): java.util.Enumeration[URL] = {
        Thread.currentThread().getContextClassLoader.getResources(resourcePath)
    }

    // TODO plenty to convert to Scala collections in here!
    @throws[IOException]
    private def loadPlugins(properties: Properties): java.util.List[ShellPlugin] = {
        val plugins = new util.ArrayList[ShellPlugin]()
        val entrySet: java.util.Set[java.util.Map.Entry[AnyRef, AnyRef]] = properties.entrySet()
        entrySet.asScala.foreach { (entry: java.util.Map.Entry[AnyRef, AnyRef]) =>
            // we can ignore the lhs
            val pluginClassName = entry.getValue.toString
            plugins.add(loadPlugin(pluginClassName))
        }
        plugins
    }

    @throws[IOException]
    private def loadPlugin(pluginClassName: String): ShellPlugin = {
        if (pluginClassName == null || pluginClassName.trim().length() == 0) {
            throw new IOException("Cannot load a Plugin from null or empty class name")
        }
        // or groovy, etc., - whatever's supported by Spring, ideally
        instantiateJavaPlugin(pluginClassName)
    }

    @throws[IOException]
    private def instantiateJavaPlugin(pluginClassName: String): ShellPlugin = {
        PluginLoader.LOGGER.debug("Loading plugin from class " + pluginClassName)
        try {
            Class.forName(pluginClassName).asInstanceOf[Class[ShellPlugin]].newInstance()
        } catch {
            case e: Exception =>
                val warning = "Cannot load class '" + pluginClassName + "': " + e.getClass.getSimpleName + ": " + e.getMessage
                PluginLoader.LOGGER.warn(warning)
                throw new IOException(warning)
        }
    }

    @throws[IOException]
    private def loadProperties(propertiesURL: URL): Properties = {
        PluginLoader.LOGGER.debug("Loading properties file at " + propertiesURL.toString)
        var is: InputStream = null
        try {
            val stream = propertiesURL.openStream()
            is = new BufferedInputStream(stream)
            val properties = new Properties()
            properties.load(is)
            properties
        } catch {
            case e: IOException =>
                val warning = "Cannot load plugin descriptor at URL" + propertiesURL.toString + ": " + e.getMessage
                PluginLoader.LOGGER.warn(warning)
                PluginLoader.LOGGER.debug(warning, e)
                throw new IOException(warning)
        } finally {
            if (is != null) {
                try {
                    is.close()
                } catch {
                    case e: IOException =>
                        // do nothing
                }
            }
        }
    }
}
