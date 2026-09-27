package com.erikedits.justquests.generator.v2.api;

import java.util.UUID;

/**
 * A claim the core released on its own (expiry or start-up reconciliation). For expiries the mod
 * removes the quest from that player's active quests (as if abandoned) and notifies the player.
 *
 * @param questId full quest id
 * @param holder  the player who held it
 */
public record ExpiredClaim(String questId, UUID holder) {
}
