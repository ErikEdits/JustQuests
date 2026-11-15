package com.erikedits.justquests.discord;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.badge.Badges;
import com.erikedits.justquests.data.Badge;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import com.erikedits.justquests.text.Msg;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
 * Badges as roles on the JustQuests Discord (0.6.0). A player links with a code the Discord bot gives
 * them (/justquests link on Discord, then /quest discord link &lt;code&gt; here); from then on their badges
 * are sent to the bot, which gives them a role (with no rights) for each.
 * <ul>
 * <li>Nothing leaves the game before a player links, and only their Minecraft name, UUID and badges then.
 *     No token or secret is in the mod: the bot hands each link its own sync token, which is kept in
 *     config/justquests/discord-links.json of this game or server (so a link holds for all its worlds,
 *     singleplayer included).</li>
 * <li>A server owner can turn it off ("discordLink": false) or point it at an own bot with the same API
 *     ("discordApi").</li>
 * <li>Badges of a datapack (or custom-quests.json) with "discord": true and a pack.json are sent to the
 *     bot once as a request; its owner approves them before they become roles.</li>
 * </ul>
 */
public final class DiscordLink {
    public static final String DEFAULT_API = "https://pocketbot-gateway.erikbestemc-da62f9.workers.dev/api/v1";
    public static final String INVITE = "https://discord.gg/tExvseWRE4";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    /** At most this many badge syncs a minute (all players of a server share its IP at the bot). */
    private static final int PER_MINUTE = 20;
    /** A linked player's badges are sent again at most once a day when they join. */
    private static final long RESEND = 24L * 3600_000L;

    private static MinecraftServer server;
    private static JsonObject info = new JsonObject();
    private static Path linksFile, requestsFile;
    /** uuid -> {token, user, since, synced} */
    private static JsonObject links = new JsonObject();
    /** namespace -> {hash, statuses, verify_code, message} of the last badge request */
    private static JsonObject requests = new JsonObject();
    /** players whose badges are to be sent -> not before this time */
    private static final Map<UUID, Long> due = new LinkedHashMap<>();
    private static final Map<UUID, Integer> failures = new HashMap<>();
    private static long pauseUntil, minuteStart;
    private static int sentThisMinute;
    private static String invite = INVITE, privacy = DEFAULT_API + "/privacy";

    private DiscordLink() {}

    // ---- life cycle

    /** Server started: reads the links and asks the bot about badges waiting for approval. */
    public static void init(MinecraftServer s, String loader, String modVersion) {
        server = s;
        info = new JsonObject();
        info.addProperty("mod_version", modVersion);
        info.addProperty("loader", loader);
        info.addProperty("mc_version", s.getServerVersion());
        Path dir = WorldSettings.configDir().resolve("justquests");
        linksFile = dir.resolve("discord-links.json");
        requestsFile = dir.resolve("discord-requests.json");
        links = read(linksFile);
        requests = read(requestsFile);
        due.clear();
        failures.clear();
        pauseUntil = 0;
        if (enabled()) {
            health();
            requestBadges();
        }
    }

    public static void stop() {
        server = null;
        due.clear();
    }

    private static boolean enabled() {
        return WorldSettings.discordLink() && server != null;
    }

    private static String api() {
        String a = WorldSettings.discordApi();
        return a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
    }

