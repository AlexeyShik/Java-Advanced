package info.kgeorgiy.ja.shik.i18n.tests;

import info.kgeorgiy.ja.shik.i18n.TextStatistics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@RunWith(Parameterized.class)
public class AdvancedTextStatisticsTest extends BaseTest {

    public AdvancedTextStatisticsTest(final String inputTag, final String outputTag) {
        this.inputLocale = TextStatistics.buildLocale(inputTag);
        this.outputLocale = TextStatistics.buildLocale(outputTag);
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> languageTags() {
        return Arrays.asList(new Object[][]{
                {"ru-RU", "ru-RU"},
                {"en-UK", "ru-RU"},
                {"ru-RU", "en-UK"},
                {"en-UK", "en-UK"},
                {"zh-CN", "ru-RU"},
                {"zh-CN", "en-UK"}
        });
    }

    private static String toFirstUpper(final String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private static String safeToString(final Object o) {
        return o == null ? "null" : o.toString();
    }

    private void commonTest(final String inputName) throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        final String input = tagFromLocal(inputLocale);
        final String text = Files.readString(Path.of(ROOT + inputName + "_" + input + ".txt"));
        final ResourceBundle bundle = ResourceBundle.getBundle(
                "info.kgeorgiy.ja.shik.i18n.resources." + toFirstUpper(inputName) + "ResourceBundle", inputLocale);
        final Map<TextStatistics.StatType, TextStatistics.TextStat<?>> textStat =
                TextStatistics.getTextStat(text, inputLocale);
        for (final var entry : textStat.entrySet()) {
            final TextStatistics.StatType type = entry.getKey();
            final TextStatistics.TextStat<?> stat = entry.getValue();
            final String typeName = type.name().toLowerCase(Locale.ROOT);
            for (final Field field : TextStatistics.TextStat.class.getDeclaredFields()) {
                final String name = field.getName();
                if ("type".equals(name)) {
                    continue;
                }
                final Method getter = TextStatistics.TextStat.class.getDeclaredMethod("get" + toFirstUpper(name));
                Object value = getter.invoke(stat);
                if (name.contains("occurrences")) {
                    value = NumberFormat.getInstance(inputLocale).format(value);
                } else  if (type == TextStatistics.StatType.NUMBER && value != null && name.contains("Value")
                        || "averageLength".equals(name)) {
                    value = NumberFormat.getInstance(inputLocale).format(value);
                } else if (type == TextStatistics.StatType.MONEY && value != null && name.contains("Value")) {
                    value = NumberFormat.getCurrencyInstance(inputLocale).format(value);
                } else if (type == TextStatistics.StatType.DATE && value != null && name.contains("Value")) {
                    value = DateFormat.getDateInstance(DateFormat.DEFAULT, inputLocale).format(value);
                }
                value = safeToString(value);
                final String str = typeName + "_" + name;
                final String expected = bundle.getString(str);
                Assert.assertEquals(expected, value);
            }
        }
    }

    @Test
    public void test_01_empty() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        commonTest("empty");
    }

    @Test
    public void test_02_homework() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        commonTest("homework");
    }

    @Test
    public void test_03_numbers() throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        commonTest("numbers");
    }

    @Test
    public void test_04_burn_performance() throws InvocationTargetException, IOException, NoSuchMethodException, IllegalAccessException {
        commonTest("performance");
    }
}
