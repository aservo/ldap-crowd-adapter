package com.aservo.ldap.adapter.helper;

import com.aservo.ldap.adapter.runner.JUnit5TestRunner;
import org.junit.Test;


public class BootTest {

    @Test
    public void start() {

        String[] args = new String[]{"no-colors"};

        JUnit5TestRunner.main(args);
    }
}
