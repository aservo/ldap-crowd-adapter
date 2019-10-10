package com.aservo.ldap.adapter.runner;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;


public class JUnit5TestRunner {

    public static TestExecutionSummary executeTests(PrintWriter out, Path reportsDir, boolean disableAnsiColors) {

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(summaryListener);
        launcher.registerTestExecutionListeners(new FlatPrintingListener(out, disableAnsiColors));
        launcher.registerTestExecutionListeners(new LegacyXmlReportGeneratingListener(reportsDir, out));

        LauncherDiscoveryRequest discoveryRequest =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectPackage("com.aservo.ldap.adapter"))
                        .build();

        launcher.execute(discoveryRequest);

        return summaryListener.getSummary();
    }

    public static void main(String[] args) {

        List<String> arguments = Arrays.asList(args);

        boolean disableAnsiColors = false;

        if (arguments.contains("no-colors"))
            disableAnsiColors = true;

        System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
        System.setProperty("junit.jupiter.execution.parallel.mode.default", "same_thread");
        System.setProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent");

        PrintWriter out = new PrintWriter(System.out);
        TestExecutionSummary summary = executeTests(out, Paths.get("target/test-reports"), disableAnsiColors);

        summary.printFailuresTo(out);
        summary.printTo(out);
    }
}
