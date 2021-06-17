package test.api;

import com.aservo.ldap.adapter.DirectoryBackendFactory;
import com.aservo.ldap.adapter.Main;
import com.aservo.ldap.adapter.ServerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


public abstract class AbstractBackendTest {

    private final BackendSetup backendSetup;
    private DirectoryBackendFactory backendFactory;

    public AbstractBackendTest(BackendSetup backendSetup) {

        this.backendSetup = backendSetup;
    }

    public BackendSetup getBackendSetup() {

        return backendSetup;
    }

    public DirectoryBackendFactory getBackendFactory() {

        return backendFactory;
    }

    @BeforeAll
    public void setup()
            throws Exception {

        // preparation for boot process
        if (backendSetup instanceof IntegrationTestBackendSetup)
            ((IntegrationTestBackendSetup) backendSetup).startup();

        ServerConfiguration config = Main.createConfiguration(backendSetup.getProperties());

        backendFactory = new DirectoryBackendFactory(config);

        // boot process
        backendFactory.startup();
    }

    @AfterAll
    public void shutdown()
            throws Exception {

        backendFactory.shutdown();

        if (backendSetup instanceof IntegrationTestBackendSetup)
            ((IntegrationTestBackendSetup) backendSetup).shutdown();
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
