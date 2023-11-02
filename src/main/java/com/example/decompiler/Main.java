package com.example.decompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final BiConsumer<Path, Path> DECOMPILER = (rootPath, path) -> {
        String classFile = path.toFile().getAbsolutePath();
        Path newPath = FileExplorer.toDecompilePath(rootPath, path);
        File decompiledFile = newPath.toFile();
        try {
            int i = decompiledFile.getName().lastIndexOf('.');
            String name = decompiledFile.getName().substring(0,i);
            File java = new File(decompiledFile.getParent(), name + ".java");
            if (!java.exists()) {
                decompiledFile.getParentFile().mkdirs();
                java.createNewFile();
                try (FileOutputStream stream = new FileOutputStream(java);
                        OutputStreamWriter writer = new OutputStreamWriter(stream);) {
                    Decompiler.decompile(classFile, new PlainTextOutput(writer));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot decompile " + classFile, e);
        }
    };

    public static void main(String[] args) throws IOException {
        Path rootPath = Paths.get(args[0]);
        LOGGER.info("Explore in " + rootPath);
        FileExplorer.explore(rootPath, Arrays.asList(DECOMPILER));
    }

}
