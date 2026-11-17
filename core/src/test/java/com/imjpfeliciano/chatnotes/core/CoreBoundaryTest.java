package com.imjpfeliciano.chatnotes.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Core is plain Java, and the 1.21 screen layer must not import a loader. */
class CoreBoundaryTest {

    @Test
    void coreDoesNotImportMinecraftOrFabric() throws IOException {
        assertNoImports(Path.of("src/main/java"), "import net.minecraft", "import net.fabricmc");
    }

    @Test
    void mc121DoesNotImportFabric() throws IOException {
        assertNoImports(Path.of("../shared/mc121/src/main/java"), "import net.fabricmc");
    }

    @Test
    void mc26DoesNotImportFabric() throws IOException {
        assertNoImports(Path.of("../shared/mc26/src/main/java"), "import net.fabricmc");
    }

    private static void assertNoImports(Path root, String... banned) throws IOException {
        assertTrue(Files.isDirectory(root), () -> "missing " + root.toAbsolutePath());
        List<String> offenders = new ArrayList<>();
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    for (String prefix : banned) {
                        if (source.contains(prefix)) {
                            offenders.add(path + " contains " + prefix);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertTrue(offenders.isEmpty(), () -> String.join("\n", offenders));
    }
}
