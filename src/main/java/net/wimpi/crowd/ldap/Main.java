package net.wimpi.crowd.ldap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import net.wimpi.crowd.ldap.util.ServerConfiguration;
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
