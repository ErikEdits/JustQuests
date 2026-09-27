package com.erikedits.justquests.generator.v2;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.1 — the core uses only the JDK and the Gson 2.8.8 API. */
class PurityTest {
    private static final Pattern FORBIDDEN = Pattern.compile(
        "\\b(net\\.minecraft|com\\.mojang|net\\.neoforged|net\\.minecraftforge|net\\.fabricmc|com\\.google\\.common"
            + "|org\\.slf4j|org\\.apache\\.logging|it\\.unimi\\.dsi)\\.");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(static\\s+)?([\\w.]+)", Pattern.MULTILINE);
    private static final Pattern NEWER_GSON = Pattern.compile("\\.asList\\(\\)|\\.asMap\\(\\)|JsonParser\\.parseReader\\("
        + "|getAsJsonPrimitive\\(\\)\\.isJsonNull");
    private static final Pattern NO_GO = Pattern.compile("System\\.exit|new Thread\\b|java\\.util\\.Timer|Executors\\."
        + "|java\\.net\\.|java\\.nio\\.file|java\\.io\\.File\\b|Class\\.forName|getDeclaredField|setAccessible"
        + "|\\bswitch\\s*\\([^)]*\\)\\s*\\{[^}]*\\bcase\\s+[A-Z]\\w*\\s+\\w+\\s*->");

    private static List<Path> mainSources() throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java"))) {
            s.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
        }
        return out;
    }

    @Test
    void noForbiddenPackages() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path p : mainSources()) {
            String text = Files.readString(p);
            Matcher m = FORBIDDEN.matcher(text);
            while (m.find()) {
                problems.add(p + ": " + m.group());
            }
            Matcher imp = IMPORT.matcher(text);
            while (imp.find()) {
                String pkg = imp.group(2);
                if (!pkg.startsWith("java.") && !pkg.startsWith("com.google.gson.")
                    && !pkg.startsWith("com.erikedits.justquests.generator.v2")) {
                    problems.add(p + ": import " + pkg);
                }
            }
        }
        assertTrue(problems.isEmpty(), "forbidden references: " + problems);
    }

    @Test
    void noNewerGsonApiThreadsIoOrReflection() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path p : mainSources()) {
            String text = Files.readString(p);
            Matcher m = NEWER_GSON.matcher(text);
            while (m.find()) {
                problems.add(p + ": " + m.group());
            }
            Matcher n = NO_GO.matcher(text);
            while (n.find()) {
                problems.add(p + ": " + n.group());
            }
        }
        assertTrue(problems.isEmpty(), "forbidden API use: " + problems);
    }

    @Test
    void noStaticMutableState() throws IOException {
        Pattern staticField = Pattern.compile("^\\s*(private|public|protected)?\\s*static\\s+(?!final)[\\w<>,\\[\\] ]+\\s+\\w+\\s*[=;]",
            Pattern.MULTILINE);
        List<String> problems = new ArrayList<>();
        for (Path p : mainSources()) {
            Matcher m = staticField.matcher(Files.readString(p));
            while (m.find()) {
                problems.add(p + ": " + m.group().trim());
            }
        }
        assertTrue(problems.isEmpty(), "non-final static fields: " + problems);
    }

    @Test
    void compiledForJava17() throws IOException {
        try (InputStream in = QuestGeneratorV2.class.getResourceAsStream("QuestGeneratorV2.class")) {
            byte[] head = in.readNBytes(8);
            int major = ((head[6] & 0xFF) << 8) | (head[7] & 0xFF);
            assertEquals(61, major, "class file major version (61 = Java 17)");
        }
    }
}
