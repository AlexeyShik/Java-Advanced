package info.kgeorgiy.ja.shik.bank.tests;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

public class BankTests {
    public static void main(final String[] args) {
        final JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.err));
        System.exit(junit.run(AdvancedClientTest.class, AdvancedBankTest.class).wasSuccessful() ? 0 : 1);
    }
}