    /** Once a second: sends the badges that are due, within the rate limit. */
    public static void tick() {
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

    /** A player joined: a linked one's badges go again if they weren't sent for a day. */
    public static void joined(ServerPlayer player) {
        JsonObject link = link(player.getUUID());
        if (link == null || !enabled()) return;
        long synced = link.has("synced") ? link.get("synced").getAsLong() : 0;
        if (System.currentTimeMillis() - synced > RESEND) schedule(player.getUUID(), 10_000);
    }

    /** The player's badges changed: a linked player's are sent soon (several changes at once go as one). */
    public static void badgesChanged(ServerPlayer player) {
        if (link(player.getUUID()) != null && enabled()) schedule(player.getUUID(), 5_000);
    }

    private static void schedule(UUID player, long inMillis) {
        due.merge(player, System.currentTimeMillis() + inMillis, Math::min);
    }

    // ---- commands

    /** /quest discord link &lt;code&gt; */
    public static void link(ServerPlayer player, String code, Consumer<Component> reply) {
        if (!enabled()) {
            reply.accept(Msg.tr("justquests.discord.off"));
            return;
        }
        String clean = code.trim();
        if (clean.length() < 4 || clean.length() > 32) {
            reply.accept(Msg.tr("justquests.discord.code_invalid", invite(), "/justquests link"));
            return;
        }
        reply.accept(Msg.tr("justquests.discord.sending", link(privacy)));
        JsonObject body = new JsonObject();
        body.addProperty("code", clean);
        body.addProperty("minecraft_uuid", player.getUUID().toString());
        body.addProperty("minecraft_name", player.getName().getString());
        body.add("server", info);
        UUID id = player.getUUID();
        String name = player.getName().getString();
        post("/link/confirm", body, 4096, (status, json) -> {
            ServerPlayer p = online(id);
            Consumer<Component> tell = c -> { if (p != null) p.displayClientMessage(c, false); };
            if (status == 200 && json.has("sync_token")) {
                JsonObject l = new JsonObject();
                l.addProperty("token", json.get("sync_token").getAsString());
                l.addProperty("user", json.has("discord_user") ? json.get("discord_user").getAsString() : "?");
                l.addProperty("name", name);
                l.addProperty("since", System.currentTimeMillis());
                links.add(id.toString(), l);
                save(linksFile, links);
                tell.accept(Msg.tr("justquests.discord.linked", l.get("user").getAsString()));
                // the roles at once, for the badges the player has already
                schedule(id, 0);
            } else {
                tell.accept(error(status, json, true));
            }
        });
    }

    /** /quest discord unlink */
    public static void unlink(ServerPlayer player, Consumer<Component> reply) {
        JsonObject l = link(player.getUUID());
        if (l == null) {
            reply.accept(Msg.tr("justquests.discord.not_linked", invite(), "/justquests link"));
            return;
        }
        links.remove(player.getUUID().toString());
        save(linksFile, links);
        due.remove(player.getUUID());
        reply.accept(Msg.tr("justquests.discord.unlinked"));
        if (!enabled()) return;
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", player.getUUID().toString());
        body.addProperty("sync_token", l.get("token").getAsString());
        UUID id = player.getUUID();
        post("/unlink", body, 4096, (status, json) -> {
            if (status != 200 && status != 401) {
                ServerPlayer p = online(id);
                if (p != null) p.displayClientMessage(Msg.tr("justquests.discord.unlink_failed", "/justquests unlink"), false);
            }
        });
    }

    /** /quest discord status */
    public static Component status(ServerPlayer player) {
        if (!WorldSettings.discordLink()) return Msg.tr("justquests.discord.off");
        JsonObject l = link(player.getUUID());
        if (l == null) return Msg.tr("justquests.discord.not_linked", invite(), "/justquests link");
        return Msg.tr("justquests.discord.status", l.get("user").getAsString(), "/quest discord unlink");
    }

    /** For the quest book's badge page. */
    public static JsonObject clientJson(ServerPlayer player) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", WorldSettings.discordLink());
        JsonObject l = link(player.getUUID());
        if (l != null) o.addProperty("user", l.get("user").getAsString());
        o.addProperty("invite", invite);
        return o;
    }

    private static JsonObject link(UUID player) {
        JsonElement l = links.get(player.toString());
        return l != null && l.isJsonObject() && l.getAsJsonObject().has("token") ? l.getAsJsonObject() : null;
    }

    private static ServerPlayer online(UUID id) {
        return server == null ? null : server.getPlayerList().getPlayer(id);
    }

    private static MutableComponent invite() {
        return link(invite);
    }

    private static MutableComponent link(String url) {
        return Msg.url(url);
    }

    // ---- badges to the bot

    /** The badges of this world that may become roles (the bot ignores ids it doesn't know). */
    private static JsonArray roleBadges(PlayerQuestData data) {
        JsonArray out = new JsonArray();
        if (data == null) return out;
        for (ResourceLocation id : data.badges.keySet()) {
            Badge b = Badges.get(id);
            if (b != null && b.discord()) out.add(id.toString());
        }
        return out;
    }

