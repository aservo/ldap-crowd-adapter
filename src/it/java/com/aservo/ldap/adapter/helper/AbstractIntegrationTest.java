package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.CommonLdapServer;
import com.aservo.ldap.adapter.Main;
import com.aservo.ldap.adapter.backend.DirectoryBackend;
import com.aservo.ldap.adapter.backend.DirectoryBackendFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.*;


public abstract class AbstractIntegrationTest {

    private static final Object lock = new Object();
    private static final Map<Integer, CommonLdapServer> servers = new HashMap<>();
    private static boolean initialised = false;
    private static int counter = 0;

    private static final String host = "localhost";
    public static final int MODE_NESTED_GROUPS_PORT = 10392;
    public static final int MODE_FLATTENING_PORT = 10393;

    private static final Path keyStoreFile = Paths.get("./tmp/local.keystore");
    private static final String keyStorePassword = "changeit";

    protected DirectoryBackend directoryBackend;

    @BeforeAll
    public static void setup()
            throws Exception {

        synchronized (lock) {

            counter++;

            if (!initialised) {

                Path testPath = Paths.get("./tmp");

                if (Files.exists(testPath))
                    FileUtils.deleteDirectory(testPath.toFile());

                Files.createDirectories(testPath);

                System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
                System.setProperty("javax.net.ssl.trustStorePassword", keyStorePassword);

                Security.addProvider(new BouncyCastleProvider());

                KeyPair keyPair = CertificateGenerator.generateKeyPair();
                X509Certificate cert = CertificateGenerator.generateX509Certificate(keyPair, "CN=" + host, 10);

                CertificateGenerator.updateKeyStore(keyPair, cert, "self-signed", keyStoreFile.toFile(),
                        keyStorePassword);

                boot(MODE_NESTED_GROUPS_PORT, false);
                boot(MODE_FLATTENING_PORT, true);

                initialised = true;
            }
        }
    }

    @AfterAll
    public static void shutdown()
            throws Exception {

        synchronized (lock) {

            counter--;

            if (initialised && counter == 0) {

                servers.get(MODE_NESTED_GROUPS_PORT).shutdown();
                servers.get(MODE_FLATTENING_PORT).shutdown();
                servers.clear();
            }
        }
    }

    private static void boot(int port, boolean flattening)
            throws Exception {

        System.setProperty("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.CrowdDirectoryBackend," +
                        "com.aservo.ldap.adapter.backend.CachedInMemoryDirectoryBackend");
        System.setProperty("ds-cache-directory", "./tmp/" + port + "/cache");
        System.setProperty("bind.address", host + ":" + port);
        System.setProperty("ssl.enabled", "true");
        System.setProperty("ssl.key-store-file", keyStoreFile.toString());
        System.setProperty("ssl.key-store-password", keyStorePassword);
        System.setProperty("mode.flattening", Boolean.toString(flattening));

        CommonLdapServer server = Main.createServerInstance(Main.createConfiguration());

        // boot process
        server.startup();

        // wait 10 seconds for server boot
        for (int i = 0; i < 10; i++)
            if (server.isStarted())
                Thread.sleep(1000);

        // expect running server
        Assertions.assertTrue(server.isStarted());

        servers.put(port, server);
    }

    @BeforeEach
    public void begin()
            throws Exception {

        File configFile = new File("./etc", "backend.properties");

        Properties serverProperties = new Properties();
        Properties backendProperties = new Properties();

        serverProperties.setProperty("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend");

        try {

            backendProperties.load(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

        } catch (IOException e) {

            throw new UncheckedIOException(e);
        }

        directoryBackend =
                new DirectoryBackendFactory(Main.createConfiguration(serverProperties, backendProperties))
                        .getPermanentDirectory();

        directoryBackend.startup();
    }

    @AfterEach
    public void end()
            throws Exception {

        directoryBackend.shutdown();
        directoryBackend = null;
    }

    protected InitialDirContext createContext(String userId, String password, int port)
            throws Exception {

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=" + userId + ",ou=users,dc=json");
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldaps://" + host + ":" + port);

        return new InitialDirContext(env);
    }
}
