package com.aservo.ldap.adapter.runner;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;


public class JUnit5TestRunner {

    public static TestExecutionSummary executeTests(PrintWriter out,
                                                    Path reportXmlFilePath,
                                                    boolean disableAnsiColors,
                                                    String[] testPackages,
                                                    String[] testClasses) {

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(summaryListener);
        launcher.registerTestExecutionListeners(new FlatPrintingListener(out, disableAnsiColors));
        launcher.registerTestExecutionListeners(new LegacyXmlReportGeneratingListener(reportXmlFilePath, out));

        LauncherDiscoveryRequest discoveryRequest =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(Stream.concat(
                                Arrays.stream(testPackages).map(DiscoverySelectors::selectPackage),
                                Arrays.stream(testClasses).map(DiscoverySelectors::selectClass)
                        ).collect(Collectors.toList()))
                        .build();

        launcher.execute(discoveryRequest);

        return summaryListener.getSummary();
    }

    public static void main(String[] args) {

        List<String> arguments = Arrays.asList(args);

        boolean disableAnsiColors = false;

        if (arguments.contains("no-colors"))
            disableAnsiColors = true;

        Path reportXmlFilePath =
                Arrays.stream(args)
                        .filter((x) -> x.startsWith("report-xml-file-path="))
                        .findAny()
                        .map((arg) -> Paths.get(arg.substring(21)))
                        .orElse(Paths.get("target/test-reports/TEST-default.xml"));

        String[] testPackages =
                Arrays.stream(args)
                        .filter((x) -> x.startsWith("test-packages="))
                        .findAny()
                        .map((testPackage) -> {

                            return Arrays.stream(testPackage.split(","))
                                    .filter((x) -> !x.isEmpty())
                                    .map((x) -> x.substring(14))
                                    .toArray(String[]::new);
                        })
                        .orElse(new String[0]);

        String[] testClasses =
                Arrays.stream(args)
                        .filter((x) -> x.startsWith("test-classes="))
                        .findAny()
                        .map((testPackage) -> {

                            return Arrays.stream(testPackage.split(","))
                                    .filter((x) -> !x.isEmpty())
                                    .map((x) -> x.substring(13))
                                    .toArray(String[]::new);
                        })
                        .orElse(new String[0]);

        System.setProperty("junit.jupiter.execution.parallel.enabled", "true");
        System.setProperty("junit.jupiter.execution.parallel.mode.default", "same_thread");
        System.setProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent");

        PrintWriter out = new PrintWriter(System.out);

        TestExecutionSummary summary =
                executeTests(out, reportXmlFilePath, disableAnsiColors, testPackages, testClasses);

        summary.printFailuresTo(out);
        summary.printTo(out);
    }
}
