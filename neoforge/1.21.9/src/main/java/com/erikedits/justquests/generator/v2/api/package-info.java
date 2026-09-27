/**
 * The stable contract between the generator core and the mod.
 *
 * <p>Two directions:
 * <ul>
 *   <li><b>Endpoints IN</b> — interfaces the mod implements once per build (the "host adapter"):
 *       {@link com.erikedits.justquests.generator.v2.api.GeneratorHost} and the views it hands out
 *       ({@link com.erikedits.justquests.generator.v2.api.ContentView},
 *       {@link com.erikedits.justquests.generator.v2.api.WorldContext},
 *       {@link com.erikedits.justquests.generator.v2.api.StateStore},
 *       {@link com.erikedits.justquests.generator.v2.api.QuestValidator},
 *       {@link com.erikedits.justquests.generator.v2.api.HostCapabilities},
 *       {@link com.erikedits.justquests.generator.v2.api.GenLog}).</li>
 *   <li><b>Values OUT</b> — records and enums returned by
 *       {@link com.erikedits.justquests.generator.v2.QuestGeneratorV2}.</li>
 * </ul>
 *
 * <p>Everything in this package is plain Java 17 plus the Gson 2.8.8 API; nothing here touches
 * Minecraft or loader classes. All calls happen on the Minecraft server thread; nothing in the core
 * is thread-safe.
 */
package com.erikedits.justquests.generator.v2.api;
