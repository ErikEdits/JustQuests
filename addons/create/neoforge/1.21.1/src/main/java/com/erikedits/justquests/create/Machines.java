package com.erikedits.justquests.create;

import com.erikedits.justquests.progress.QuestProgressService;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Where the mixins report what a machine made. Only the server thread gets past {@link #made}. */
public final class Machines {
    /** The fan or nozzle whose air current is working right now. */
    private static BlockEntity fan;
    /** The finished item of a sequenced assembly, until the machine doing the last step reports it. */
    private static ItemStack assembled = ItemStack.EMPTY;

    private Machines() {}

    public static void made(Machine machine, BlockEntity by, List<ItemStack> outputs) {
        if (by.getLevel() == null || by.getLevel().isClientSide) return;
        ItemStack finished = assembled;
        assembled = ItemStack.EMPTY;
        ServerPlayer player = Owners.credited(by);
        if (player == null) return;
        for (ItemStack stack : outputs) {
            if (stack.isEmpty()) continue;
            count(player, machine, stack);
            if (ItemStack.isSameItem(stack, finished)) count(player, Machine.ASSEMBLE, stack);
        }
    }

    private static void count(ServerPlayer player, Machine machine, ItemStack stack) {
        QuestProgressService.advance(player, QuestProgressService.kind(stack), obj ->
            obj instanceof MachineObjective m && m.matches(machine, stack) ? stack.getCount() : 0);
    }

    /** A basin's outputs: the press or mixer above it tells which machine it was. */
    public static void basin(BlockEntity basin, List<ItemStack> outputs) {
        BlockEntity operator = basin.getLevel().getBlockEntity(basin.getBlockPos().above(2));
        Machine machine = operator instanceof MechanicalPressBlockEntity ? Machine.COMPACT
            : operator instanceof MechanicalMixerBlockEntity ? Machine.MIX : null;
        if (machine != null) made(machine, Owners.owned(operator) ? operator : basin, outputs);
    }

    /** The results that a saw or crushing wheels put after the input slot. */
    public static List<ItemStack> results(ProcessingInventory inventory) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 1; slot < inventory.getSlots(); slot++) out.add(inventory.getStackInSlot(slot));
        return out;
    }

    public static void fan(BlockEntity source) {
        fan = source;
    }

    public static void fanned(FanProcessingType type, List<ItemStack> outputs) {
        if (fan == null) return;
        Machine machine = type == AllFanProcessingTypes.SPLASHING ? Machine.WASH
            : type == AllFanProcessingTypes.SMOKING ? Machine.SMOKE
            : type == AllFanProcessingTypes.BLASTING ? Machine.BLAST
            : type == AllFanProcessingTypes.HAUNTING ? Machine.HAUNT : null;
        if (machine != null) made(machine, fan, outputs);
    }

    public static void assembled(ItemStack result) {
        assembled = result.copy();
    }
}
