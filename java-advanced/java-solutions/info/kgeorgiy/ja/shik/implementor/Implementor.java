package info.kgeorgiy.ja.shik.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;


/**
 * Implementation of {@link JarImpler}
 *
 * @author Alexey Shik
 */
public class Implementor implements JarImpler {
    /**
     * Platform independent line separator
     */
    private static final String NEW_LINE = System.lineSeparator();

    /**
     * Visitor that recursively
     * deletes files and directories from given path
     *
     * @see SimpleFileVisitor
     */
    private static final FileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {

        /**
         * Deletes already visited file
         *
         * @see FileVisitor#visitFile(Object, BasicFileAttributes)
         * @see Files#delete(Path)
         */
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Deletes already visited empty directory
         *
         * @see FileVisitor#postVisitDirectory(Object, IOException)
         * @see Files#delete(Path)
         */
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Default constructor
     */
    public Implementor() { }

    /**
     * Wrapper for {@link BufferedWriter}, that writes formatted implementation of the class in the output file
     */
    private Appender appender;

    /**
     * Checks if given {@code Class} can't be inherited
     *
     * @param token type token that would be checked
     * @return {@code true}, if given {@code Class} can't be inherited, {@code false} otherwise
     */
    private static boolean cannotBeInherited(final Class<?> token) {
        int modifiers = token.getModifiers();
        return Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)
                || token.equals(Enum.class)
                || token.isEnum()
                || token.isArray()
                || token.isPrimitive()
                || hasOnlyPrivateConstructors(token);
    }

    /**
     * Builds name of the implementation of given {@code Class} with given extension
     *
     * @param token     type token
     * @param extension extension for given {@code Class}
     * @return name of implementation of given {@code Class} with given extension
     */
    private static String getNameWithExtension(final Class<?> token, final String extension) {
        return getImplementationName(token) + extension;
    }

    /**
     * Builds name of the implementation of given {@code Class} with extension <var>.java</var>
     *
     * @param token type token
     * @return name of implementation of given {@code Class} with extension <var>.java</var>
     */
    private static String getSourceFileName(final Class<?> token) {
        return getNameWithExtension(token, ".java");
    }

    /**
     * Builds name of the implementation of given {@code Class} with extension <var>.class</var>
     *
     * @param token type token
     * @return name of implementation of given {@code Class} with extension <var>.class</var>
     */
    private static String getClassName(final Class<?> token) {
        return getNameWithExtension(token, ".class");
    }

    /**
     * Resolves root with package name from given {@code Class} and specified ending
     *
     * @param root   root path
     * @param token  type token
     * @param ending ending of resulting path
     * @return root path resolved with package name from given class and specified ending
     */
    private static Path resolve(final Path root, final Class<?> token, final String ending) {
        return root.resolve(Paths.get(token.getPackageName().replace(".", File.separator), ending));
    }

    @Override
    public void implement(final Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments must not be null");
        }
        if (cannotBeInherited(token)) {
            throw new ImplerException("Target class must be able to be inherited");
        }

        root = resolve(root, token, getSourceFileName(token));
        createParentDirectories(root);

