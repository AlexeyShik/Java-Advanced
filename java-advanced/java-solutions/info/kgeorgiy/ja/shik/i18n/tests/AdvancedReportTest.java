package info.kgeorgiy.ja.shik.i18n.tests;

import info.kgeorgiy.ja.shik.i18n.TextStatistics;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AdvancedReportTest extends BaseTest {

    public AdvancedReportTest() {
        inputLocale = TextStatistics.buildLocale("zh-CN");
    }

    @Test
    public void test_01_print_example_report() throws IOException {
        final String input = tagFromLocal(inputLocale);
        final String inputFileName = ROOT + "homework_" + input + ".txt";
        for (final String output : new String[]{"ru-RU", "en-UK"}) {
            outputLocale = TextStatistics.buildLocale(output);
            TextStatistics.main(new String[]{"zh-CN",
                    outputLocale.getLanguage() + "-" + outputLocale.getCountry(), inputFileName, OUTPUT});
            System.out.println(Files.readString(Path.of(OUTPUT)));
        }
    }

    @Test
    public void test_02_incorrect_arguments() {
        TextStatistics.main(null);
        TextStatistics.main(new String[]{});
        TextStatistics.main(new String[]{"GK", "GK", "input", "output"});
        TextStatistics.main(new String[]{"ch-RU", "ru-RU", "input", null});
        TextStatistics.main(new String[]{"ch-RU", "en-UK", "input", null});
        TextStatistics.main(new String[]{"ru-RU", "ru-RU", "noSuchFile", "noSuchFile"});
        TextStatistics.main(new String[]{"ru-RU", "en-UK", "noSuchFile", "noSuchFile"});
    }
}
