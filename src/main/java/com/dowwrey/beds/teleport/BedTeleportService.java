package com.dowwrey.beds.teleport;

import com.dowwrey.beds.network.BedNetworking;
import com.dowwrey.beds.network.payload.BedTeleportCooldownPayload;
import com.dowwrey.beds.network.payload.BedTeleportStatePayload;
import com.dowwrey.beds.storage.BedSavedData;
import com.dowwrey.beds.storage.BedSelectionSavedData;
import com.dowwrey.beds.storage.BedServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative teleportation to already discovered bed locations.
 * The only gameplay cancellation source is actual damage; movement itself is not a cancellation source.
 */
public final class BedTeleportService {

    private static final Map<UUID, PendingTeleport> PENDING = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    private BedTeleportService() {}

    private record PendingTeleport(UUID bedId, String bedName, int mode, int cost, int durationTicks,
                                   long startedAt, boolean brokenAtStart) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BedTeleportService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancelAndRefund(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(BedTeleportService::stopAndRefund);

        // Damage is the one gameplay event that cancels a teleport.
        // We cancel the pending transaction here and return true so the original damage still lands.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount > 0.0F && entity instanceof ServerPlayer player && !player.isRemoved() && PENDING.containsKey(player.getUUID())) {
                MinecraftServer server = player.level().getServer();
                if (server != null) {
                    cancelForDamage(server, player);
                }
            }
            return true;
        });
    }

    public static void syncCooldown(MinecraftServer server, ServerPlayer player) {
        long now = server.getTickCount();
        long remaining = Math.max(0L, COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L) - now);
        ServerPlayNetworking.send(player, new BedTeleportCooldownPayload((int) Math.min(Integer.MAX_VALUE, remaining)));
    }

    public static void begin(MinecraftServer server, ServerPlayer player, UUID bedId, int mode) {
        BedServerConfig config = BedServerConfig.get(server);
        if (!config.teleportEnabled) {
            BedNetworking.notifyError(player, "dowwrey_beds.server.feature_disabled");
            return;
        }
        if (player.isDeadOrDying() || player.isSpectator()) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.unavailable");
            return;
        }
        if (player.isPassenger() || player.isSleeping()) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.busy");
            return;
        }
        if (PENDING.containsKey(player.getUUID())) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.already_active");
            return;
        }

        long now = server.getTickCount();
        long cooldownUntil = COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L);
        if (cooldownUntil > now) {
            int seconds = Math.max(1, (int) Math.ceil((cooldownUntil - now) / 20.0D));
            ServerPlayNetworking.send(player, new BedTeleportCooldownPayload((int) Math.min(Integer.MAX_VALUE, cooldownUntil - now)));
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.cooldown", String.valueOf(seconds));
            return;
        }

        BedSavedData.SavedBed bed = BedSavedData.get(server).findById(player.getUUID(), bedId);
        if (bed == null) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.not_found");
            return;
        }

        ServerLevel targetLevel = getLevel(server, bed.dimension());
        if (targetLevel == null) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.unavailable");
            return;
        }

        boolean broken = !isBedPresent(targetLevel, bed.pos());
        TeleportProfile profile = profileFor(broken, mode, config);
        if (profile == null) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.bad_option");
            return;
        }

        if (countEyes(player) < profile.cost()) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.not_enough_eyes", String.valueOf(profile.cost()));
            return;
        }

        Optional<net.minecraft.world.phys.Vec3> safe = findSafePosition(player, targetLevel, bed.pos(), broken);
        if (safe.isEmpty()) {
            BedNetworking.notifyError(player, broken
                    ? "dowwrey_beds.teleport.no_safe_broken"
                    : "dowwrey_beds.teleport.no_safe");
            return;
        }

        if (!consumeEyes(player, profile.cost())) {
            BedNetworking.notifyError(player, "dowwrey_beds.teleport.not_enough_eyes", String.valueOf(profile.cost()));
            return;
        }

        PendingTeleport pending = new PendingTeleport(
                bed.id(), bed.name(), profile.mode(), profile.cost(), profile.durationTicks(), now, broken
        );
        PENDING.put(player.getUUID(), pending);

        // A quiet Ender cue marks the beginning without making the feature feel like a command.
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.35F, 0.72F);
        ServerPlayNetworking.send(player, new BedTeleportStatePayload(
                bed.id(), bed.name(), profile.durationTicks(), profile.cost(), BedTeleportStatePayload.STARTED
        ));
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now);

        if (PENDING.isEmpty()) return;

        for (var iterator = PENDING.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            UUID uuid = entry.getKey();
            PendingTeleport pending = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (player.isDeadOrDying() || player.isRemoved()) {
                refund(player, pending.cost());
                sendCancelled(player, pending, "dowwrey_beds.teleport.cancelled");
                iterator.remove();
                continue;
            }

            if (now - pending.startedAt() < pending.durationTicks()) {
                continue;
            }

            BedSavedData data = BedSavedData.get(server);
            BedSavedData.SavedBed bed = data.findById(player.getUUID(), pending.bedId());
            if (bed == null) {
                refund(player, pending.cost());
                sendCancelled(player, pending, "dowwrey_beds.teleport.not_found");
                iterator.remove();
                continue;
            }

            ServerLevel targetLevel = getLevel(server, bed.dimension());
            if (targetLevel == null) {
                refund(player, pending.cost());
                sendCancelled(player, pending, "dowwrey_beds.teleport.unavailable");
                iterator.remove();
                continue;
            }

            boolean brokenNow = !isBedPresent(targetLevel, bed.pos());
            if (!pending.brokenAtStart() && brokenNow) {
                refund(player, pending.cost());
                sendCancelled(player, pending, "dowwrey_beds.teleport.bed_broken_during");
                iterator.remove();
                BedNetworking.refreshBedList(server, player);
                continue;
            }

            Optional<net.minecraft.world.phys.Vec3> safe = findSafePosition(player, targetLevel, bed.pos(), brokenNow);
            if (safe.isEmpty()) {
                refund(player, pending.cost());
                sendCancelled(player, pending, brokenNow
                        ? "dowwrey_beds.teleport.no_safe_broken"
                        : "dowwrey_beds.teleport.no_safe");
                iterator.remove();
                continue;
            }

            var destination = safe.get();
            ServerLevel originLevel = player.level();
            originLevel.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ(),
                    10, 0.28D, 0.55D, 0.28D, 0.06D);
            player.teleportTo(targetLevel, destination.x, destination.y, destination.z,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            targetLevel.sendParticles(ParticleTypes.PORTAL,
                    destination.x, destination.y + 0.6D, destination.z,
                    14, 0.32D, 0.65D, 0.32D, 0.07D);
            targetLevel.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.78F, 1.0F);
            data.markLastUsed(player.getUUID(), bed.id(), server.overworld().getGameTime());

            if (pending.brokenAtStart()) {
                BedSavedData.SavedBed removed = data.remove(player.getUUID(), bed.id());
                if (removed != null) {
                    BedSelectionSavedData selection = BedSelectionSavedData.get(server);
                    selection.clearIfSelected(player.getUUID(), removed.id());
                    ensureSelectedBed(server, player.getUUID());
                }
            }

            long cooldownUntil = now + BedServerConfig.get(server).teleportCooldownSeconds * 20L;
            COOLDOWN_UNTIL.put(player.getUUID(), cooldownUntil);
            ServerPlayNetworking.send(player, new BedTeleportCooldownPayload((int) Math.min(Integer.MAX_VALUE, BedServerConfig.get(server).teleportCooldownSeconds * 20L)));
            ServerPlayNetworking.send(player, new BedTeleportStatePayload(
                    pending.bedId(), pending.bedName(), pending.durationTicks(), pending.cost(), BedTeleportStatePayload.COMPLETED
            ));
            BedNetworking.notifySuccess(player, "dowwrey_beds.teleport.success", pending.bedName());
            BedNetworking.refreshBedList(server, player);
            iterator.remove();
        }
    }

    private static void cancelForDamage(MinecraftServer server, ServerPlayer player) {
        PendingTeleport pending = PENDING.remove(player.getUUID());
        if (pending == null) return;

        refund(player, pending.cost());
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.28F, 0.48F);
        sendCancelled(player, pending, "dowwrey_beds.teleport.cancelled");
    }

    private static void sendCancelled(ServerPlayer player, PendingTeleport pending, String notificationKey) {
        ServerPlayNetworking.send(player, new BedTeleportStatePayload(
                pending.bedId(), pending.bedName(), pending.durationTicks(), pending.cost(), BedTeleportStatePayload.CANCELLED
        ));
        BedNetworking.notifyInfo(player, notificationKey);
    }

    private record TeleportProfile(int mode, int cost, int durationTicks) {}

    private static TeleportProfile profileFor(boolean broken, int mode, BedServerConfig config) {
        if (broken) {
            if (!config.brokenBedTeleportEnabled || mode != 3) return null;
            return new TeleportProfile(3, config.brokenTeleportCost, config.brokenTeleportSeconds * 20);
        }
        return switch (mode) {
            case 0 -> new TeleportProfile(0, config.slowTeleportCost, config.slowTeleportSeconds * 20);
            case 1 -> new TeleportProfile(1, config.fastTeleportCost, config.fastTeleportSeconds * 20);
            case 2 -> new TeleportProfile(2, config.instantTeleportCost, config.instantTeleportSeconds * 20);
            default -> null;
        };
    }

    private static Optional<net.minecraft.world.phys.Vec3> findSafePosition(ServerPlayer player, ServerLevel level, BlockPos origin, boolean broken) {
        var dimensions = player.getDimensions(Pose.STANDING);
        ArrayList<BlockPos> candidates = new ArrayList<>();

        if (!broken) {
            BlockState state = level.getBlockState(origin);
            if (state.getBlock() instanceof BedBlock) {
                Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                candidates.add(origin.relative(facing.getOpposite()));
                candidates.add(origin.relative(facing.getClockWise()));
                candidates.add(origin.relative(facing.getCounterClockWise()));
                candidates.add(origin.relative(facing));
            }
        }

        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) != radius) continue;
                    candidates.add(origin.offset(dx, 0, dz));
                }
            }
        }

        for (BlockPos stand : candidates) {
            Optional<net.minecraft.world.phys.Vec3> result = checkCandidate(player, level, dimensions, stand);
            if (result.isPresent()) return result;
            result = checkCandidate(player, level, dimensions, stand.above());
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private static Optional<net.minecraft.world.phys.Vec3> checkCandidate(ServerPlayer player, ServerLevel level,
                                                                            net.minecraft.world.entity.EntityDimensions dimensions,
                                                                            BlockPos stand) {
        if (!level.getFluidState(stand).isEmpty()) return Optional.empty();
        if (!level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()) return Optional.empty();
        BlockPos support = stand.below();
        if (level.getBlockState(support).getCollisionShape(level, support).isEmpty()) return Optional.empty();
        var position = new net.minecraft.world.phys.Vec3(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
        return level.noCollision(player, dimensions.makeBoundingBox(position))
                ? Optional.of(position) : Optional.empty();
    }

    public static boolean hasSafeDestination(ServerPlayer player, ServerLevel level, BlockPos origin, boolean broken) {
        return findSafePosition(player, level, origin, broken).isPresent();
    }

    private static boolean isBedPresent(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    private static ServerLevel getLevel(MinecraftServer server, String dimension) {
        try {
            Identifier id = Identifier.parse(dimension);
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
            return server.getLevel(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void ensureSelectedBed(MinecraftServer server, UUID owner) {
        BedSelectionSavedData selection = BedSelectionSavedData.get(server);
        BedSavedData data = BedSavedData.get(server);
        UUID selected = selection.getSelected(owner);
        if (selected != null && data.findById(owner, selected) != null) return;
        List<BedSavedData.SavedBed> remaining = data.list(owner);
        if (!remaining.isEmpty()) selection.setSelected(owner, remaining.getFirst().id());
    }

    private static int countEyes(ServerPlayer player) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.ENDER_EYE)) total += stack.getCount();
        }
        return total;
    }

    private static boolean consumeEyes(ServerPlayer player, int amount) {
        if (countEyes(player) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.ENDER_EYE)) continue;
            int take = Math.min(remaining, stack.getCount());
            player.getInventory().removeItem(slot, take);
            remaining -= take;
        }
        player.containerMenu.broadcastChanges();
        return remaining == 0;
    }

    private static void refund(ServerPlayer player, int amount) {
        if (amount <= 0) return;

        ItemStack refund = new ItemStack(Items.ENDER_EYE, amount);
        player.getInventory().placeItemBackInInventory(refund, Prediction.SERVER_ONLY);
        player.containerMenu.broadcastChanges();

        // Do not silently delete a refund if the inventory was full. Any uninserted remainder is dropped safely.
        if (!refund.isEmpty()) {
            player.drop(refund, false, Prediction.SERVER_ONLY);
        }
    }

    private static void cancelAndRefund(ServerPlayer player) {
        PendingTeleport pending = PENDING.remove(player.getUUID());
        if (pending != null) refund(player, pending.cost());
    }

    private static void stopAndRefund(MinecraftServer server) {
        for (var entry : PENDING.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) refund(player, entry.getValue().cost());
        }
        PENDING.clear();
        COOLDOWN_UNTIL.clear();
    }
}
