package test.configuration.server;

import test.api.IntegrationTestServerSetup;

import java.util.Properties;


public class JsonWithGroupFlattening
        implements IntegrationTestServerSetup {

    private final int port;

    public JsonWithGroupFlattening(int port) {

        this.port = port;
    }

    public int getPort() {

        return port;
    }

    public boolean isSslEnabled() {

        return false;
    }

    public boolean isFlatteningEnabled() {

        return true;
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

        return properties;
    }

    public Properties getBackendProperties() {

        Properties properties = new Properties();

        properties.put("db-uri", "file:./src/test/resources/de/aservo/ldap/adapter/db.json");

        properties.put("database.jdbc.connection.url", "jdbc:h2:" + getTestDirectory() + "/db");

        return properties;
    }
}
