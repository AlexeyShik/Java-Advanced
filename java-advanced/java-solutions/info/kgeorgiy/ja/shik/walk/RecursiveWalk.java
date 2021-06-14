package info.kgeorgiy.ja.shik.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {

    private static void processFileOrDirectory(Path file, BufferedWriter outputFileWriter) throws IOException {
        Files.walkFileTree(file, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                info.kgeorgiy.ja.shik.walk.Walk.processFile(file, outputFileWriter);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void main(String[] args) {
        info.kgeorgiy.ja.shik.walk.Walk.main(args, RecursiveWalk::processFileOrDirectory);
    }
}