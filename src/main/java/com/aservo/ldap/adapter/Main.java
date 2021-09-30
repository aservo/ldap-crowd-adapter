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

package com.aservo.ldap.adapter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Logger logger = LoggerFactory.getLogger("Bootloader");
        ServerConfiguration config = createConfiguration();
        CommonLdapServer server = createServerInstance(config);

        logger.debug("Application is running with max memory: {}", memoryBean.getHeapMemoryUsage().getMax());
        logger.debug("Application is running with initial memory: {}", memoryBean.getHeapMemoryUsage().getInit());

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

        // hard coded directory for initial configuration
        File configDir = new File("./etc");

        // configure logging
        Properties loggingProperties = loadConfigFile(new File(configDir, "log4j.properties"));
        setLogLevel(loggingProperties);
        PropertyConfigurator.configure(loggingProperties);

        // load server configuration
        Properties finalServerProperties = loadConfigFile(new File(configDir, "server.properties"));
        finalServerProperties.putAll(System.getProperties());
        finalServerProperties.putAll(serverProperties);

        // load backend configuration
        Properties finalBackendProperties = loadConfigFile(new File(configDir, "backend.properties"));
        finalBackendProperties.putAll(System.getProperties());
        finalBackendProperties.putAll(backendProperties);

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

    private static void setLogLevel(Properties loggingProperties) {

        String rootCategoryKey = "log4j.rootCategory";
        String rootCategoryValue = loggingProperties.getProperty(rootCategoryKey);
        String sysLogLevel = System.getProperty("loglevel");

        if (sysLogLevel != null) {

            if (rootCategoryValue == null)
                throw new IllegalArgumentException("Cannot find key for " + rootCategoryKey);

            int pos = rootCategoryValue.indexOf(',');

            if (pos < 0)
                throw new IllegalArgumentException("Cannot parse value of key " + rootCategoryKey);

            String logResources = rootCategoryValue.substring(pos);

            loggingProperties.put(rootCategoryKey, sysLogLevel.toUpperCase() + logResources);
        }
    }

    private static Properties loadConfigFile(File file) {

        Properties properties = new Properties();

        try (final FileInputStream fis = new FileInputStream(file);
             final InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            properties.load(isr);

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        return properties;
    }
}
