package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.runner.JUnit5TestRunner;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;


public class BootTest {

    @Test
    public void start() {

        String[] args = new String[]{"no-colors"};

        JUnit5TestRunner.main(args);

        Assert.assertTrue(Files.exists(Paths.get("./tmp")));
    }
}
