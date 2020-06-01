/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package com.aservo.ldap.adapter.runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import javax.xml.stream.XMLStreamException;
import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * {@code LegacyXmlReportGeneratingListener} is a {@link TestExecutionListener} that
 * generates a separate XML report for each {@linkplain TestPlan#getRoots() root}
 * in the {@link TestPlan}.
 *
 * <p>Note that the generated XML format is compatible with the <em>legacy</em>
 * de facto standard for JUnit 4 based test reports that was made popular by the
 * Ant build system.
 *
 * @see org.junit.platform.launcher.listeners.LoggingListener
 * @see org.junit.platform.launcher.listeners.SummaryGeneratingListener
 * @since 1.4
 */
@API(status = EXPERIMENTAL, since = "1.4")
public class LegacyXmlReportGeneratingListener implements TestExecutionListener {

    private final Path reportXmlFilePath;
    private final PrintWriter out;
    private final Clock clock;

    private XmlReportData reportData;

    public LegacyXmlReportGeneratingListener(Path reportXmlFilePath, PrintWriter out) {
        this(reportXmlFilePath, out, Clock.systemDefaultZone());
    }

    // For tests only
    LegacyXmlReportGeneratingListener(String reportXmlFilePath, PrintWriter out, Clock clock) {
        this(Paths.get(reportXmlFilePath), out, clock);
    }

    private LegacyXmlReportGeneratingListener(Path reportXmlFilePath, PrintWriter out, Clock clock) {
        this.reportXmlFilePath = reportXmlFilePath;
        this.out = out;
        this.clock = clock;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.reportData = new XmlReportData(testPlan, clock);
        try {
            Files.createDirectories(this.reportXmlFilePath.getParent());
        } catch (IOException e) {
            printException("Could not create reports directory: " + this.reportXmlFilePath.getParent(), e);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        this.reportData = null;
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        this.reportData.markSkipped(testIdentifier, reason);
        writeXmlReportInCaseOfRoot(testIdentifier);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        this.reportData.markStarted(testIdentifier);
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        this.reportData.addReportEntry(testIdentifier, entry);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        this.reportData.markFinished(testIdentifier, result);
        writeXmlReportInCaseOfRoot(testIdentifier);
    }

    private void writeXmlReportInCaseOfRoot(TestIdentifier testIdentifier) {
        if (isRoot(testIdentifier)) {
            String rootName = UniqueId.parse(testIdentifier.getUniqueId()).getSegments().get(0).getValue();
            writeXmlReportSafely(testIdentifier, rootName);
        }
    }

    private void writeXmlReportSafely(TestIdentifier testIdentifier, String rootName) {

        try (Writer fileWriter = Files.newBufferedWriter(reportXmlFilePath)) {
            new XmlReportWriter(this.reportData).writeXmlReport(testIdentifier, fileWriter);
        } catch (XMLStreamException | IOException e) {
            printException("Could not write XML report: " + reportXmlFilePath, e);
        }
    }

    private boolean isRoot(TestIdentifier testIdentifier) {
        return !testIdentifier.getParentId().isPresent();
    }

    private void printException(String message, Exception exception) {
        out.println(message);
        exception.printStackTrace(out);
    }

}
