package test.api;

import com.aservo.ldap.adapter.CommonLdapServer;
import com.aservo.ldap.adapter.Main;
import com.aservo.ldap.adapter.ServerConfiguration;
import java.nio.file.Files;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import test.api.helper.AssertionsLdap;


public abstract class AbstractServerTest {

    private static final Object lock = new Object();

    private final IntegrationTestServerSetup serverSetup;
    private CommonLdapServer server;
    private AssertionsLdap ldapAssertions;

    public AbstractServerTest(IntegrationTestServerSetup serverSetup) {

        this.serverSetup = serverSetup;
    }

    public IntegrationTestServerSetup getServerSetup() {

        return serverSetup;
    }

    public CommonLdapServer getServer() {

        return server;
    }

    protected AssertionsLdap getLdapAssertions() {

        return ldapAssertions;
    }

    protected InitialDirContext createContext(String userId, String password)
            throws Exception {

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=" + userId + ",ou=users,dc=json");
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, serverSetup.getUrl());

        if (serverSetup.isSslEnabled())
            env.put(Context.SECURITY_PROTOCOL, "ssl");

        return new InitialDirContext(env);
    }

    @BeforeAll
    public void setup()
            throws Exception {

        synchronized (lock) {

            if (Files.exists(serverSetup.getTestDirectory()))
                FileUtils.deleteDirectory(serverSetup.getTestDirectory().toFile());

            Files.createDirectories(serverSetup.getTestDirectory());

            // preparation for boot process
            serverSetup.startup();

            ServerConfiguration config =
                    Main.createConfiguration(serverSetup.getServerProperties(), serverSetup.getBackendProperties());

            server = Main.createServerInstance(config);

            // boot process
            server.startup();

            // wait max 10 seconds for server boot
            for (int i = 0; i < 10; i++)
                if (server.isStarted())
                    Thread.sleep(1000);

            // expect running server
            Assertions.assertTrue(server.isStarted());

            ldapAssertions = new AssertionsLdap(
                    serverSetup.isFlatteningEnabled(),
                    server.getServerConfig().isAbbreviateSnAttribute(),
                    server.getServerConfig().isAbbreviateGnAttribute());
        }
    }

    @AfterAll
    public void shutdown()
            throws Exception {

        synchronized (lock) {

            server.shutdown();
            serverSetup.shutdown();
        }
    }

    @BeforeEach
    public void begin()
            throws Exception {
    }

    @AfterEach
    public void end()
            throws Exception {
    }
}
