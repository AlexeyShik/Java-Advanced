package info.kgeorgiy.ja.shik.i18n.tests;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

public class TextStatisticsTests {

    public static void main(final String[] args) {
        final JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.err));
        System.exit(junit.run(AdvancedTextStatisticsTest.class, AdvancedReportTest.class).wasSuccessful() ? 0 : 1);
    }
}
