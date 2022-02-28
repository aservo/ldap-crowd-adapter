package test.configuration.backend;

import test.api.BackendSetup;

import java.util.Properties;


public class JsonBackend
        implements BackendSetup {

    public Properties getProperties() {

        Properties properties = new Properties();

        properties.put("directory-backend.permanent",
                "de.aservo.ldap.adapter.backend.JsonDirectoryBackend");

        properties.put("directory-backend.session", "");

        properties.put("db-uri", "file:./src/test/resources/de/aservo/ldap/adapter/db.json");

        return properties;
    }
}
