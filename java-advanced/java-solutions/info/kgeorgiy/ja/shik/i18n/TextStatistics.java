package info.kgeorgiy.ja.shik.i18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextStatistics {

    public enum StatType {
        SENTENCE,
        WORD,
        NUMBER,
        MONEY,
        DATE
    }

    public static class TextStat<V> {
        public StatType type;
        private int occurrences;
        private int distinctOccurrences;
        private V minValue;
        private V maxValue;
        private int minLength;
        private int maxLength;
        private V minLengthValue;
        private V maxLengthValue;
        private double averageLength;
        private V averageValue;

        public StatType getType() {
            return type;
        }

        public int getOccurrences() {
            return occurrences;
        }

        public int getDistinctOccurrences() {
            return distinctOccurrences;
        }

        public V getMinValue() {
            return minValue;
        }

        public V getMaxValue() {
            return maxValue;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public V getMinLengthValue() {
            return minLengthValue;
        }

        public V getMaxLengthValue() {
            return maxLengthValue;
        }

        public double getAverageLength() {
            return averageLength;
        }

        public V getAverageValue() {
            return averageValue;
        }
    }

    private static class Parser {
        private final String text;
        private final Locale locale;
        final NumberFormat numberFormat;
        final NumberFormat currencyFormat;
        final List<DateFormat> dateFormats;

        public Parser(final String text, final Locale locale) {
            this.text = text;
            this.locale = locale;
            numberFormat = NumberFormat.getNumberInstance(locale);
            currencyFormat = NumberFormat.getCurrencyInstance(locale);
            dateFormats = List.of(
                    DateFormat.getDateInstance(DateFormat.FULL, locale),
                    DateFormat.getDateInstance(DateFormat.LONG, locale),
                    DateFormat.getDateInstance(DateFormat.MEDIUM, locale),
                    DateFormat.getDateInstance(DateFormat.SHORT, locale)
            );
        }

        private List<String> parseStringElements(final BreakIterator iterator, final Predicate<String> predicate) {
            final List<String> elements = new ArrayList<>();
            iterator.setText(text);
            int begin = iterator.first();
            int end = iterator.next();
            while (end != BreakIterator.DONE) {
                final String substr = text.substring(begin, end).trim();
                if (!substr.isEmpty() && predicate.test(substr)) {
                    elements.add(substr);
                }
                begin = end;
                end = iterator.next();
            }
            return elements;
        }

        private Number parseMoney(final ParsePosition pos) {
            return currencyFormat.parse(text, pos);
        }

        private <V> List<V> parseTokens(final Function<ParsePosition, V> toValue) {
            final List<V> elements = new ArrayList<>();
            final BreakIterator iterator = BreakIterator.getWordInstance(locale);
            iterator.setText(text);

            int begin = iterator.first();
            int end = iterator.next();
            int skipTo = 0;
            while (end != BreakIterator.DONE) {
                if (begin >= skipTo) {
                    final ParsePosition pos = new ParsePosition(begin);
                    final V number = toValue.apply(pos);
                    if (number != null) {
                        elements.add(number);
                        skipTo = pos.getIndex();
                    }
                }
                begin = end;
                end = iterator.next();
            }
            return elements;
        }

        private List<Number> parseMoneyElements() {
            return parseTokens(this::parseMoney);
        }

        private Date parseDate(final ParsePosition pos) {
            for (final DateFormat dateFormat : dateFormats) {
                final Date date = dateFormat.parse(text, pos);
                if (date != null) {
                    return date;
                }
            }
            return null;
        }

        private List<Date> parseDateElements() {
            return parseTokens(this::parseDate);
        }

        private List<Number> parseNumberElements() {
            final List<Number> elements = new ArrayList<>();
            final BreakIterator iterator = BreakIterator.getWordInstance(locale);
            iterator.setText(text);

            int begin = iterator.first();
            int end = iterator.next();
            int skipTo = 0;
            while (end != BreakIterator.DONE) {
                if (begin >= skipTo) {
                    final ParsePosition pos = new ParsePosition(begin);
                    if (parseMoney(pos) == null && parseDate(pos) == null) {
                        final Number number = numberFormat.parse(text, pos);
                        if (number != null) {
                            elements.add(number);
                        }
                    }
                    skipTo = pos.getIndex();
                }
                begin = end;
                end = iterator.next();
            }
            return elements;
        }

        private <V> TextStat<V> getCommonStat(final List<V> elements, final StatType type, final Comparator<V> comparator) {
            final TextStat<V> stat = new TextStat<>();
            if (elements.isEmpty()) {
                return stat;
            }
            stat.type = type;
            stat.occurrences = elements.size();
            elements.sort(comparator);
            stat.distinctOccurrences = 1;
            for (int i = 1; i < elements.size(); ++i) {
                if (comparator.compare(elements.get(i - 1), elements.get(i)) != 0) {
                    ++stat.distinctOccurrences;
                }
            }
            stat.minValue = elements.get(0);
            stat.maxValue = elements.get(elements.size() - 1);
            return stat;
        }

        private TextStat<String> getStringStat(final StatType type,
                                               final BreakIterator iterator, final Predicate<String> predicate) {
            final List<String> elements = parseStringElements(iterator, predicate);
            final TextStat<String> stat = getCommonStat(elements, type, Collator.getInstance(locale)::compare);
            if (elements.isEmpty()) {
                return stat;
            }
            elements.sort(Comparator.comparing(String::length));
            stat.minLengthValue = elements.get(0);
            stat.maxLengthValue = elements.get(elements.size() - 1);
            stat.minLength = stat.minLengthValue.length();
            stat.maxLength = stat.maxLengthValue.length();
            stat.averageLength = elements.stream().collect(Collectors.averagingDouble(String::length));
            return stat;
        }

        private TextStat<String> getSentenceStat() {
            return getStringStat(StatType.SENTENCE, BreakIterator.getSentenceInstance(locale), s -> true);
        }

        private TextStat<String> getWordStat() {
            return getStringStat(StatType.WORD, BreakIterator.getWordInstance(locale),
                    s -> s.chars().allMatch(Character::isLetter));
        }

        private TextStat<Number> getNumericStat(final List<Number> elements, final StatType type) {
            final TextStat<Number> stat = getCommonStat(elements, type,
                    Comparator.comparing(Number::doubleValue));
            if (stat.occurrences > 0) {
                stat.averageValue = elements.stream().collect(Collectors.averagingDouble(Number::doubleValue));
            }
            return stat;
        }

        private TextStat<Number> getNumberStat() {
            return getNumericStat(parseNumberElements(), StatType.NUMBER);
        }

        private TextStat<Number> getMoneyStat() {
            return getNumericStat(parseMoneyElements(), StatType.MONEY);
        }

        private TextStat<Date> getDateStat() {
            final List<Date> elements = parseDateElements();
            final TextStat<Date> stat = getCommonStat(elements, StatType.DATE,
                    Comparator.comparing(Date::getTime));
            if (stat.occurrences > 0) {
                stat.averageValue = new Date(elements.stream()
                        .collect(Collectors.averagingLong(Date::getTime)).longValue());
            }
            return stat;
        }

        private Map<StatType, TextStat<?>> getTextStat() {
            return Map.of(
                    StatType.SENTENCE, getSentenceStat(),
                    StatType.WORD, getWordStat(),
                    StatType.NUMBER, getNumberStat(),
                    StatType.MONEY, getMoneyStat(),
                    StatType.DATE, getDateStat()
            );
        }
    }

    private static class Formatter {
        private final Locale inputLocale;
        private final Map<StatType, TextStat<?>> textStat;
        private final String inputFileName;
        private final ResourceBundle bundle;
        private final NumberFormat numberFormat;
        private final Function<String, MessageFormat> toMessageFormat;

        private Formatter(final Locale inputLocale, final Locale outputLocale,
                          final Map<StatType, TextStat<?>> textStat, final String inputFileName) {
            this.inputLocale = inputLocale;
            this.textStat = textStat;
            this.inputFileName = inputFileName;
            bundle = getTextBundle(outputLocale);
            numberFormat = NumberFormat.getNumberInstance(inputLocale);
            toMessageFormat = s -> new MessageFormat(s, this.inputLocale);
        }


        private List<String> appendHeader() {
            return List.of(getAndFormat("analyzedFile", "oneArgEnding", inputFileName));
        }

        private List<String> appendSummary() {
            final List<String> args = new LinkedList<>();
            args.add(bundle.getString("summaryStatistics"));
            for (final StatType type : StatType.values()) {
                args.add(getAndFormat(
                        type.toString().toLowerCase(Locale.ROOT) + "Summary",
                        "oneArgEnding",
                        numberFormat.format(textStat.get(type).occurrences)));
            }
            return args;
        }

        private String[] joinArrays(final String[] a, final String[] b) {
            return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
        }

        private String applyAndFormat(final String key, final String... options) {
            return toMessageFormat.apply(bundle.getString(key)).format(options);
        }

        private String getAndFormat(final String name, final String ending, final String... options) {
            final String[] nameOption = new String[]{bundle.getString(name)};
            return Arrays.stream(options).anyMatch(Objects::isNull)
                    ? applyAndFormat("notFound", nameOption)
                    : applyAndFormat(ending, joinArrays(nameOption, options));
        }

        private String safeFormat(final Object value, final Format format) {
            return value == null ? null : format.format(value);
        }

        private List<String> appendStringType(final StatType type) {
            final String typeName = type.toString().toLowerCase(Locale.ROOT);
            final TextStat<?> currentStat = textStat.get(type);
            return List.of(bundle.getString(typeName + "Statistics"),
                    getAndFormat(typeName + "Number", "twoArgDifferentEnding",
                            numberFormat.format(currentStat.occurrences),
                            numberFormat.format(currentStat.distinctOccurrences)),
                    getAndFormat(typeName + "Min", "oneArgStringEnding", (String) currentStat.minValue),
                    getAndFormat(typeName + "Max", "oneArgStringEnding", (String) currentStat.maxValue),
                    getAndFormat(typeName + "MinLength", "twoArgEnding",
                            numberFormat.format(currentStat.minLength), (String) currentStat.minLengthValue),
                    getAndFormat(typeName + "MaxLength", "twoArgEnding",
                            numberFormat.format(currentStat.maxLength), (String) currentStat.maxLengthValue),
                    getAndFormat(typeName + "Average", "oneArgEnding", numberFormat.format(currentStat.averageLength)));
        }

        private List<String> appendCustomType(final StatType type, final Format formatter) {
            final String typeName = type.toString().toLowerCase(Locale.ROOT);
            final TextStat<?> currentStat = textStat.get(type);
            return List.of(bundle.getString(typeName + "Statistics"),
                    getAndFormat(typeName + "Number", "twoArgDifferentEnding",
                            numberFormat.format(currentStat.occurrences),
                            numberFormat.format(currentStat.distinctOccurrences)),
                    getAndFormat(typeName + "Min", "oneArgEnding", safeFormat(currentStat.minValue, formatter)),
                    getAndFormat(typeName + "Max", "oneArgEnding", safeFormat(currentStat.maxValue, formatter)),
                    getAndFormat(typeName + "Average", "oneArgEnding", safeFormat(currentStat.averageValue, formatter)));
        }

        private String[] generateReportArguments() {
            final List<List<String>> arguments = List.of(
                    appendHeader(),
                    appendSummary(),
                    appendStringType(StatType.SENTENCE),
                    appendStringType(StatType.WORD),
                    appendCustomType(StatType.NUMBER, numberFormat),
                    appendCustomType(StatType.MONEY, NumberFormat.getCurrencyInstance(inputLocale)),
                    appendCustomType(StatType.DATE, DateFormat.getDateInstance(DateFormat.DEFAULT, inputLocale))
            );
            return arguments.stream().flatMap(Collection::stream).toArray(String[]::new);
        }

        private String toFormattedString() throws IOException {
            final String template = Files.readString(Path.of("info/kgeorgiy/ja/shik/i18n/report_template.txt"));
            return toMessageFormat.apply(template).format(generateReportArguments());
        }
    }

    public static Locale buildLocale(final String languageTag) {
        return new Locale.Builder().setLanguageTag(languageTag).build();
    }

    public static Map<StatType, TextStat<?>> getTextStat(final String text, final Locale inputLocale) {
        return new Parser(text, inputLocale).getTextStat();
    }

    private static ResourceBundle getBundle(final String name, final Locale locale) {
        return ResourceBundle.getBundle("info.kgeorgiy.ja.shik.i18n." + name, locale);
    }

    private static ResourceBundle getTextBundle(final Locale locale) {
        return getBundle("TextResourceBundle", locale);
    }

    private static ResourceBundle getMainBundle(final Locale locale) {
        return getBundle("MainResourceBundle", locale);
    }

    private static String formatException(final ResourceBundle bundle, final String key, final Exception e) {
        return MessageFormat.format(bundle.getString(key), e.getMessage());
    }

    public static void main(final String[] args) {
        ResourceBundle bundle = getMainBundle(Locale.ENGLISH);
        if (args == null || args.length <= 1 || args[1] == null) {
            System.err.println(bundle.getString("usage"));
            return;
        }
        final Locale outputLocale = buildLocale(args[1]);
        if (!"ru".equals(outputLocale.getLanguage()) && !"en".equals(outputLocale.getLanguage())) {
            System.err.println(bundle.getString("invalidLang"));
            return;
        }
        bundle = getMainBundle(outputLocale);
        if (args.length != 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println(bundle.getString("usage"));
            return;
        }
        final Locale inputLocale = buildLocale(args[0]);
        try {
            final String text = Files.readString(Path.of(args[2]));
            final Map<StatType, TextStat<?>> textStat = getTextStat(text, inputLocale);
            final Path outputPath = Path.of(args[3]);
            final Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final Formatter formatter = new Formatter(inputLocale, outputLocale, textStat, args[2]);
            Files.writeString(outputPath, formatter.toFormattedString(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            System.err.println(formatException(bundle, "ioException", e));
        } catch (final InvalidPathException e) {
            System.err.println(formatException(bundle, "invalidPath", e));
        }
    }
}
