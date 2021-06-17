package test.configuration.backend;

import java.io.IOException;
import java.util.Properties;
import test.api.IntegrationTestBackendSetup;


public class CrowdBackendIntegration
        implements IntegrationTestBackendSetup {

    public Properties getProperties() {

        Properties properties = new Properties();

        properties.put("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.CrowdDirectoryBackend");

        properties.put("directory-backend.session", "");

        return properties;
    }

    public void startup()
            throws Exception {

        String crowdTestVersion = System.getenv("CROWD_TEST_VERSION");

        if (crowdTestVersion == null || crowdTestVersion.trim().isEmpty())
            throw new IllegalArgumentException("Missing env property CROWD_TEST_VERSION.");

        exec("./crowd-test-setup/it-boot.sh");
    }

    public void shutdown()
            throws Exception {

        exec("./crowd-test-setup/stop.sh");
    }

    private void exec(String... command)
            throws Exception {

        Process process = new ProcessBuilder(command).start();
        int exitCode = process.waitFor();

        if (exitCode != 0)
            throw new IOException("The command " + String.join(" ", command) + " quits with exit code " +
                    exitCode + ".");
    }
}
