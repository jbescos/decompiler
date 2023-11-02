package com.example.decompiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileExplorer {

    private static final Logger LOGGER = Logger.getLogger(FileExplorer.class.getName());
    private static final PathMatcher CLASS_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.class");
    private static final PathMatcher ZIP_MATCHER = FileSystems.getDefault().getPathMatcher("glob:*.{zip,jar,war,ear,tar}");
    static final Path DECOMPILED_DIR = Paths.get("./decompiled");

    public static void explore(Path rootPath, List<BiConsumer<Path, Path>> consumers) throws IOException {
        try {
            Files.walk(rootPath).parallel().forEach(path -> {
                LOGGER.fine("In file: " + path.toString());
                if (!Files.isDirectory(path)) {
                    Path fileName = path.getFileName();
                    if (ZIP_MATCHER.matches(fileName)) {
                        LOGGER.info(path + " ZIP matched");
                        checkZip(rootPath, path, consumers);
                    } else if (CLASS_MATCHER.matches(fileName)) {
                        LOGGER.fine(path + " CLASS matched");
                        consumers.forEach(c -> {
                            c.accept(rootPath, path);
                        });
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot iterate " + rootPath, e);
        }
    }
    
    private static void checkZip(Path rootPath, Path zipFile, List<BiConsumer<Path, Path>> consumers) {
        Path decompiledPath = toDecompilePath(rootPath, zipFile);
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream inputStream = zip.getInputStream(entry)) {
                        Path classPath = decompiledPath.resolve(entry.getName());
                        File classFile = classPath.toFile();
                        classFile.getParentFile().mkdirs();
                        classFile.createNewFile();
                        Files.copy(inputStream, classPath, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.fine(classPath + " CLASS matched");
                        consumers.forEach(c -> {
                            c.accept(rootPath, classPath);
                        });
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot check zip " + zipFile, e);
        }
    }
    
    public static Path toDecompilePath(Path rootPath, Path filePath) {
        if (filePath.startsWith(FileExplorer.DECOMPILED_DIR)) {
            return filePath;
        } else {
            Path rootRemoved = rootPath.relativize(filePath);
            Path newPath = FileExplorer.DECOMPILED_DIR.resolve(rootRemoved);
            return newPath;
        }
    }

}
