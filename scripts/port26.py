"""Turns the source of a 1.21.11 tree into a Minecraft 26.x tree (unobfuscated, GuiGraphicsExtractor era).

python port26.py <tree dir> <neoforge|fabric> <mc version>   (edits the tree's src/ in place; run on a fresh copy)
Each rule must match at least once somewhere in the tree, so a stale rule shows up instead of silently doing nothing.
"""
import os
import re
import sys

COMMON = [
    # GUI: the draw context is an "extractor" now and screens extract instead of render
    (r"\bimport net\.minecraft\.client\.gui\.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;"),
    (r"\bGuiGraphics\b", "GuiGraphicsExtractor"),
    (r"\bg\.drawString\(", "g.text("),
    (r"\bg\.renderItem\(", "g.item("),
    (r"public void render\(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt\)",
     "public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt)"),
    (r"this\.renderBackground\(g, mouseX, mouseY, pt\);", "this.extractBackground(g, mouseX, mouseY, pt);"),
    (r"search\.render\(g, mouseX, mouseY, pt\);", "search.extractRenderState(g, mouseX, mouseY, pt);"),
    # spawn eggs are looked up as item holders
    (r"SpawnEggItem egg = SpawnEggItem\.byId\(s\.type\(\)\);\s*return egg != null \? egg : fallback;",
     "return SpawnEggItem.byId(s.type()).map(net.minecraft.core.Holder::value).orElse(fallback);"),
    (r"SpawnEggItem egg = SpawnEggItem\.byId\(t\);\s*if \(egg != null\) return egg;",
     "var egg = SpawnEggItem.byId(t);\n                if (egg.isPresent()) return egg.get().value();"),
    # entity type tags go through the registry holder
    (r"return t\.is\(tag\);", "return t.builtInRegistryHolder().is(tag);"),
    # action-bar message
    (r"player\.displayClientMessage\(([^;]*?), true\);", r"player.sendOverlayMessage(\1);"),
]

FABRIC = [
    (r"net\.fabricmc\.fabric\.api\.client\.keybinding\.v1\.KeyBindingHelper", "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper"),
    (r"KeyBindingHelper\.registerKeyBinding\(", "KeyMappingHelper.registerKeyMapping("),
    (r"net\.fabricmc\.fabric\.api\.client\.rendering\.v1\.HudRenderCallback\.EVENT\.register\(\(graphics, tick\) -> QuestHud\.render\(graphics\)\);",
     "net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(\n"
     "            net.minecraft.resources.Identifier.fromNamespaceAndPath(\"justquests\", \"quest_tracker\"),\n"
     "            (graphics, tick) -> QuestHud.render(graphics));"),
    (r"\bServerEntityWorldChangeEvents\.AFTER_PLAYER_CHANGE_WORLD\b", "ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL"),
    (r"\bServerEntityWorldChangeEvents\b", "ServerEntityLevelChangeEvents"),
    (r"PayloadTypeRegistry\.playS2C\(\)", "PayloadTypeRegistry.clientboundPlay()"),
]

NEOFORGE = [
    (r"\bBlockEvent\.BreakEvent\b", "BreakBlockEvent"),
    (r"(import net\.neoforged\.neoforge\.event\.level\.BlockEvent;)",
     r"\1\nimport net.neoforged.neoforge.event.level.block.BreakBlockEvent;"),
]

# 26.2+: the screen and the HUD moved under Minecraft.gui; ChatFormatting.isColor and I18n.exists are gone
V262 = [
    (r"\b(mc|client)\.screen\b", r"\1.gui.screen()"),
    (r"\b(mc|client)\.setScreen\(", r"\1.gui.setScreen("),
    (r"\.gui\.setOverlayMessage\(", ".gui.hud.setOverlayMessage("),
    (r"\bmc\.options\.hideGui\b", "mc.gui.hud.isHidden()"),
    (r"\bI18n\.exists\(key\)", "net.minecraft.locale.Language.getInstance().has(key)"),
    (r"\bf\.isColor\(\)", "f.ordinal() <= ChatFormatting.WHITE.ordinal()"),
]

# 26.3+: keyboard constants without GLFW, item drops say whether the client predicts them
V263 = [
    (r"import org\.lwjgl\.glfw\.GLFW;\n", ""),
    (r"\bGLFW\.GLFW_KEY_([A-Z0-9]+)\b", r"InputConstants.KEY_\1"),
    (r"\bInputConstants\.Type\.KEYSYM\b", "InputConstants.Type.KEYBOARD"),
    (r"player\.drop\(stack, false\);", "player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);"),
]


def main():
    root, loader, mc = sys.argv[1], sys.argv[2], sys.argv[3]
    minor = int(mc.split(".")[1])
    rules = COMMON + (FABRIC if loader == "fabric" else NEOFORGE)
    if minor >= 2:
        rules += V262
    if minor >= 3:
        rules += V263
    hits = {i: 0 for i in range(len(rules))}
    for dirpath, _, files in os.walk(os.path.join(root, "src", "main", "java")):
        for f in files:
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            raw = open(p, encoding="utf-8", newline="").read()
            crlf = "\r\n" in raw
            text = raw.replace("\r\n", "\n")
            for i, (pat, rep) in enumerate(rules):
                text, n = re.subn(pat, rep, text)
                hits[i] += n
            if crlf:
                text = text.replace("\n", "\r\n")
            if text != raw:
                open(p, "w", encoding="utf-8", newline="").write(text)
    unused = [rules[i][0] for i, n in hits.items() if n == 0]
    if unused:
        sys.exit("rules that matched nothing: %s" % unused)
    print("ported", root, mc, sum(hits.values()), "replacements")


if __name__ == "__main__":
    main()
