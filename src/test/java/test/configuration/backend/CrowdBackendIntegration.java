package test.configuration.backend;

import java.util.Properties;
import test.api.IntegrationTestBackendSetup;


public class CrowdBackendIntegration
        implements IntegrationTestBackendSetup {

    public Properties getProperties() {

        Properties properties = new Properties();

        properties.put("directory-backend.permanent",
                "de.aservo.ldap.adapter.backend.CrowdDirectoryBackend");

        properties.put("directory-backend.session", "");

        return properties;
    }

    public void startup()
            throws Exception {
    }

    public void shutdown()
            throws Exception {
    }
}
