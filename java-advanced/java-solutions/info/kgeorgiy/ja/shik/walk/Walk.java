package info.kgeorgiy.ja.shik.walk;

import java.io.*;
import java.nio.file.*;

public class Walk {
    private static final int BUFFER_SIZE = 4096;
    private static final long HIGH_BITS_MASK = 0xff00_0000_0000_0000L;
    private static final int LOW_BITS_MASK = 0xff;
    private static final int BITS = 64;

    private static void printError(String message, Exception e) {
        System.err.printf("%s: %s%n", message, e.getMessage());
    }

    private static long PJWHash(Path file) {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = Files.newInputStream(file)) {
            long h = 0;
            int size;
            while ((size = inputStream.read(buffer)) >= 0) {
                for (int i = 0; i < size; i++) {
                    h = (h << 8) + (buffer[i] & LOW_BITS_MASK);
                    final long high = h & HIGH_BITS_MASK;
                    if (high != 0) {
                        h ^= high >> (3 * BITS / 4);
                        h &= ~high;
                    }
                }
            }
            return h;
        } catch (IOException e) {
            return 0;
        }
    }

    private static void printHash(String file, BufferedWriter outputFileWriter, long hash) throws IOException {
        outputFileWriter.write(String.format("%016x %s%n", hash, file));
    }

    static void processFile(Path file, BufferedWriter outputFileWriter) throws IOException {
        printHash(file.toString(), outputFileWriter, PJWHash(file));
    }

    private static void createParentDirectory(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static boolean checkArgs(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong arguments format: should be 2 non-null arguments");
            return false;
        }
        try {
            Path.of(args[0]);
            createParentDirectory(Path.of(args[1]));
        } catch (FileAlreadyExistsException e) {
            printError("File exists, but is not a directory", e);
            return false;
        } catch (InvalidPathException e) {
            printError("Invalid path", e);
            return false;
        } catch (IOException e) {
            return true;
        }
        return true;
    }

    static void main(String[] args, info.kgeorgiy.ja.shik.walk.BiWalkConsumer<Path, BufferedWriter> process) {
        if (!checkArgs(args)) {
            return;
        }
        try (BufferedReader inputFileReader = Files.newBufferedReader(Path.of(args[0]))) {
            try (BufferedWriter outputFileWriter = Files.newBufferedWriter(Path.of(args[1]))) {
                try {
                    String filename;
                    while ((filename = inputFileReader.readLine()) != null) {
                        try {
                            process.accept(Path.of(filename), outputFileWriter);
                        } catch (InvalidPathException | IOException e) {
                            printHash(filename, outputFileWriter, 0);
                        }
                    }
                } catch (IOException e) {
                    printError("Exception while reading input file", e);
                }
            } catch (IOException e) {
                printError("Exception with output file", e);
            }
        } catch (IOException e) {
            printError("Exception with input file", e);
        }
    }

    public static void main(String[] args) {
        main(args, Walk::processFile);
    }
}