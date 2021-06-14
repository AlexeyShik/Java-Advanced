package info.kgeorgiy.ja.shik.i18n.tests;

import java.util.Locale;

public class BaseTest {
    protected static final String ROOT = "info/kgeorgiy/ja/shik/i18n/resources/";
    protected static final String OUTPUT = ROOT + "output.txt";
    protected Locale inputLocale;
    protected Locale outputLocale;

    protected static String tagFromLocal(final Locale locale) {
        return locale.getLanguage() + "-" + locale.getCountry();
    }
}
