package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.runner.JUnit5TestRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;


public class BootUnitTest {

    @Test
    public void start() {

        System.setProperty("directory-backend", "com.aservo.ldap.adapter.JsonDirectoryBackend");

        String[] args = new String[]{
                "no-colors",
                "test-packages=com.aservo.ldap.adapter",
                "report-xml-file-path=target/test-reports/TEST-units.xml"
        };

        JUnit5TestRunner.main(args);

        Assert.assertTrue(Files.exists(Paths.get("./tmp")));
    }
}
