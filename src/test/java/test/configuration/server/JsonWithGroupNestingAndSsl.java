package test.configuration.server;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Properties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import test.api.CertificateGenerator;
import test.api.IntegrationTestServerSetup;


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
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend," +
                        "com.aservo.ldap.adapter.backend.CachedWithPersistenceDirectoryBackend");

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

        properties.put("db-uri", "file:./src/test/resources/com/aservo/ldap/adapter/db.json");

        properties.put("database.jdbc.connection.url", "jdbc:h2:" + getTestDirectory() + "/db");

        return properties;
    }

    public void startup()
            throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        KeyPair keyPair = CertificateGenerator.generateKeyPair();
        X509Certificate cert = CertificateGenerator.generateX509Certificate(keyPair, "CN=" + getHost(), 10);

        CertificateGenerator.updateKeyStore(keyPair, cert, "self-signed", getKeyStoreFile().toFile(),
                getKeyStorePassword());

        System.setProperty("javax.net.ssl.trustStore", getKeyStoreFile().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", getKeyStorePassword());
    }

    private Path getKeyStoreFile() {

        return getTestDirectory().resolve("local.keystore");
    }

    private String getKeyStorePassword() {

        return "changeit";
    }
}
