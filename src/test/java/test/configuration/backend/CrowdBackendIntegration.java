package test.configuration.backend;

import test.api.IntegrationTestBackendSetup;
import test.api.helper.ShellAccess;

import java.util.Properties;


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

        String crowdTestVersion = System.getenv("CROWD_TEST_VERSION");

        if (crowdTestVersion == null || crowdTestVersion.trim().isEmpty())
            throw new IllegalArgumentException("Missing env property CROWD_TEST_VERSION.");

        ShellAccess.syncExec("./crowd-test-setup/it-boot.sh");
    }

    public void shutdown()
            throws Exception {

        ShellAccess.syncExec("./crowd-test-setup/stop.sh");
    }
}
