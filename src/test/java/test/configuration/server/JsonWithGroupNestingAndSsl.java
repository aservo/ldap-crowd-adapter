package test.configuration.server;

import test.api.IntegrationTestServerSetup;
import test.api.helper.ShellAccess;

import java.nio.file.Path;
import java.util.Properties;


public class JsonWithGroupNestingAndSsl
        implements IntegrationTestServerSetup {

    private final int port;

    public JsonWithGroupNestingAndSsl(int port) {

        this.port = port;
    }

    public int getPort() {

        return port;
    }

    public boolean isSslEnabled() {

        return true;
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
        properties.put("ssl.enabled", String.valueOf(isSslEnabled()));
        properties.put("ssl.key-store-file", getKeyStoreFile().toString());
        properties.put("ssl.key-store-password", getKeyStorePassword());
        properties.put("mode.flattening", String.valueOf(isFlatteningEnabled()));

        return properties;
    }

    public Properties getBackendProperties() {

        Properties properties = new Properties();

        properties.put("db-uri", "file:./src/test/resources/de/aservo/ldap/adapter/db.json");

        properties.put("database.jdbc.connection.url", "jdbc:h2:" + getTestDirectory() + "/db");

        return properties;
    }

    public void startup()
            throws Exception {

        ShellAccess.syncExec(
                "./ssl-test-setup/create-key-store.sh",
                getHost(),
                getKeyStorePassword(),
                getSelfSignedDataDir().toString()
        );

        System.setProperty("javax.net.ssl.trustStore", getKeyStoreFile().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", getKeyStorePassword());
    }

    private Path getSelfSignedDataDir() {

        return getTestDirectory().resolve("self-signed");
    }

    private Path getKeyStoreFile() {

        return getSelfSignedDataDir().resolve("local.keystore.jks");
    }

    private String getKeyStorePassword() {

        return "changeit";
    }
}
