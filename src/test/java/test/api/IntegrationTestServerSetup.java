package test.api;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;


public interface IntegrationTestServerSetup {

    default String getHost() {

        return "localhost";
    }

    int getPort();

    default String getUrl() {

        StringBuilder builder = new StringBuilder();

        if (isSslEnabled())
            builder.append("ldaps://");
        else
            builder.append("ldap://");

        builder.append(getHost());
        builder.append(":");
        builder.append(getPort());

        return builder.toString();
    }

    boolean isSslEnabled();

    boolean isFlatteningEnabled();

    default Path getTestDirectory() {

        StringBuilder builder = new StringBuilder();

        builder.append("./tmp/");
        builder.append(getPort());

        return Paths.get(builder.toString());
    }

    Properties getServerProperties();

    Properties getBackendProperties();

    default void startup()
            throws Exception {
    }

    default void shutdown()
            throws Exception {
    }
}
