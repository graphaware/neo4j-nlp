package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilsProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.utils.listFiles")
    public Stream<FilePathResult> listFiles(@Name("directory") String directory, @Name(value = "extension", defaultValue = "*") String extensionFilter) {
        try {
            return Files.list(Paths.get(directory))
                    .filter(s -> filter(s, extensionFilter))
                    .map(FilePathResult::new)
                    .collect(Collectors.toList())
                    .stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.utils.walkdir")
    public Stream<FilePathResult> walkdir(@Name("directory") String directory, @Name(value = "extension", defaultValue = "*") String extensionFilter) {
        try {
            return Files.list(Paths.get(directory))
                    .filter(s -> filter(s, extensionFilter))
                    .map(FilePathResult::new)
                    .collect(Collectors.toList())
                    .stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean filter(Path s, String extensionFilter) {
        if (!Files.isRegularFile(s)) {
            return false;
        }

        if (extensionFilter.equalsIgnoreCase("*")) {
            return true;
        } else {
            String f = extensionFilter.startsWith(".") ? extensionFilter : "." + extensionFilter;
            return s.toString().endsWith(f);
        }
    }


    public class FilePathResult {
        public String filePath;

        public FilePathResult(Path path) {
            this.filePath = path.toString();
        }
    }
}
