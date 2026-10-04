package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.api.ContentKind;

import java.util.List;

/**
 * A tag concept (tags.json): the first candidate tag with non-empty members on this build wins.
 *
 * @param key        concept key ({@code logs})
 * @param kind       ITEM, BLOCK or ENTITY
 * @param candidates tag ids without {@code #}, in preference order
 * @param name       singular English noun for hints ("log") 
 * @param plural     plural English noun ("logs")
 */
public record TagConcept(String key, ContentKind kind, List<String> candidates, String name, String plural) {
}
