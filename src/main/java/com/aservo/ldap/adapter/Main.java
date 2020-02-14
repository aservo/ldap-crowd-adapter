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

import com.aservo.ldap.adapter.util.ServerConfiguration;
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
        CommonLdapServer server = createServerInstance();

        logger.debug("Application is running with max memory: {}", memoryBean.getHeapMemoryUsage().getMax());
        logger.debug("Application is running with initial memory: {}", memoryBean.getHeapMemoryUsage().getInit());

        // boot process
        server.startup();
        logger.info("Starting directory listener...");
    }

    /**
     * Creates a server instance.
     * It configures all system components including Crowd client api.
     *
     * @return the server instance
     */
    public static CommonLdapServer createServerInstance() {

        // hard coded directory for initial configuration
        File configDir = new File("./etc");

        // configure logging
        Properties loggingProperties = loadConfigFile(new File(configDir, "log4j.properties"));
        setLogLevel(loggingProperties);
        PropertyConfigurator.configure(loggingProperties);

        // load server configuration
        Properties serverProperties = loadConfigFile(new File(configDir, "server.properties"));
        serverProperties.putAll(System.getProperties());

        // load backend configuration
        Properties backendProperties = loadConfigFile(new File(configDir, "backend.properties"));
        backendProperties.putAll(System.getProperties());

        // parse configuration
        ServerConfiguration serverConfig = new ServerConfiguration(serverProperties, backendProperties);

        // return with server instance
        return new CommonLdapServer(serverConfig);
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

        try {

            properties.load(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        return properties;
    }
}
