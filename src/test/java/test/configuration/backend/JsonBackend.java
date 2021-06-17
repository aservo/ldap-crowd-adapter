package test.configuration.backend;

import java.util.Properties;
import test.api.BackendSetup;


public class JsonBackend
        implements BackendSetup {

    public Properties getProperties() {

        Properties properties = new Properties();

        properties.put("directory-backend.permanent",
                "com.aservo.ldap.adapter.backend.JsonDirectoryBackend");

        properties.put("directory-backend.session", "");

        properties.put("db-uri", "file:./src/test/resources/com/aservo/ldap/adapter/db.json");

        return properties;
    }
}
