package com.erikedits.justquests.create;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Who a machine works for: the player who placed it. Machines without a known owner (placed before
 * the addon came, built by a schematic) work for the nearest player. An owner who is offline gets
 * nothing, and nobody else gets it instead.
 */
public final class Owners {
    /** How far away a player still gets the credit for a machine without an owner. */
    private static final double NEARBY = 32;

    static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JustQuestsCreate.MOD_ID);

    private static final Supplier<AttachmentType<UUID>> OWNER = ATTACHMENTS.register("owner",
        () -> AttachmentType.<UUID>builder(() -> Util.NIL_UUID).serialize(UUIDUtil.CODEC).build());

    private Owners() {}

    static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (isMachine(be)) be.setData(OWNER, player.getUUID());
    }

    private static boolean isMachine(BlockEntity be) {
        return be instanceof MechanicalPressBlockEntity || be instanceof MechanicalMixerBlockEntity
            || be instanceof BasinBlockEntity || be instanceof MillstoneBlockEntity
            || be instanceof CrushingWheelBlockEntity || be instanceof SawBlockEntity
            || be instanceof DeployerBlockEntity || be instanceof SpoutBlockEntity
            || be instanceof MechanicalCrafterBlockEntity || be instanceof EncasedFanBlockEntity;
    }

    static boolean owned(BlockEntity be) {
        return be != null && be.hasData(OWNER);
    }

    /** The player to count for, or null. */
    static ServerPlayer credited(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) return null;
        UUID owner = owner(level, be);
        if (owner != null) return level.getServer().getPlayerList().getPlayer(owner);
        BlockPos pos = be.getBlockPos();
        return level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, NEARBY, EntitySelector.NO_SPECTATORS)
            instanceof ServerPlayer player ? player : null;
    }

    private static UUID owner(ServerLevel level, BlockEntity be) {
        if (owned(be)) return be.getData(OWNER);
        // nobody places the block between two crushing wheels, so its wheels count
        if (be instanceof CrushingWheelControllerBlockEntity) {
            for (Direction side : Direction.values()) {
                BlockEntity wheel = level.getBlockEntity(be.getBlockPos().relative(side));
                if (wheel instanceof CrushingWheelBlockEntity && owned(wheel)) return wheel.getData(OWNER);
            }
        }
        return null;
    }
}
