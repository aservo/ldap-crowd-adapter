package test.configuration.server;

import java.util.Properties;
import test.api.IntegrationTestServerSetup;


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
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend");

        properties.put("directory-backend.session", "");

        properties.put("ds-cache-directory", getTestDirectory().resolve("cache").toString());
        properties.put("bind.address", getHost() + ":" + getPort());
        properties.put("mode.flattening", String.valueOf(isFlatteningEnabled()));

        return properties;
    }

    public Properties getBackendProperties() {

        Properties properties = new Properties();

        properties.put("db-uri", "file:./src/test/resources/com/aservo/ldap/adapter/db.json");

        return properties;
    }
}
