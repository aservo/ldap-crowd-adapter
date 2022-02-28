package test.configuration.server;

import test.api.IntegrationTestServerSetup;

import java.util.Properties;


public class JsonWithGroupNestingAndAbbrevAttr
        implements IntegrationTestServerSetup {

    private final int port;

    public JsonWithGroupNestingAndAbbrevAttr(int port) {

        this.port = port;
    }

    public int getPort() {

        return port;
    }

    public boolean isSslEnabled() {

        return false;
    }

    public boolean isFlatteningEnabled() {

        return false;
    }

    public Properties getServerProperties() {

        Properties properties = new Properties();

        properties.put("directory-backend.permanent",
                "de.aservo.ldap.adapter.backend.JsonDirectoryBackend," +
                        "de.aservo.ldap.adapter.backend.CachedWithPersistenceDirectoryBackend");

        properties.put("directory-backend.session", "");

        properties.put("ds-cache-directory", getTestDirectory().resolve("cache").toString());
        properties.put("bind.address", getHost() + ":" + getPort());
        properties.put("mode.flattening", String.valueOf(isFlatteningEnabled()));

        properties.put("attribute.sn.abbreviate", "true");
        properties.put("attribute.gn.abbreviate", "true");

        return properties;
    }

    public Properties getBackendProperties() {

        Properties properties = new Properties();

        properties.put("db-uri", "file:./src/test/resources/de/aservo/ldap/adapter/db.json");

        properties.put("database.jdbc.connection.url", "jdbc:h2:" + getTestDirectory() + "/db");

        return properties;
    }
}
