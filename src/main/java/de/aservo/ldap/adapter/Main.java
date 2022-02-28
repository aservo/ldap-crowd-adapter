/*
 * Copyright (c) 2019 ASERVO Software GmbH
 * contact@aservo.com
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

package de.aservo.ldap.adapter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;


public class Main {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger("Bootloader");
        ServerConfiguration config = createConfiguration();
        CommonLdapServer server = createServerInstance(config);

        // boot process
        server.startup();
        logger.info("Starting directory listener...");
    }

    /**
     * Creates a server configuration instance.
     * It configures all directory backends.
     *
     * @return the server configuration instance
     */
    public static ServerConfiguration createConfiguration() {

        return createConfiguration(new Properties());
    }

    /**
     * Creates a server configuration instance.
     * It configures all directory backends.
     *
     * @param properties additional backend properties
     * @return the server configuration instance
     */
    public static ServerConfiguration createConfiguration(Properties properties) {

        return createConfiguration(properties, properties);
    }

    /**
     * Creates a server configuration instance.
     * It configures all directory backends.
     *
     * @param serverProperties  additional server properties
     * @param backendProperties additional backend properties
     * @return the server configuration instance
     */
    public static ServerConfiguration createConfiguration(Properties serverProperties, Properties backendProperties) {

        // hard coded paths for configuration files
        String serverConfigFile = "etc/server.properties";
        String backendConfigFile = "etc/backend.properties";
        String log4j2ConfigFile = "etc/log4j2.xml";

        // load server configuration
        Properties finalServerProperties = loadConfigFile(serverConfigFile);
        finalServerProperties.putAll(System.getProperties());
        finalServerProperties.putAll(serverProperties);

        // load backend configuration
        Properties finalBackendProperties = loadConfigFile(backendConfigFile);
        finalBackendProperties.putAll(System.getProperties());
        finalBackendProperties.putAll(backendProperties);

        // configure logging
        configLogging(log4j2ConfigFile, finalServerProperties);

        return new ServerConfiguration(finalServerProperties, finalBackendProperties);
    }

    /**
     * Creates a server instance.
     *
     * @return the server instance
     */
    public static CommonLdapServer createServerInstance(ServerConfiguration config) {

        return new CommonLdapServer(config);
    }

    private static void configLogging(String configFile, Properties properties) {

        String logLevel = properties.getProperty("log.level");
        URI configLocation;

        if (Files.exists(Paths.get(configFile)))
            configLocation = (new File(configFile)).toURI();
        else
            configLocation = URI.create(configFile);

        {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);

            context.setConfigLocation(configLocation);
            context.updateLoggers();
        }

        {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();

            if (logLevel != null)
                config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.getLevel(logLevel.toUpperCase()));

            context.updateLoggers();
        }
    }

    private static Properties loadConfigFile(String configFile) {

        Properties properties = new Properties();

        try {

            if (Files.exists(Paths.get(configFile))) {

                try (final FileInputStream fis = new FileInputStream(configFile);
                     final InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

                    properties.load(isr);
                }

            } else {

                try (final InputStream is = Main.class.getClassLoader().getResourceAsStream(configFile)) {

                    if (is == null)
                        throw new IllegalArgumentException("Cannot load resource " + configFile);

                    try (final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                        properties.load(isr);
                    }
                }
            }

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        return properties;
    }
}