    private static void sendBadges(UUID id) {
        JsonObject l = link(id);
        WorldQuestStore store = WorldQuestStore.get();
        if (l == null || store == null) return;
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", id.toString());
        body.addProperty("sync_token", l.get("token").getAsString());
        body.add("badges", roleBadges(store.peek(id)));
        post("/badges", body, 4096, (status, json) -> {
            ServerPlayer p = online(id);
            if (status == 200) {
                failures.remove(id);
                l.addProperty("synced", System.currentTimeMillis());
                save(linksFile, links);
                if (p != null && json.has("roles_added") && json.getAsJsonArray("roles_added").size() > 0) {
                    p.displayClientMessage(Msg.tr("justquests.discord.roles", String.join(", ", strings(json.getAsJsonArray("roles_added")))), false);
                }
                if (p != null && json.has("not_in_guild") && json.get("not_in_guild").getAsBoolean()) {
                    p.displayClientMessage(Msg.tr("justquests.discord.join", invite()), false);
                }
            } else if (status == 401) {
                // unlinked on Discord, or linked again from another account
                links.remove(id.toString());
                save(linksFile, links);
                if (p != null) p.displayClientMessage(Msg.tr("justquests.discord.token_gone", "/justquests link"), false);
            } else if (status == 429) {
                schedule(id, 0);
            } else {
                // offline, timeout or a network error: again later, waiting longer each time (at most 6 times)
                int n = failures.merge(id, 1, Integer::sum);
                if (n <= 6) schedule(id, Math.min(30L * 60_000L, 30_000L << (n - 1)));
            }
        });
    }

    private static String[] strings(JsonArray a) {
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsString();
        return out;
    }

    /** What the bot says about the badges of datapacks and custom-quests.json that want to be roles. */
    public static void requestBadges() {
        if (!enabled()) return;
        for (Map.Entry<String, JsonObject> e : Badges.packs().entrySet()) {
            String key = e.getKey();
            JsonObject pack = e.getValue().deepCopy();
            String ns = key.equals("custom")
                ? (pack.has("namespace") ? pack.get("namespace").getAsString() : "")
                : key;
            if (ns.isEmpty() || ns.equals(JustQuests.MOD_ID) || ns.equals("minecraft")) continue;
            pack.addProperty("namespace", ns);
            JsonArray badges = new JsonArray();
            for (Map.Entry<ResourceLocation, Badge> b : Badges.sorted()) {
                if (!b.getKey().getNamespace().equals(ns) || !b.getValue().discord()) continue;
                JsonObject o = new JsonObject();
                o.addProperty("id", b.getKey().toString());
                o.addProperty("name", b.getValue().name().getDefault());
                o.addProperty("color", b.getValue().color());
                o.addProperty("description", b.getValue().description().getDefault());
                o.addProperty("how", b.getValue().how().getDefault());
                badges.add(o);
                if (badges.size() == 20) break;
            }
            if (badges.isEmpty()) continue;
            JsonObject body = new JsonObject();
            body.add("pack", pack);
            body.add("badges", badges);
            body.add("server", info);
            String hash = sha(GSON.toJson(pack) + GSON.toJson(badges));
            JsonObject last = requests.has(ns) ? requests.getAsJsonObject(ns) : null;
            // asked before with the same badges and all decided: nothing to ask again
            if (last != null && hash.equals(str(last, "hash")) && !str(last, "statuses").contains("pending")) {
                continue;
            }
            post("/badges/request", body, 16384, (status, json) -> {
                JsonObject r = new JsonObject();
                r.addProperty("hash", hash);
                r.addProperty("status", status);
                if (status == 202) {
                    StringBuilder st = new StringBuilder();
                    for (JsonElement b : json.getAsJsonArray("badges")) {
                        JsonObject o = b.getAsJsonObject();
                        st.append(str(o, "id")).append(" ").append(str(o, "status"))
                            .append(o.has("reason") ? " (" + str(o, "reason") + ")" : "").append("; ");
                        JustQuests.LOG.info("JustQuests Discord: badge {} is {}{}", str(o, "id"), str(o, "status"),
                            o.has("reason") ? " (" + str(o, "reason") + ")" : "");
                    }
                    r.addProperty("statuses", st.toString());
                    String code = str(json, "verify_code");
                    if (!code.isEmpty()) {
                        r.addProperty("verify_code", code);
                        JustQuests.LOG.info("JustQuests Discord: put {} in your project description or README ({}) so the "
                            + "Discord owner sees the request for the badges of '{}' really comes from the pack.", code, str(pack, "url"), ns);
                    }
                } else if (status == 409) {
                    r.addProperty("statuses", "namespace_taken");
                    JustQuests.LOG.warn("JustQuests Discord: the namespace '{}' belongs to another pack on the JustQuests Discord. "
                        + "Use a different namespace for your badges.", ns);
                } else {
                    r.addProperty("statuses", "error " + status + ": " + str(json, "message"));
                    JustQuests.LOG.warn("JustQuests Discord: the badges of '{}' could not be sent ({} {} {})", ns, status,
                        str(json, "field"), str(json, "message"));
                }
                requests.add(ns, r);
                save(requestsFile, requests);
            });
        }
    }

