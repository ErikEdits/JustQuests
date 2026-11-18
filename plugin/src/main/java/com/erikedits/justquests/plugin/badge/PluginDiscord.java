package com.erikedits.justquests.plugin.badge;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Badges as roles on the JustQuests Discord (0.6.0), as in the mod (DiscordLink): a player links with the
 * code of /justquests link on Discord (/quest discord link &lt;code&gt;), then their badges go to the bot,
 * which gives a role for each. Nothing is sent before a player links; no token or secret is in the plugin
 * (each link gets its own sync token, kept in plugins/JustQuests/discord-links.json). config.yml:
 * discord.link (off: false) and discord.api (an own bot with the same API).
 */
public final class PluginDiscord {
    public static final String DEFAULT_API = "https://pocketbot-gateway.erikbestemc-da62f9.workers.dev/api/v1";
    public static final String INVITE = "https://discord.gg/tExvseWRE4";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP = client();

    /**
     * The HTTP client. On Windows it trusts Java's own certificates and those of Windows, as browsers do:
     * a virus scanner that checks HTTPS adds its certificate to Windows only, and Java alone would then
     * refuse the bot ("PKIX path building failed").
     */
    private static HttpClient client() {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15));
        try {
            if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) b.sslContext(trustJavaAndWindows());
        } catch (Exception e) {
            // Java's own certificates only
        }
        return b.build();
    }

    private static javax.net.ssl.SSLContext trustJavaAndWindows() throws Exception {
        javax.net.ssl.TrustManagerFactory own = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        own.init((java.security.KeyStore) null);
        java.security.KeyStore winStore = java.security.KeyStore.getInstance("Windows-ROOT");
        winStore.load(null, null);
        javax.net.ssl.TrustManagerFactory win = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
        win.init(winStore);
        javax.net.ssl.X509TrustManager a = x509(own), w = x509(win);
        javax.net.ssl.X509TrustManager both = new javax.net.ssl.X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String auth) throws java.security.cert.CertificateException {
                a.checkClientTrusted(chain, auth);
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String auth) throws java.security.cert.CertificateException {
                try {
                    a.checkServerTrusted(chain, auth);
                } catch (java.security.cert.CertificateException e) {
                    w.checkServerTrusted(chain, auth);
                }
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                java.security.cert.X509Certificate[] x = a.getAcceptedIssuers(), y = w.getAcceptedIssuers();
                java.security.cert.X509Certificate[] all = java.util.Arrays.copyOf(x, x.length + y.length);
                System.arraycopy(y, 0, all, x.length, y.length);
                return all;
            }
        };
        javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("TLS");
        ctx.init(null, new javax.net.ssl.TrustManager[] {both}, null);
        return ctx;
    }

    private static javax.net.ssl.X509TrustManager x509(javax.net.ssl.TrustManagerFactory f) {
        for (javax.net.ssl.TrustManager t : f.getTrustManagers()) if (t instanceof javax.net.ssl.X509TrustManager x) return x;
        throw new IllegalStateException("no X509TrustManager");
    }
    private static final int PER_MINUTE = 20;
    private static final long RESEND = 24L * 3600_000L;

    private final JustQuestsPlugin plugin;
    private final Path linksFile, requestsFile;
    private JsonObject links, requests;
    private final JsonObject info = new JsonObject();
    private final Map<UUID, Long> due = new LinkedHashMap<>();
    private final Map<UUID, Integer> failures = new HashMap<>();
    private long pauseUntil, minuteStart;
    private int sentThisMinute;
    private String invite = INVITE, privacy = DEFAULT_API + "/privacy";

    public PluginDiscord(JustQuestsPlugin plugin) {
        this.plugin = plugin;
        Path dir = plugin.getDataFolder().toPath();
        linksFile = dir.resolve("discord-links.json");
        requestsFile = dir.resolve("discord-requests.json");
        links = read(linksFile);
        requests = read(requestsFile);
        info.addProperty("mod_version", plugin.getDescription().getVersion());
        info.addProperty("loader", "plugin");
        info.addProperty("mc_version", Bukkit.getBukkitVersion());
    }

    private boolean enabled() {
        return plugin.settings().discordLink;
    }

    private String api() {
        String a = plugin.settings().discordApi;
        return a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
    }

    /** After the start: invite and privacy links, and the badge requests of quest packs. */
    public void start() {
        // nothing goes to the bot at the start; only pack badges that asked for it (see requestBadges)
        if (enabled()) requestBadges();
    }

    private boolean healthAsked;

    /** The bot's invite and privacy links, asked once when a player first uses /quest discord. */
    private void health() {
        if (healthAsked || !enabled()) return;
        healthAsked = true;
        get("/health", (status, json) -> {
            if (status != 200) return;
            if (!str(json, "invite").isEmpty()) invite = str(json, "invite");
            if (!str(json, "privacy").isEmpty()) privacy = str(json, "privacy");
        });
    }

    /** Once a second (main thread). */
    public void tick() {
        if (!enabled() || due.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now < pauseUntil) return;
        if (now - minuteStart > 60_000) {
            minuteStart = now;
            sentThisMinute = 0;
        }
        for (Iterator<Map.Entry<UUID, Long>> it = due.entrySet().iterator(); it.hasNext() && sentThisMinute < PER_MINUTE; ) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() > now) continue;
            it.remove();
            sentThisMinute++;
            sendBadges(e.getKey());
        }
    }

    public void joined(Player player) {
        JsonObject l = link(player.getUniqueId());
        if (l == null || !enabled()) return;
        long synced = l.has("synced") ? l.get("synced").getAsLong() : 0;
        if (System.currentTimeMillis() - synced > RESEND) schedule(player.getUniqueId(), 10_000);
    }

    public void badgesChanged(Player player) {
        if (link(player.getUniqueId()) != null && enabled()) schedule(player.getUniqueId(), 5_000);
    }

    private void schedule(UUID id, long in) {
        due.merge(id, System.currentTimeMillis() + in, Math::min);
    }

    // ---- commands

    public void link(Player player, String code) {
        health();
        String lang = Lang.of(player);
        if (!enabled()) {
            say(player, Text.tr(lang, "justquests.discord.off"));
            return;
        }
        String clean = code.trim();
        if (clean.length() < 4 || clean.length() > 32) {
            say(player, Text.tr(lang, "justquests.discord.code_invalid", url(invite), "/justquests link"));
            return;
        }
        say(player, Text.tr(lang, "justquests.discord.sending", url(privacy)));
        UUID id = player.getUniqueId();
        String name = player.getName();
        JsonObject body = new JsonObject();
        body.addProperty("code", clean);
        body.addProperty("minecraft_uuid", id.toString());
        body.addProperty("minecraft_name", name);
        body.add("server", info);
        post("/link/confirm", body, 4096, (status, json) -> {
            Player p = Bukkit.getPlayer(id);
            if (status == 200 && json.has("sync_token")) {
                JsonObject l = new JsonObject();
                l.addProperty("token", json.get("sync_token").getAsString());
                l.addProperty("user", json.has("discord_user") ? json.get("discord_user").getAsString() : "?");
                l.addProperty("name", name);
                l.addProperty("since", System.currentTimeMillis());
                links.add(id.toString(), l);
                save(linksFile, links, true);
                if (p != null) say(p, Text.tr(Lang.of(p), "justquests.discord.linked", l.get("user").getAsString()));
                schedule(id, 0);
            } else if (p != null) {
                say(p, error(Lang.of(p), status, json));
            }
        });
    }

    public void unlink(Player player) {
        String lang = Lang.of(player);
        JsonObject l = link(player.getUniqueId());
        if (l == null) {
            say(player, Text.tr(lang, "justquests.discord.not_linked", url(invite), "/justquests link"));
            return;
        }
        links.remove(player.getUniqueId().toString());
        save(linksFile, links, true);
        due.remove(player.getUniqueId());
        say(player, Text.tr(lang, "justquests.discord.unlinked"));
        if (!enabled()) return;
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", player.getUniqueId().toString());
        body.addProperty("sync_token", l.get("token").getAsString());
        UUID id = player.getUniqueId();
        post("/unlink", body, 4096, (status, json) -> {
            Player p = Bukkit.getPlayer(id);
            if (status != 200 && status != 401 && p != null) say(p, Text.tr(Lang.of(p), "justquests.discord.unlink_failed", "/justquests unlink"));
        });
    }

    public BaseComponent status(Player player) {
        health();
        String lang = Lang.of(player);
        if (!enabled()) return Text.tr(lang, "justquests.discord.off");
        JsonObject l = link(player.getUniqueId());
        if (l == null) return Text.tr(lang, "justquests.discord.not_linked", url(invite), "/justquests link");
        return Text.tr(lang, "justquests.discord.status", l.get("user").getAsString(), "/quest discord unlink");
    }

    /** The linked Discord user, or null. */
    public String user(UUID player) {
        JsonObject l = link(player);
        return l == null ? null : l.get("user").getAsString();
    }

    private JsonObject link(UUID player) {
        JsonElement l = links.get(player.toString());
        return l != null && l.isJsonObject() && l.getAsJsonObject().has("token") ? l.getAsJsonObject() : null;
    }

    private static BaseComponent url(String url) {
        return Text.link(Text.lit("§9§n" + url), url, Text.lit(url));
    }

    private static void say(Player p, BaseComponent c) {
        p.spigot().sendMessage(c);
    }

    // ---- badges to the bot

    private void sendBadges(UUID id) {
        JsonObject l = link(id);
        if (l == null) return;
        PlayerData data = plugin.store().peek(id);
        JsonArray list = new JsonArray();
        if (data != null) {
            for (String b : data.badges.keySet()) {
                PluginBadges.Badge badge = plugin.badges().badge(b);
                if (badge != null && badge.discord()) list.add(b);
            }
        }
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", id.toString());
        body.addProperty("sync_token", l.get("token").getAsString());
        body.add("badges", list);
        post("/badges", body, 4096, (status, json) -> {
            Player p = Bukkit.getPlayer(id);
            if (status == 200) {
                failures.remove(id);
                l.addProperty("synced", System.currentTimeMillis());
                save(linksFile, links, true);
                if (p != null && json.has("roles_added") && json.getAsJsonArray("roles_added").size() > 0) {
                    StringBuilder roles = new StringBuilder();
                    for (JsonElement r : json.getAsJsonArray("roles_added")) roles.append(roles.length() > 0 ? ", " : "").append(r.getAsString());
                    say(p, Text.tr(Lang.of(p), "justquests.discord.roles", roles.toString()));
                }
                if (p != null && json.has("not_in_guild") && json.get("not_in_guild").getAsBoolean()) {
                    say(p, Text.tr(Lang.of(p), "justquests.discord.join", url(invite)));
                }
            } else if (status == 401) {
                links.remove(id.toString());
                save(linksFile, links, true);
                if (p != null) say(p, Text.tr(Lang.of(p), "justquests.discord.token_gone", "/justquests link"));
            } else if (status == 429) {
                schedule(id, 0);
            } else {
                int n = failures.merge(id, 1, Integer::sum);
                if (n <= 6) schedule(id, Math.min(30L * 60_000L, 30_000L << (n - 1)));
            }
        });
    }

    /** Sends the badges of quest packs that want to be roles to the bot once (its owner approves them). */
    public void requestBadges() {
        if (!enabled()) return;
        for (Map.Entry<String, JsonObject> e : plugin.badges().packs().entrySet()) {
            JsonObject pack = e.getValue().deepCopy();
            String ns = e.getKey().equals("custom") ? str(pack, "namespace") : e.getKey();
            if (ns.isEmpty() || ns.equals("justquests") || ns.equals("minecraft")) continue;
            pack.addProperty("namespace", ns);
            JsonArray badges = new JsonArray();
            for (PluginBadges.Badge b : plugin.badges().sorted()) {
                if (!b.id().startsWith(ns + ":") || !b.discord()) continue;
                JsonObject o = new JsonObject();
                o.addProperty("id", b.id());
                o.addProperty("name", b.name().getDefault());
                o.addProperty("color", b.color());
                o.addProperty("description", b.description().getDefault());
                o.addProperty("how", b.how().getDefault());
                badges.add(o);
                if (badges.size() == 20) break;
            }
            if (badges.isEmpty()) continue;
            JsonObject body = new JsonObject();
            body.add("pack", pack);
            body.add("badges", badges);
            body.add("server", info);
            String hash = sha(GSON.toJson(pack) + GSON.toJson(badges));
            JsonObject last = requests.has(ns) && requests.get(ns).isJsonObject() ? requests.getAsJsonObject(ns) : null;
            if (last != null && hash.equals(str(last, "hash")) && !str(last, "statuses").contains("pending")) continue;
            post("/badges/request", body, 16384, (status, json) -> {
                JsonObject r = new JsonObject();
                r.addProperty("hash", hash);
                if (status == 202) {
                    StringBuilder st = new StringBuilder();
                    for (JsonElement b : json.getAsJsonArray("badges")) {
                        JsonObject o = b.getAsJsonObject();
                        st.append(str(o, "id")).append(" ").append(str(o, "status"))
                            .append(o.has("reason") ? " (" + str(o, "reason") + ")" : "").append("; ");
                    }
                    r.addProperty("statuses", st.toString());
                    plugin.getLogger().info("JustQuests Discord: badges of '" + ns + "': " + st);
                    String code = str(json, "verify_code");
                    if (!code.isEmpty()) {
                        r.addProperty("verify_code", code);
                        plugin.getLogger().info("JustQuests Discord: put " + code + " in your project description or README ("
                            + str(pack, "url") + ") so the Discord owner sees the request for the badges of '" + ns + "' really comes from the pack.");
                    }
                } else if (status == 409) {
                    r.addProperty("statuses", "namespace_taken");
                    plugin.getLogger().warning("JustQuests Discord: the namespace '" + ns + "' belongs to another pack on the JustQuests Discord. "
                        + "Use a different namespace for your badges.");
                } else {
                    r.addProperty("statuses", "error " + status + ": " + str(json, "message"));
                    plugin.getLogger().warning("JustQuests Discord: the badges of '" + ns + "' could not be sent (" + status + " "
                        + str(json, "field") + " " + str(json, "message") + ")");
                }
                requests.add(ns, r);
                save(requestsFile, requests, false);
            });
        }
    }

    public Map<String, String> requestStatus() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : requests.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject r = e.getValue().getAsJsonObject();
            out.put(e.getKey(), str(r, "statuses") + (r.has("verify_code") ? " verify code: " + str(r, "verify_code") : ""));
        }
        return out;
    }

    // ---- HTTP

    private interface Answer {
        void accept(int status, JsonObject json);
    }

    private void post(String path, JsonObject body, int maxBytes, Answer answer) {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) {
            plugin.getLogger().warning("JustQuests Discord: request to " + path + " too big (" + bytes.length + " bytes)");
            return;
        }
        send(HttpRequest.newBuilder(URI.create(api() + path)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .header("Content-Type", "application/json"), answer);
    }

    private void get(String path, Answer answer) {
        send(HttpRequest.newBuilder(URI.create(api() + path)).GET(), answer);
    }

    private void send(HttpRequest.Builder request, Answer answer) {
        HttpRequest r;
        try {
            r = request.timeout(Duration.ofSeconds(30))
                .header("User-Agent", "JustQuests/" + str(info, "mod_version") + " (plugin " + str(info, "mc_version") + ")").build();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("JustQuests Discord: bad discord.api setting: " + e.getMessage());
            return;
        }
        HTTP.sendAsync(r, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((resp, err) -> {
            int status = err != null ? 0 : resp.statusCode();
            JsonObject json = new JsonObject();
            if (resp != null) {
                try {
                    JsonElement j = JsonParser.parseString(resp.body());
                    if (j.isJsonObject()) json = j.getAsJsonObject();
                } catch (Exception ignored) {
                }
                if (status == 429) {
                    long wait = resp.headers().firstValue("Retry-After").map(v -> {
                        try {
                            return Long.parseLong(v.trim()) * 1000L;
                        } catch (NumberFormatException e) {
                            return 60_000L;
                        }
                    }).orElse(60_000L);
                    pauseUntil = Math.max(pauseUntil, System.currentTimeMillis() + wait);
                }
            }
            JsonObject result = json;
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> answer.accept(status, result));
        });
    }

    private static BaseComponent error(String lang, int status, JsonObject json) {
        String code = str(json, "error");
        if (status == 404) return Text.tr(lang, "justquests.discord.code_invalid", url(INVITE), "/justquests link");
        if (status == 429) return Text.tr(lang, "justquests.discord.too_many");
        if (status == 503 && "unavailable".equals(code)) return Text.tr(lang, "justquests.discord.paused");
        if (status == 503 || status == 504 || status == 0) return Text.tr(lang, "justquests.discord.offline");
        return Text.tr(lang, "justquests.discord.failed", status + (code.isEmpty() ? "" : " " + code));
    }

    // ---- files

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private JsonObject read(Path file) {
        try {
            if (Files.exists(file)) {
                JsonElement j = JsonParser.parseString(Files.readString(file));
                if (j.isJsonObject()) return j.getAsJsonObject();
            }
        } catch (Exception e) {
            com.erikedits.justquests.plugin.data.SafeFiles.keepBroken(file, e.getMessage(), plugin.getLogger());
        }
        return new JsonObject();
    }

    private void save(Path file, JsonObject json, boolean isLinks) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject out = json.deepCopy();
            out.addProperty("_help", isLinks
                ? "JustQuests: links of players to the JustQuests Discord (their badges become roles there). "
                    + "A player removes their own with /quest discord unlink. Keep this file private: it holds each link's token."
                : "JustQuests: what the JustQuests Discord said about the badges of your quest packs (asked again when they change).");
            com.erikedits.justquests.plugin.data.SafeFiles.write(file, GSON.toJson(out));
        } catch (Exception e) {
            plugin.getLogger().warning("Could not write " + file + ": " + e.getMessage());
        }
    }
}