        try (final BufferedWriter writer = Files.newBufferedWriter(root)) {
            appender = new Appender(writer);
            generateImplementation(token);
            generateInner(token, root);
        } catch (IOException e) {
            throw new ImplerException("IOException: " + e.getMessage(), e);
        }
    }

    /**
     * Creates parent directories, if they doesn't exists
     *
     * @param root {@link Path} to a file/directory
     */
    private static void createParentDirectories(final Path root) {
        try {
            Path parent = root.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            //  file may be accessible later
        }
    }

    /**
     * Generates implementation for given {@code Class}
     *
     * @param token type token
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void generateImplementation(final Class<?> token) throws ImplerException {
        generatePackage(token);
        generateHeader(token);
        generateConstructors(token);
        generateMethods(token);
    }

    /**
     * Checks that given {@code Class} in inner
     *
     * @param token type token
     * @return {@code true}, if given {@code Class} in inner, {@code false} otherwise
     */
    private static boolean isInner(final Class<?> token) {
        int modifiers = token.getModifiers();
        return !Modifier.isPrivate(modifiers) && Modifier.isAbstract(modifiers);
    }

    /**
     * Generates implementation for inner {@code Class}
     *
     * @param token type token
     * @param path  {@link Path} to inner {@code Class}
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private static void generateInner(final Class<?> token, final Path path) throws ImplerException {
        for (final Class<?> clazz : token.getDeclaredClasses()) {
            if (isInner(clazz)) {
                new Implementor().implement(clazz, path);
            }
        }
    }

    /**
     * Class is in use for writing formatted implementation in output file
     */
    private static class Appender {
        /**
         * Tabulation (4 whitespaces)
         */
        private static final String TAB = "    ";
        /**
         * {@link BufferedWriter} for writing in output file
         */
        private final BufferedWriter writer;
        /**
         * Number of indents for current line
         */
        private int indent;
        /**
         * Number of new lines after current line
         */
        private int newLines;

        /**
         * Constructor from {@link BufferedWriter}
         *
         * @param writer instance of {@code BufferedWriter}
         */
        Appender(final BufferedWriter writer) {
            this.writer = writer;
        }

        /**
         * Resets indent and new lines to default values
         */
        private void reset() {
            indent = 0;
            newLines = 0;
        }

        /**
         * Converts given {@code String} to {@code unicode} representation and writes it in output file
         *
         * @param string {@code String} to be written
         * @throws IOException if an {@link IOException} exception occurs
         */
        private void write(String string) throws IOException {
            writer.write(string
                    .chars()
                    .mapToObj(c -> c >= 128 ? String.format("\\u%04x", c) : Character.toString(c))
                    .reduce(String::concat)
                    .orElse(""));
        }

        /**
         * Appends new portion of given strings to the output file
         *
         * @param strings vararg of {@code String}
         * @throws ImplerException if an {@link IOException} occurs while writing in output file
         */
        public void append(final String... strings) throws ImplerException {
            try {
                write(TAB.repeat(indent));
                for (String string : strings) {
                    write(string);
                }
                write(NEW_LINE.repeat(newLines));
            } catch (IOException e) {
                throw new ImplerException("IOException: " + e.getMessage(), e);
            } finally {
                reset();
            }
        }

        /**
         * Sets current indent equal to given indent
         *
         * @param indent other indent
         * @return current state of Appender
         */
        public Appender setIndent(final int indent) {
            this.indent = indent;
            return this;
        }

        /**
         * Sets current number of new lines equal to given number of new lines
         *
         * @param newLines other number of new lines
         * @return current state of Appender
         */
        public Appender setNewLines(final int newLines) {
            this.newLines = newLines;
            return this;
        }
    }

    /**
     * Generates package for a given {@code Class}
     *
     * @param token type token
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void generatePackage(final Class<?> token) throws ImplerException {
        final String packageName = token.getPackageName();
        if (!packageName.isEmpty() && !packageName.startsWith("java")) {
            appender
                    .setNewLines(2)
                    .append(String.format("package %s;", packageName));
        }
    }

    /**
     * Gets name of given {@code Class} implementation.
     * Returns {@link Class#getSimpleName()} with ending {@code "Impl"}
     *
     * @param token type token
     * @return name of given {@code Class} implementation
     */
    private static String getImplementationName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Gets full name of given {@link Parameter} in format: {@link #getTypeName(Type)}
     * of type of parameter, concatenated through whitespace with name of parameter
     *
     * @param parameter {@code Parameter}
     * @return full name of given {@code Parameter}
     */
    private static String getFullParameterName(final Parameter parameter) {
        return String.format("%s %s",
                getTypeName(parameter.getParameterizedType()),
                parameter.getName());
    }

    /**
     * Gets modifiers from given {@link Executable} as a {@code String}
     *
     * @param executable instance of {@code Executable}
     * @return {@code String} that contains modifiers
     */
    private static String modifiersToString(final Executable executable) {
        return Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT) + " ";
    }

    /**
     * Gets name of given {@code Type}
     *
     * @param type instance of {@code Type}
     * @return name of given {@code Type}.
     * <ul>
     *     <li>
     *          If {@code type} is instance of {@code Class}
     *          then {@link Class#getCanonicalName()} is returned
     *     </li>
     *     <li>
     *         If {@code type} is instance of {@code ParametrizedType}
     *         then its string representation is returned
     *     </li>
     *     <li>
     *          If {@code type} is instance of {@code WildcardType}
     *          then its string representation is returned
     *     </li>
     *     <li>
     *         Else {@link Type#getTypeName()} is returned
     *     </li>
     * </ul>
     */
    private static String getTypeName(final Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getCanonicalName();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            try {
                return Class.forName(parameterizedType.getRawType().getTypeName()).getCanonicalName() +
                        collectGenericTypes(Arrays.stream(parameterizedType.getActualTypeArguments())
                                .map(Implementor::getTypeName));
            } catch (ClassNotFoundException ignored) {
                // impossible statement
            }
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] bounds = wildcardType.getLowerBounds();
            if (bounds.length == 0) {
                return getWildcardBounds(wildcardType.getUpperBounds(), "? extends ");
            } else {
                return getWildcardBounds(bounds, "? super ");
            }
        }
        return type.getTypeName();
    }

    /**
     * Gets bounds for given array of {@code Type}, concatenated with {@code prefix String},
     * that is {@code "? extends"} or {@code "? super}
     *
     * @param types array of {@code Type}
     * @param prefix prefix {@code String}
     * @return bounds for given array of {@code Type}, concatenated with {@code prefix String}
     */
    private static String getWildcardBounds(final Type[] types, final String prefix) {
        return prefix + Arrays.stream(types).map(Implementor::getTypeName).collect(Collectors.joining(", "));
    }

    /**
     * Collects generic type names from {@code Stream} to one string.
     *
     * @param stream {@code Stream} of {@code String}'s
     * @return Result of joining given {@code String}'s
     * using {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     * with {@code ","} as delimiter, {@code "<>"} as prefix and suffix
     */
    private static String collectGenericTypes(final Stream<String> stream) {
        String[] genericTypes = stream.toArray(String[]::new);
        return genericTypes.length == 0 ? ""
                : Arrays.stream(genericTypes).collect(Collectors.joining(", ", "<", ">"));
    }

    /**
     * Gets all generic types of given {@code Class}.
     *
     * @param token type token
     * @return Result of {@link #collectGenericTypes(Stream)} of all parameter type names
     * of given {@code Class}
     */
    private String getGenericClassTypes(final Class<?> token) {
        return collectGenericTypes(Arrays.stream(token.getTypeParameters()).map(TypeVariable::getName));
    }

    /**
     * Gets generic parameter names into one {@code String} from array of {@code TypeVariable}.
     *
     * @param typeVariables array of {@code TypeVariable}
     * @return {@code String} that contains information about all generic types
     * and their upper bounds. Different upper bounds are joined by
     * {@link Collectors#joining(CharSequence)} with delimiter {@code "&"}.
     * Different generic type are joined by {@link #collectGenericTypes(Stream)} with delimiter {@code ","}.
     */
    private static String getGenericParameterNames(final TypeVariable<?>[] typeVariables) {
        return collectGenericTypes(Arrays.stream(typeVariables).map(type ->
                type.getName() + " extends " +
                        Arrays.stream(type.getBounds()).map(Implementor::getTypeName).
                                collect(Collectors.joining(" & "))));
    }

    /**
     * Generates header for given {@code Class}
     *
     * @param token type token
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void generateHeader(final Class<?> token) throws ImplerException {
        appender
                .setNewLines(2)
                .append(String.format("public class %s%s%s%s%s {",
                        getImplementationName(token),
                        getGenericParameterNames(token.getTypeParameters()),
                        Modifier.isInterface(token.getModifiers()) ? " implements " : " extends ",
                        getTypeName(token),
                        getGenericClassTypes(token)));
    }


    /**
     * Checks that given {@code Class} has only private constructors
     *
     * @param token type token
     * @return {@code true}, if given {@code Class} has only private constructors, {@code false} otherwise
     */
    private static boolean hasOnlyPrivateConstructors(final Class<?> token) {
        boolean hasNonPrivate = false;
        boolean hasDefault = true;

        for (final Constructor<?> constructor : token.getDeclaredConstructors()) {
            hasNonPrivate |= isNonPrivate(constructor);
            hasDefault &= isNonPrivate(constructor) || constructor.getParameters().length != 0;
        }

        return !hasNonPrivate && !hasDefault;
    }

    /**
     * Gets body of implemented {@link Executable}
     *
     * @param executable instance of {@code Executable}
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void getBody(final Executable executable) throws ImplerException {
        getParameters(executable);
        getExceptions(executable);
        if (executable instanceof Constructor) {
            getRealization((Constructor<?>) executable);
        } else {
            getRealization((Method) executable);
        }
    }

    /**
     * Generates constructors for given {@code Class}
     *
     * @param token type token
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void generateConstructors(final Class<?> token) throws ImplerException {
        for (final Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (isNonPrivate(constructor)) {
                appender
                        .setIndent(1)
                        .append(modifiersToString(constructor), getImplementationName(token));

                getBody(constructor);
            }
        }
    }

    /**
     * Filters given methods by non {@link Method#isBridge()} and given {@link Predicate}.
     * After that wraps methods using {@link MethodSignature#MethodSignature(Method)}
     * and collects to given {@code Set}
     *
     * @param methods   array of {@link Method}
     * @param methodSet {@code Set} of {@code Methods}
     * @param predicate {@code Predicate} for {@code Method}
     */
    private static void getMethodsBy(final Method[] methods, final Set<MethodSignature> methodSet, final Predicate<Method> predicate) {
        Arrays.stream(methods)
                .filter(predicate)
                .filter(x -> !x.isBridge())
                .map(MethodSignature::new)
                .collect(Collectors.toCollection(() -> methodSet));
    }

    /**
     * Checks that given {@link Executable} is {@code abstract}
     *
     * @param method {@code Executable} for checking
     * @return {@code true} if {@code method} is {@code abstract},
     * {@code false} otherwise
     */
    private static boolean isAbstract(final Executable method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Checks that given {@link Executable} is not {@code private}
     *
     * @param method {@code Executable} for checking
     * @return {@code true} if {@code method} is not {@code private},
     * * {@code false} otherwise
     */
    private static boolean isNonPrivate(final Executable method) {
        return !Modifier.isPrivate(method.getModifiers());
    }

    /**
     * Collects abstract methods from array of {@link Method} to given {@code Set}
     *
     * @param methods   array of {@code Method}
     * @param methodSet {@code Set} of {@link MethodSignature}
     */
    private static void getAbstractMethods(final Method[] methods, final Set<MethodSignature> methodSet) {
        getMethodsBy(methods, methodSet, Implementor::isAbstract);
    }

    /**
     * Collects non-private methods from array of {@link Method} to given {@code Set}
     *
     * @param methods   array of {@code Method}
     * @param methodSet {@code Set} of {@link MethodSignature}
     */
    private static void getNonPrivateMethods(final Method[] methods, final Set<MethodSignature> methodSet) {
        getMethodsBy(methods, methodSet, Implementor::isNonPrivate);
    }

    /**
     * Class that wraps {@link Method} and represents its signature
     */
    private static class MethodSignature {
        /**
         * {@link Method} whose signature is represented
         */
        private final Method method;

        /**
         * Constructs {@code MethodSignature} from {@link Method}
         *
         * @param method {@code Method} instance
         */
        private MethodSignature(final Method method) {
            this.method = method;
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object.
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName().hashCode(),
                    Arrays.hashCode(method.getParameterTypes()));
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param obj the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(final Object obj) {
            if (method == obj) {
                return true;
            }
            if (!(obj instanceof MethodSignature)) {
                return false;
            }
            final Method other = ((MethodSignature) obj).method;
            return Objects.equals(method.getName(), other.getName())
                    && Arrays.equals(method.getParameterTypes(), other.getParameterTypes());
        }
    }

    /**
     * Generates realization for all required abstract methods of given {@code Class}
     *
     * @param token type token
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void generateMethods(final Class<?> token) throws ImplerException {
        final Set<MethodSignature> methods = new HashSet<>();
        for (Class<?> clazz = token; clazz != null; clazz = clazz.getSuperclass()) {
            getNonPrivateMethods(clazz.getDeclaredMethods(), methods);
        }
        getAbstractMethods(token.getMethods(), methods);  //  null is impossible
        for (final MethodSignature signature : methods) {
            final Method method = signature.method;
            if (isAbstract(method)) {
                appender
                        .setIndent(1)
                        .append(String.format("%s%s %s %s",
                                modifiersToString(method),
                                getGenericParameterNames(method.getTypeParameters()),
                                getTypeName(method.getGenericReturnType()),
                                method.getName()));
                getBody(method);
                appender
                        .setIndent(1)
                        .setNewLines(2)
                        .append("}");

            }
        }
        appender
                .append("}");
    }

    /**
     * Converts each element from array of type {@code T}
     * to its {@code String} representation and then
     * collects them all into single {@code String}
     * using {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     *
     * @param array       array of type {@code T}
     * @param mapFunction mapping function from {@code T} to {@code String}
     * @param leftBound   left bound for {@code Collectors.joining}
     * @param rightBound  right bound for {@code Collectors.joining}
     * @param <T>         generic array type
     * @return String representation of array of type {@code T}
     * using {@code Collectors.joining} after each {@code T}
     * is mapped to a String using {@code Function<T, String>}
     * Converts each element from array of type {@code T}
     */
    private static <T> String getJoining(final T[] array, final Function<T, String> mapFunction,
                                         final String leftBound, final String rightBound) {
        return Arrays.stream(array)
                .map(mapFunction)
                .collect(Collectors.joining(", ", leftBound, rightBound));
    }

    /**
     * Converts each element from array of {@link Executable#getParameters()}
     * to its {@code String} representation using {@link Implementor#getFullParameterName(Parameter)}
     * and then collects them all into single {@code String}
     * using {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     *
     * @param executable {@code Executable}, which parameters are to get
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     * @see #getJoining(Object[], Function, String, String)
     */
    private void getParameters(final Executable executable) throws ImplerException {
        appender
                .append(getJoining(executable.getParameters(), Implementor::getFullParameterName, "(", ")"));
    }

    /**
     * Converts each element from array of {@link Executable#getExceptionTypes()}
     * to its {@code String} representation using {@link Implementor#getTypeName(Type)}
     * and then collects them all into single {@code String}
     * using {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     * and then format it like "{@code throws IOException, ImplerException}"
     *
     * @param executable {@code Executable}, which parameters are to get
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     * @see #getJoining(Object[], Function, String, String)
     */
    private void getExceptions(final Executable executable) throws ImplerException {
        final Type[] exceptions = executable.getGenericExceptionTypes();
        appender
                .setNewLines(1)
                .append(String.format("%s%s {",
                        exceptions.length > 0 ? " throws " : "",
                        getJoining(exceptions, Implementor::getTypeName, "", "")));
    }

    /**
     * Gets realization for given {@link Constructor}
     *
     * @param constructor instance of {@code Constructor}
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void getRealization(final Constructor<?> constructor) throws ImplerException {
        appender
                .setIndent(2)
                .setNewLines(2)
                .append(String.format("super%s;%s}",
                        getJoining(constructor.getParameters(), Parameter::getName, "(", ")"),
                        NEW_LINE));
    }

    /**
     * Gets realization for given {@link Method}
     *
     * @param method instance of {@code Method}
     * @throws ImplerException if an {@link IOException} occurs while writing in output file
     */
    private void getRealization(final Method method) throws ImplerException {
        appender
                .setIndent(2)
                .setNewLines(1)
                .append(String.format("return%s;", getDefaultValue(method.getReturnType())));
    }

    /**
     * Gets default value for given {@code Class}
     *
     * @param returnType type token
     * @return default value for given {@code Class}
     */
    private static String getDefaultValue(final Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return " null";
        } else if (returnType.equals(boolean.class)) {
            return " false";
        } else if (returnType.equals(void.class)) {
            return "";
        }
        return " 0";
    }

    /**
     * Checks that arguments have correct format:
     * {@code [(optional) "-jar", class name, output file path]}
     *
     * @param args command line arguments
     * @throws ImplerException if arguments haven't correct format
     */
    private static void checkArgs(final String[] args) throws ImplerException {
        if (args == null || args.length < 2 || args.length > 3
                || args.length == 3 && !"-jar".equals(args[0])) {
            throw new ImplerException("Should be 2 or 3 arguments in format: " +
                    "[(optional) \"-jar\", class name, output file path]");
        }
    }

    /**
     * Gets type token from {@code String} name of {@code Class}
     *
     * @param token {@code String} name {@code Class}
     * @return corresponding {@code Class}
     * @throws ImplerException if {@link ClassNotFoundException}
     *                         is throws in {@link Class#forName(String)}
     */
    private static Class<?> getToken(final String token) throws ImplerException {
        try {
            return Class.forName(token);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Invalid class name found", e);
        }
    }

    /**
     * Gets {@link Path} from {@code String}
     *
     * @param path given {@code String}
     * @return corresponding {@code Path}
     * @throws ImplerException if {@link Path#of(String, String...)} throws {@link InvalidPathException}
     */
    private static Path getPath(final String path) throws ImplerException {
        try {
            return Path.of(path);
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path found", e);
        }
    }

    /**
     * Implements given class and writes its implementation in given file.
     * <p>
     * Invokes:
     * </p>
     * <ul>
     *
     *     <li>
     *         {@link #implement(Class, Path)} if arguments are {@code [class name, output file path]}
     *     </li>
     *     <li>
     *          {@link #implementJar(Class, Path)} if arguments are {@code ["-jar", class name, output file path]}
     *     </li>
     * </ul>
     *
     * @param args command line arguments
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void main(final String[] args) {
        try {
            checkArgs(args);
            if (args.length == 2) {
                new Implementor().implement(getToken(args[0]), getPath(args[1]));
            } else {
                new Implementor().implementJar(getToken(args[1]), getPath(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("ImplerException: " + e.getMessage());
        }
    }

    /**
     * Gets class path for given {@code Class}
     *
     * @param token type token
     * @return {@code String} representation of class path
     * @throws ImplerException if {@link URISyntaxException} is thrown by {@link URL#toURI()}
     */
    private static String getClassPath(final Class<?> token) throws ImplerException {
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            return Path.of(codeSource.getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Can't create classpath", e);
        }
    }

    /**
     * Gets full implementation name, that is equal to package name
     * concatenated through dot with {@link #getImplementationName(Class)}
     *
     * @param token type token
     * @return full implementation name
     */
    private static String getFullImplementationName(final Class<?> token) {
        return token.getPackageName() + "." + getImplementationName(token);
    }

    /**
     * Gets {@link Path} to the file where the implementation
     * will be located after the {@link #implement(Class, Path)} for given root and {@code Class}.
     * <p>
     * Resulting {@code Path} is equal to absolute path from {@link #getFullImplementationName(Class)}
     * where folders are concatenated through dot, ends with <var>.java</var> extension
     * </p>
     *
     * @param root  root path
     * @param token {@code Class}
     * @return {@code Path} to file with implementation
     */
    private static Path getFileName(final Path root, final Class<?> token) {
        // :NOTE: no need for absolute path
        return root.resolve(getFullImplementationName(token).replace(".", File.separator) + ".java");
    }

    /**
     * Compiles <var>.java</var> code from given {@link Path} for given {@code Class}
     *
     * @param token     type token
     * @param directory {@code Path} to directory
     * @throws ImplerException if can't find Java compiler or can't compile code
     */
    private static void compileCode(final Class<?> token, final Path directory) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Can't find Java compiler");
        }
        final String classPath = getClassPath(token);
        final String fileName = getFileName(directory, token).toString();
        final String[] args = classPath == null
                ? new String[]{fileName}
                : new String[]{"-cp", classPath, fileName};
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Can't compile code");
        }
    }

    /**
     * Creates temporary directory near given {@link Path}
     *
     * @param path {@code Path} to  directory
     * @return {@code Path} of created temporary directory
     * @throws ImplerException if {@link IOException} occurs while creating
     */
    private static Path createTemporaryDirectory(final Path path) throws ImplerException {
        try {
            return Files.createTempDirectory(path.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }
    }

    /**
     * Cleans given directory
     *
     * @param tmpDirectory {@link Path} to directory
     * @throws ImplerException if {@link IOException} occurs
     */
    private static void cleanTemporaryDirectory(final Path tmpDirectory) throws ImplerException {
        try {
            Files.walkFileTree(tmpDirectory, DELETE_VISITOR);
        } catch (IOException e) {
            throw new ImplerException("Can't clean temporary directory", e);
        }
    }

    /**
     * Creates <var>.jar</var> for {@code Class} from given directory to specified file
     *
     * @param token        type token for target {@code Class}
     * @param tmpDirectory {@link Path} to directory
     * @param jarFile      {@code Path} to resulting <var>.jar</var> file
     * @throws ImplerException if an {@link IOException} occurs
     */
    private static void createJar(final Class<?> token, final Path tmpDirectory, final Path jarFile) throws ImplerException {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jarOutputStream.putNextEntry(new ZipEntry(String.format("%s/%s.class",
                    token.getPackageName().replace('.', '/'),
                    getImplementationName(token))));
            Files.copy(resolve(tmpDirectory, token, getClassName(token)), jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("Can't write to jar file", e);
        }
    }

    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Arguments mustn't be null");
        }
        createParentDirectories(jarFile);
        final Path tmpDirectory = createTemporaryDirectory(jarFile);
        try {
            implement(token, tmpDirectory);
            compileCode(token, tmpDirectory);
            createJar(token, tmpDirectory, jarFile);
        } finally {
            cleanTemporaryDirectory(tmpDirectory);
        }
    }
}
