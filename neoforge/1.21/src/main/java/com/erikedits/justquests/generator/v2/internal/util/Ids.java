package com.erikedits.justquests.generator.v2.internal.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Namespaced id helpers. */
public final class Ids {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** Prefix of generated quest ids. */
    public static final String GEN_PREFIX = "justquests:gen/";

    private Ids() {
    }

    /** True for {@code ns:path} with Minecraft's allowed characters. */
    public static boolean isValid(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** True for {@code #ns:path}. */
    public static boolean isValidTag(String tag) {
        return tag != null && tag.startsWith("#") && isValid(tag.substring(1));
    }

    public static String namespace(String id) {
        int i = id.indexOf(':');
        return i < 0 ? "minecraft" : id.substring(0, i);
    }

    public static String path(String id) {
        int i = id.indexOf(':');
        return i < 0 ? id : id.substring(i + 1);
    }

    /** {@code minecraft:raw_iron} → "Raw Iron"; namespace and tag marker dropped. */
    public static String prettify(String id) {
        String p = path(id.startsWith("#") ? id.substring(1) : id);
        int slash = p.lastIndexOf('/');
        if (slash >= 0) {
            p = p.substring(slash + 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String part : p.split("[_.-]+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return sb.length() == 0 ? p : sb.toString();
    }

    public static boolean isGeneratedQuestId(String questId) {
        return questId != null && questId.startsWith(GEN_PREFIX) && questId.length() > GEN_PREFIX.length();
    }
}
