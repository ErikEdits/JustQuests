package com.erikedits.justquests.create;

/** What a Create machine can do. Each one is an objective type: "justquests_create:press_item". */
public enum Machine {
    PRESS("press"),
    COMPACT("compact"),
    MIX("mix"),
    MILL("mill"),
    CRUSH("crush"),
    SAW("saw"),
    DEPLOY("deploy"),
    FILL("fill"),
    MECHANICAL_CRAFT("mechanical_craft"),
    WASH("wash"),
    SMOKE("smoke"),
    BLAST("blast"),
    HAUNT("haunt"),
    ASSEMBLE("assemble");

    private final String id;

    Machine(String id) {
        this.id = id;
    }

    public String typeId() {
        return JustQuestsCreate.MOD_ID + ":" + id + "_item";
    }

    /** The goal text: "Press %s". With "_count" added: "Press %sx %s". */
    public String key() {
        return JustQuestsCreate.MOD_ID + ".goal." + id;
    }
}