    /** The bot's invite and privacy links. */
    private static void health() {
        get("/health", (status, json) -> {
            if (status != 200) return;
            if (!str(json, "invite").isEmpty()) invite = str(json, "invite");
            if (!str(json, "privacy").isEmpty()) privacy = str(json, "privacy");
        });
    }

    /** What the bot said about each datapack's badges (for /quest badges, operators). */
    public static Map<String, String> requestStatus() {
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

    private static void post(String path, JsonObject body, int maxBytes, Answer answer) {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) {
            JustQuests.LOG.warn("JustQuests Discord: request to {} too big ({} bytes)", path, bytes.length);
            return;
        }
        send(HttpRequest.newBuilder(URI.create(api() + path)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .header("Content-Type", "application/json"), answer);
    }

    private static void get(String path, Answer answer) {
        send(HttpRequest.newBuilder(URI.create(api() + path)).GET(), answer);
    }

    private static void send(HttpRequest.Builder request, Answer answer) {
        MinecraftServer s = server;
        if (s == null) return;
        HttpRequest r;
        try {
            r = request.timeout(Duration.ofSeconds(30))
                .header("User-Agent", "JustQuests/" + str(info, "mod_version") + " (" + str(info, "loader") + " " + str(info, "mc_version") + ")")
                .build();
        } catch (IllegalArgumentException e) {
            JustQuests.LOG.warn("JustQuests Discord: bad discordApi setting: {}", e.getMessage());
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
            if (err != null) JustQuests.LOG.debug("JustQuests Discord: {} failed: {}", r.uri(), err.toString());
            JsonObject result = json;
            s.execute(() -> answer.accept(status, result));
        });
    }

    /** A message for the player for a failed link. */
    private static Component error(int status, JsonObject json, boolean linking) {
        String code = str(json, "error");
        if (status == 404 && linking) return Msg.tr("justquests.discord.code_invalid", invite(), "/justquests link");
        if (status == 429) return Msg.tr("justquests.discord.too_many");
        if (status == 503 && "unavailable".equals(code)) return Msg.tr("justquests.discord.paused");
        if (status == 503 || status == 504 || status == 0) return Msg.tr("justquests.discord.offline");
        return Msg.tr("justquests.discord.failed", status + (code.isEmpty() ? "" : " " + code));
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

    private static JsonObject read(Path file) {
        try {
            if (Files.exists(file)) {
                JsonElement j = JsonParser.parseString(Files.readString(file));
                if (j.isJsonObject()) return j.getAsJsonObject();
            }
        } catch (Exception e) {
            JustQuests.LOG.error("Could not read {}", file, e);
        }
        return new JsonObject();
    }

    private static void save(Path file, JsonObject json) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject out = json.deepCopy();
            out.addProperty("_help", file.equals(linksFile)
                ? "JustQuests: links of players to the JustQuests Discord (their badges become roles there). "
                    + "A player removes their own with /quest discord unlink. Keep this file private: it holds each link's token."
                : "JustQuests: what the JustQuests Discord said about the badges of your datapacks (asked again when they change).");
            Files.writeString(file, GSON.toJson(out));
        } catch (Exception e) {
            JustQuests.LOG.error("Could not write {}", file, e);
        }
    }
}
