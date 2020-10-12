package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.CrowdDirectoryBackendTest;
import com.aservo.ldap.adapter.runner.JUnit5TestRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;


public class BootIntegrationTest {

    @Test
    public void start() {

        System.setProperty("directory-backend", "com.aservo.ldap.adapter.backend.CrowdDirectoryBackend");

        String[] args = new String[]{
                "no-colors",
                "test-classes=" + CrowdDirectoryBackendTest.class.getName(),
                "report-xml-file-path=target/test-reports/TEST-integration.xml"
        };

        JUnit5TestRunner.main(args);

        Assert.assertTrue(Files.exists(Paths.get("./tmp")));
    }
}
