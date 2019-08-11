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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

    public static void main(String[] args) {

        // hard coded directory for initial configuration
        File configDir = new File("./etc");

        // configure logging
        Properties loggingProperties = loadConfigFile(new File(configDir, "log4j.properties"));
        PropertyConfigurator.configure(loggingProperties);

        // load server configuration
        Properties serverProperties = loadConfigFile(new File(configDir, "crowd-ldap-server.properties"));
        serverProperties.putAll(System.getProperties());

        // load crowd configuration
        Properties crowdProperties = loadConfigFile(new File(configDir, "crowd.properties"));
        crowdProperties.putAll(System.getProperties());

        // create object network
        Logger logger = LoggerFactory.getLogger("Bootloader");
        ServerConfiguration serverConfig = new ServerConfiguration(serverProperties, crowdProperties);
        CrowdLDAPServer server = new CrowdLDAPServer(serverConfig);

        // boot process
        server.start();
        logger.info("Starting directory listener...");
    }

    private static Properties loadConfigFile(File file) {

        Properties properties = new Properties();

        try {

            properties.load(new FileReader(file));

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        return properties;
    }
}
