/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.devzendo.shell;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.devzendo.shell.plugin.ShellPlugin;
import org.devzendo.shell.plugin.ShellPluginException;

public class PluginLoader {
    private static final Logger LOGGER = Logger.getLogger(PluginLoader.class);

    public List<ShellPlugin> loadPluginsFromClasspath(
            final String propertiesResourcePath) throws ShellPluginException {
        LOGGER.debug("Loading plugins from properties at "
                + propertiesResourcePath);
        try {
            final Enumeration<URL> propertiesURLs = getPluginDescriptorURLs(propertiesResourcePath);
            final List<ShellPlugin> plugins = new ArrayList<ShellPlugin>();
            while (propertiesURLs.hasMoreElements()) {
                final URL propertiesURL = propertiesURLs.nextElement();
                final Properties properties = loadProperties(propertiesURL);
                plugins.addAll(loadPlugins(properties));
            }
            LOGGER.debug("Returning " + plugins.size() + " plugin(s)");
            return plugins;
        } catch (final IOException e) {
            final String warning = "Failure loading plugins: " + e.getMessage();
            LOGGER.warn(warning);
            LOGGER.debug(warning, e);
            throw new ShellPluginException(warning);
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
    private Enumeration<URL> getPluginDescriptorURLs(final String resourcePath)
            throws IOException {
        return Thread.currentThread().getContextClassLoader()
                .getResources(resourcePath);
    }

    private List<ShellPlugin> loadPlugins(final Properties properties) throws IOException {
        final List<ShellPlugin> plugins = new ArrayList<ShellPlugin>();
        final Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (final Entry<Object, Object> entry : entrySet) {
            // we can ignore the lhs
            final String pluginClassName = entry.getValue().toString();
            plugins.add(loadPlugin(pluginClassName));
        }
        return plugins;
    }

    private ShellPlugin loadPlugin(final String pluginClassName) throws IOException {
        if (pluginClassName == null || pluginClassName.trim().length() == 0) {
            throw new IOException("Cannot load a Plugin from null or empty class name");
        }
        // or groovy, etc., - whatever's supported by Spring, ideally
        return instantiateJavaPlugin(pluginClassName);
    }

    @SuppressWarnings("unchecked")
    private ShellPlugin instantiateJavaPlugin(final String pluginClassName)
            throws IOException {
        LOGGER.debug("Loading plugin from class " + pluginClassName);
        try {
            final Class<ShellPlugin> klass = (Class<ShellPlugin>) Class.forName(pluginClassName);
            return klass.newInstance();
        } catch (final Exception e) {
            final String warning = "Cannot load class '" + pluginClassName + "': " + e.getClass().getSimpleName() + ": " + e.getMessage();
            LOGGER.warn(warning);
            throw new IOException(warning);
        }
    }

    private Properties loadProperties(final URL propertiesURL) throws IOException {
        LOGGER.debug("Loading properties file at " + propertiesURL.toString());
        InputStream is = null;
        try {
            final InputStream stream = propertiesURL.openStream();
            is = new BufferedInputStream(stream);
            final Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (final IOException e) {
            final String warning = "Cannot load plugin descriptor at URL"
                + propertiesURL.toString()
                + ": "
                + e.getMessage();
            LOGGER.warn(warning);
            LOGGER.debug(warning, e);
            throw new IOException(warning);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // do nothing
                }
            }
        }
    }
}
