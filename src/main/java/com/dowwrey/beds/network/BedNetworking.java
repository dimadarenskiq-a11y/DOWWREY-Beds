package com.dowwrey.beds.network;

import com.dowwrey.beds.network.payload.BedGiftRequestsPayload;
import com.dowwrey.beds.network.payload.BedListPayload;
import com.dowwrey.beds.network.payload.BedNotificationPayload;
import com.dowwrey.beds.network.payload.CreateBedPayload;
import com.dowwrey.beds.network.payload.CreateFriendBedPayload;
import com.dowwrey.beds.network.payload.DeleteBedPayload;
import com.dowwrey.beds.network.payload.OpenPlayerSelectPayload;
import com.dowwrey.beds.network.payload.OpenSaveBedPayload;
import com.dowwrey.beds.network.payload.RenameBedPayload;
import com.dowwrey.beds.network.payload.RequestBedGiftPayload;
import com.dowwrey.beds.network.payload.RequestBedListPayload;
import com.dowwrey.beds.network.payload.RespondBedGiftPayload;
import com.dowwrey.beds.network.payload.RespawnAtBedPayload;
import com.dowwrey.beds.network.payload.RespawnAtBedApprovedPayload;
import com.dowwrey.beds.network.payload.SetActiveBedPayload;
import com.dowwrey.beds.network.payload.StartBedTeleportPayload;
import com.dowwrey.beds.network.payload.BedTeleportStatePayload;
import com.dowwrey.beds.network.payload.BedTeleportCooldownPayload;
import com.dowwrey.beds.network.payload.BedServerRulesPayload;
import com.dowwrey.beds.storage.BedGiftRequestSavedData;
import com.dowwrey.beds.storage.BedColors;
import com.dowwrey.beds.storage.BedIcons;
import com.dowwrey.beds.storage.BedServerConfig;
import com.dowwrey.beds.storage.BedSavedData;
import com.dowwrey.beds.storage.BedSelectionSavedData;
import com.dowwrey.beds.teleport.BedTeleportService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.Level;

import java.util.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BedNetworking {
    private static final Map<UUID, Long> LIST_REQUEST_NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Long> FRIEND_REQUEST_NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Long> GIFT_LIST_NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Long> FRIEND_SELECT_NEXT_ALLOWED = new HashMap<>();
    private static final Map<UUID, Long> ACTION_NEXT_ALLOWED = new HashMap<>();
    private static final int FRIEND_SELECT_COOLDOWN_TICKS = 10; // 500 ms; only the player-selector request is throttled.
    private static final int ACTION_COOLDOWN_TICKS = 2; // 100 ms; protects server-side mutation/response handlers without affecting normal UX.

    private BedNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(OpenSaveBedPayload.TYPE, OpenSaveBedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedListPayload.TYPE, BedListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenPlayerSelectPayload.TYPE, OpenPlayerSelectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedGiftRequestsPayload.TYPE, BedGiftRequestsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedNotificationPayload.TYPE, BedNotificationPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RespawnAtBedApprovedPayload.TYPE, RespawnAtBedApprovedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedTeleportStatePayload.TYPE, BedTeleportStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedTeleportCooldownPayload.TYPE, BedTeleportCooldownPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BedServerRulesPayload.TYPE, BedServerRulesPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(CreateBedPayload.TYPE, CreateBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CreateFriendBedPayload.TYPE, CreateFriendBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestBedListPayload.TYPE, RequestBedListPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetActiveBedPayload.TYPE, SetActiveBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RenameBedPayload.TYPE, RenameBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteBedPayload.TYPE, DeleteBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestBedGiftPayload.TYPE, RequestBedGiftPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RespondBedGiftPayload.TYPE, RespondBedGiftPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RespawnAtBedPayload.TYPE, RespawnAtBedPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StartBedTeleportPayload.TYPE, StartBedTeleportPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CreateBedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var server = context.server();
            var level = player.level();
            BlockPos pos = payload.pos();

            if (!player.isAlive() || player.isSpectator()) {
                return;
            }

            if (!(level.getBlockState(pos).getBlock() instanceof BedBlock) || !withinRange(player, pos)) {
                return;
            }

            String name = payload.name().trim();
            if (!validateName(player, name)) {
                return;
            }

            BedSavedData data = BedSavedData.get(server);
            if (data.contains(player.getUUID(), level, pos)) {
                notifyError(player, "dowwrey_beds.save_bed.invalid");
                return;
            }

            if (!BedColors.isAllowed(payload.color()) || !BedIcons.isAllowed(payload.icon())) {
                notifyError(player, "dowwrey_beds.save_bed.invalid_appearance");
                return;
            }

            if (payload.forFriend()) {
                BedServerConfig rules = BedServerConfig.get(server);
                if (!rules.friendBedsEnabled) {
                    notifyError(player, "dowwrey_beds.server.feature_disabled");
                    return;
                }

                long nowTick = server.getTickCount();
                Long nextAllowed = FRIEND_SELECT_NEXT_ALLOWED.get(player.getUUID());
                if (nextAllowed != null && nextAllowed > nowTick) {
                    return;
                }
                FRIEND_SELECT_NEXT_ALLOWED.put(player.getUUID(), nowTick + FRIEND_SELECT_COOLDOWN_TICKS);

                List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers().stream()
                        .filter(other -> !other.getUUID().equals(player.getUUID()))
                        .toList();
                if (onlinePlayers.isEmpty()) {
                    notifyError(player, "dowwrey_beds.friend.no_players");
                    return;
                }

                Set<UUID> ownerIds = new HashSet<>();
                for (ServerPlayer other : onlinePlayers) {
                    ownerIds.add(other.getUUID());
                }
                Map<UUID, Integer> counts = data.countsFor(ownerIds);
                List<OpenPlayerSelectPayload.PlayerEntry> players = new ArrayList<>(onlinePlayers.size());
                for (ServerPlayer other : onlinePlayers) {
                    UUID owner = other.getUUID();
                    players.add(new OpenPlayerSelectPayload.PlayerEntry(
                            owner,
                            other.getGameProfile().name(),
                            counts.getOrDefault(owner, 0),
                            rules.maxBeds,
                            true
                    ));
                }

                ServerPlayNetworking.send(player, new OpenPlayerSelectPayload(
                        pos, name, payload.color(), BedIcons.sanitize(payload.icon()), players
                ));
                return;
            }

            if (data.hasName(player.getUUID(), name)) {
                notifyError(player, "dowwrey_beds.save_bed.name_taken");
                return;
            }
            if (!data.canAdd(player.getUUID(), BedServerConfig.get(server).maxBeds)) {
                notifyError(player, "dowwrey_beds.save_bed.limit");
                return;
            }

            data.add(player.getUUID(), name, level, pos, payload.color(), payload.icon(), level.getGameTime());
            notifySuccess(player, "dowwrey_beds.save_bed.saved", name);
            ensureSelectedBed(server, player.getUUID());
            sendBedList(server, player, false);
        });

        ServerPlayNetworking.registerGlobalReceiver(CreateFriendBedPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            var server = context.server();
            var level = sender.level();
            BlockPos pos = payload.pos();

            if (!sender.isAlive() || sender.isSpectator()) {
                return;
            }
            String name = payload.bedName().trim();

            if (!(level.getBlockState(pos).getBlock() instanceof BedBlock) || !withinRange(sender, pos)) {
                return;
            }
            if (!validateName(sender, name)) {
                return;
            }
            if (!BedColors.isAllowed(payload.color()) || !BedIcons.isAllowed(payload.icon())) {
                notifyError(sender, "dowwrey_beds.save_bed.invalid_appearance");
                return;
            }

            long nowTick = server.getTickCount();
            BedServerConfig serverConfig = BedServerConfig.get(server);
            if (!serverConfig.friendBedsEnabled) {
                notifyError(sender, "dowwrey_beds.server.feature_disabled");
                return;
            }
            Long nextAllowed = FRIEND_REQUEST_NEXT_ALLOWED.get(sender.getUUID());
            if (nextAllowed != null && nextAllowed > nowTick) {
                long seconds = Math.max(1L, (long) Math.ceil((nextAllowed - nowTick) / 20.0D));
                notifyError(sender, "dowwrey_beds.friend.too_fast", String.valueOf(seconds));
                return;
            }

            BedSavedData data = BedSavedData.get(server);
            ServerPlayer target = server.getPlayerList().getPlayer(payload.targetPlayer());
            if (target == null || target.getUUID().equals(sender.getUUID())) {
                notifyError(sender, "dowwrey_beds.friend.player_unavailable");
                return;
            }

            BedGiftRequestSavedData requests = BedGiftRequestSavedData.get(server);
            requests.cleanupExpired(server.overworld().getGameTime());
            String dimension = level.dimension().identifier().toString();
            if (!requests.canAddFor(target.getUUID())) {
                notifyError(sender, "dowwrey_beds.friend.request_limit");
                return;
            }
            if (requests.hasDuplicate(sender.getUUID(), target.getUUID(), name, dimension, pos)) {
                notifyError(sender, "dowwrey_beds.friend.duplicate_request");
                return;
            }

            BedGiftRequestSavedData.Request request = new BedGiftRequestSavedData.Request(
                    UUID.randomUUID(),
                    sender.getUUID(),
                    target.getUUID(),
                    sender.getGameProfile().name(),
                    name,
                    dimension,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    server.overworld().getGameTime() + serverConfig.friendRequestLifetimeSeconds * 20L,
                    payload.color(),
                    BedIcons.sanitize(payload.icon())
            );
            requests.add(request);
            FRIEND_REQUEST_NEXT_ALLOWED.put(sender.getUUID(), nowTick + serverConfig.friendRequestCooldownTicks);
            notifySuccess(sender, "dowwrey_beds.friend.sent", target.getGameProfile().name());
            sendGiftRequests(server, target);
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestBedListPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            long now = server.getTickCount();
            Long nextAllowed = LIST_REQUEST_NEXT_ALLOWED.get(player.getUUID());
            if (nextAllowed != null && nextAllowed > now) {
                return;
            }
            LIST_REQUEST_NEXT_ALLOWED.put(player.getUUID(), now + BedServerConfig.get(server).listRequestCooldownTicks);
            sendBedList(server, player, payload.openManager());
        });

        ServerPlayNetworking.registerGlobalReceiver(SetActiveBedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var server = context.server();
            if (!allowAction(server, player.getUUID())) return;
            BedSavedData data = BedSavedData.get(server);
            if (data.findById(player.getUUID(), payload.bedId()) == null) {
                return;
            }
            BedSelectionSavedData.get(server).setSelected(player.getUUID(), payload.bedId());
            sendBedList(server, player, false);
        });

        ServerPlayNetworking.registerGlobalReceiver(RenameBedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!allowAction(context.server(), player.getUUID())) return;
            String name = payload.name().trim();
            if (!validateName(player, name)) {
                return;
            }
            BedSavedData data = BedSavedData.get(context.server());
            if (data.hasNameExcept(player.getUUID(), payload.bedId(), name)) {
                notifyError(player, "dowwrey_beds.manager.name_taken");
                return;
            }
            if (!BedColors.isAllowed(payload.color()) || !BedIcons.isAllowed(payload.icon())) {
                notifyError(player, "dowwrey_beds.save_bed.invalid_appearance");
                return;
            }
            if (data.rename(player.getUUID(), payload.bedId(), name, payload.color(), payload.icon())) {
                notifySuccess(player, "dowwrey_beds.manager.renamed");
                sendBedList(context.server(), player, false);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(DeleteBedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var server = context.server();
            if (!allowAction(server, player.getUUID())) return;
            BedSavedData data = BedSavedData.get(server);
            BedSavedData.SavedBed removed = data.remove(player.getUUID(), payload.bedId());
            if (removed == null) {
                return;
            }

            BedSelectionSavedData selection = BedSelectionSavedData.get(server);
            selection.clearIfSelected(player.getUUID(), removed.id());
            ensureSelectedBed(server, player.getUUID());

            notifySuccess(player, "dowwrey_beds.manager.deleted");
            sendBedList(server, player, false);
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestBedGiftPayload.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            long now = server.getTickCount();
            Long nextAllowed = GIFT_LIST_NEXT_ALLOWED.get(player.getUUID());
            int cooldown = Math.max(2, BedServerConfig.get(server).listRequestCooldownTicks);
            if (nextAllowed != null && nextAllowed > now) return;
            GIFT_LIST_NEXT_ALLOWED.put(player.getUUID(), now + cooldown);
            sendGiftRequests(server, player);
        });

        ServerPlayNetworking.registerGlobalReceiver(RespondBedGiftPayload.TYPE, (payload, context) -> {
            ServerPlayer recipient = context.player();
            var server = context.server();
            if (!allowAction(server, recipient.getUUID())) return;
            BedGiftRequestSavedData requests = BedGiftRequestSavedData.get(server);
            requests.cleanupExpired(server.overworld().getGameTime());
            BedGiftRequestSavedData.Request request = requests.findFor(recipient.getUUID(), payload.requestId());
            if (request == null) {
                return;
            }

            if (!payload.accepted()) {
                requests.removeFor(recipient.getUUID(), request.id());
                notifyInfo(recipient, "dowwrey_beds.friend.declined");
                ServerPlayer sender = server.getPlayerList().getPlayer(request.sender());
                if (sender != null) {
                    notifyInfo(sender, "dowwrey_beds.friend.declined_by", recipient.getGameProfile().name());
                }
                sendGiftRequests(server, recipient);
                return;
            }

            BedServerConfig serverConfig = BedServerConfig.get(server);
            if (!serverConfig.friendBedsEnabled) {
                notifyError(recipient, "dowwrey_beds.server.feature_disabled");
                return;
            }
            if (!BedSavedData.get(server).canAdd(recipient.getUUID(), serverConfig.maxBeds)) {
                notifyError(recipient, "dowwrey_beds.friend.accept_full");
                return;
            }

            BedSavedData recipientData = BedSavedData.get(server);
            if (recipientData.hasName(recipient.getUUID(), request.bedName())) {
                notifyError(recipient, "dowwrey_beds.friend.accept_name_taken");
                return;
            }

            ServerLevel level = getLevel(server, request.dimension());
            if (level == null || !(level.getBlockState(request.pos()).getBlock() instanceof BedBlock)) {
                requests.removeFor(recipient.getUUID(), request.id());
                notifyError(recipient, "dowwrey_beds.friend.accept_broken");
                sendGiftRequests(server, recipient);
                return;
            }

            if (recipientData.contains(recipient.getUUID(), level, request.pos())) {
                requests.removeFor(recipient.getUUID(), request.id());
                notifyError(recipient, "dowwrey_beds.friend.accept_duplicate_point");
                sendGiftRequests(server, recipient);
                return;
            }

            BedSavedData data = recipientData;
            data.add(recipient.getUUID(), request.bedName(), level, request.pos(), request.color(), request.icon(), server.overworld().getGameTime());
            requests.removeFor(recipient.getUUID(), request.id());
            notifySuccess(recipient, "dowwrey_beds.friend.accepted", request.bedName());
            ServerPlayer sender = server.getPlayerList().getPlayer(request.sender());
            if (sender != null) {
                notifySuccess(sender, "dowwrey_beds.friend.accepted_by", recipient.getGameProfile().name(), request.bedName());
            }
            ensureSelectedBed(server, recipient.getUUID());
            sendBedList(server, recipient, false);
            sendGiftRequests(server, recipient);
        });

        ServerPlayNetworking.registerGlobalReceiver(StartBedTeleportPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            if (!allowAction(server, player.getUUID())) return;
            BedTeleportService.begin(server, player, payload.bedId(), payload.mode());
        });

        ServerPlayNetworking.registerGlobalReceiver(RespawnAtBedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var server = context.server();
            if (!allowAction(server, player.getUUID())) return;

            if (!player.isDeadOrDying() || server.getWorldData().isHardcore()) {
                notifyError(player, "dowwrey_beds.respawn.unavailable");
                return;
            }

            BedSavedData data = BedSavedData.get(server);
            BedSavedData.SavedBed bed = data.findById(player.getUUID(), payload.bedId());
            if (bed == null) {
                notifyError(player, "dowwrey_beds.respawn.not_found");
                return;
            }

            ServerLevel targetLevel = getLevel(server, bed.dimension());
            if (targetLevel == null) {
                notifyError(player, "dowwrey_beds.respawn.unavailable");
                return;
            }

            BlockState bedState = targetLevel.getBlockState(bed.pos());
            if (!(bedState.getBlock() instanceof BedBlock)) {
                notifyError(player, "dowwrey_beds.respawn.broken");
                sendBedList(server, player, false);
                return;
            }

            Optional<net.minecraft.world.phys.Vec3> standPosition = findBedStandPosition(player, targetLevel, bed.pos(), bedState.getValue(HorizontalDirectionalBlock.FACING));
            if (standPosition.isEmpty()) {
                notifyError(player, "dowwrey_beds.respawn.no_space");
                return;
            }

            // Arm the normal vanilla respawn pipeline instead of constructing a replacement
            // ServerPlayer ourselves. This is important: vanilla also resets the connection's
            // waiting-for-respawn state, which keeps movement, interaction and /kill working
            // normally after the custom respawn.
            BedSelectionSavedData.get(server).setSelected(player.getUUID(), bed.id());
            player.setRespawnPosition(
                    new ServerPlayer.RespawnConfig(
                            net.minecraft.world.level.storage.LevelData.RespawnData.of(
                                    targetLevel.dimension(),
                                    bed.pos(),
                                    player.getYRot(),
                                    player.getXRot()
                            ),
                            false
                    ),
                    false
            );

            BedSavedData.get(server).markLastUsed(player.getUUID(), bed.id(), server.overworld().getGameTime());

            // Keep the client model in sync before the vanilla respawn command is processed.
            sendBedList(server, player, false);
            ServerPlayNetworking.send(player, new RespawnAtBedApprovedPayload(bed.id(), bed.name()));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            long nowTick = server.getTickCount();
            LIST_REQUEST_NEXT_ALLOWED.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
            FRIEND_REQUEST_NEXT_ALLOWED.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
            GIFT_LIST_NEXT_ALLOWED.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
            FRIEND_SELECT_NEXT_ALLOWED.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
            ACTION_NEXT_ALLOWED.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
            if (now % 600L == 0L) {
                BedSavedData.get(server).backupIfDue(server, now);
            }
            if (now % 20L != 0L) {
                return;
            }
            BedGiftRequestSavedData requests = BedGiftRequestSavedData.get(server);
            List<BedGiftRequestSavedData.Request> expired = requests.cleanupExpired(now);
            if (expired.isEmpty()) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendGiftRequests(server, player);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> BedSavedData.get(server).forceBackup(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            LIST_REQUEST_NEXT_ALLOWED.remove(uuid);
            FRIEND_REQUEST_NEXT_ALLOWED.remove(uuid);
            GIFT_LIST_NEXT_ALLOWED.remove(uuid);
            FRIEND_SELECT_NEXT_ALLOWED.remove(uuid);
            ACTION_NEXT_ALLOWED.remove(uuid);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            server.execute(() -> {
                sendGiftRequests(server, player);
                sendBedList(server, player, false);
            });
        });
    }

    private static Optional<net.minecraft.world.phys.Vec3> findBedStandPosition(
            ServerPlayer player, ServerLevel level, BlockPos bedPos, net.minecraft.core.Direction facing
    ) {
        var dimensions = player.getDimensions(net.minecraft.world.entity.Pose.STANDING);
        net.minecraft.core.Direction[] directions = {
                facing.getOpposite(),
                facing.getClockWise(),
                facing.getCounterClockWise(),
                facing
        };

        for (var direction : directions) {
            BlockPos standBlock = bedPos.relative(direction);
            net.minecraft.world.phys.Vec3 candidate = new net.minecraft.world.phys.Vec3(
                    standBlock.getX() + 0.5D,
                    standBlock.getY(),
                    standBlock.getZ() + 0.5D
            );
            var box = dimensions.makeBoundingBox(candidate);
            if (level.getFluidState(standBlock).isEmpty() && level.noCollision(player, box)) {
                return Optional.of(candidate);
            }

            BlockPos elevated = standBlock.above();
            net.minecraft.world.phys.Vec3 elevatedCandidate = new net.minecraft.world.phys.Vec3(
                    elevated.getX() + 0.5D,
                    elevated.getY(),
                    elevated.getZ() + 0.5D
            );
            if (level.getFluidState(elevated).isEmpty()
                    && level.noCollision(player, dimensions.makeBoundingBox(elevatedCandidate))) {
                return Optional.of(elevatedCandidate);
            }
        }

        return Optional.empty();
    }

    private static boolean allowAction(MinecraftServer server, UUID playerUuid) {
        long now = server.getTickCount();
        Long nextAllowed = ACTION_NEXT_ALLOWED.get(playerUuid);
        if (nextAllowed != null && nextAllowed > now) {
            return false;
        }
        ACTION_NEXT_ALLOWED.put(playerUuid, now + ACTION_COOLDOWN_TICKS);
        return true;
    }

    private static boolean validateName(ServerPlayer player, String name) {
        if (name.isEmpty()) {
            notifyError(player, "dowwrey_beds.save_bed.empty_name");
            return false;
        }
        if (name.length() > 32) {
            notifyError(player, "dowwrey_beds.save_bed.name_too_long");
            return false;
        }
        return true;
    }

    private static void ensureSelectedBed(net.minecraft.server.MinecraftServer server, UUID owner) {
        BedSelectionSavedData selection = BedSelectionSavedData.get(server);
        BedSavedData data = BedSavedData.get(server);
        UUID selected = selection.getSelected(owner);
        if (selected != null && data.findById(owner, selected) != null) {
            return;
        }
        List<BedSavedData.SavedBed> remaining = data.list(owner);
        if (!remaining.isEmpty()) {
            selection.setSelected(owner, remaining.getFirst().id());
        }
    }

    private static boolean withinRange(ServerPlayer player, BlockPos pos) {
        double dx = player.getX() - (pos.getX() + 0.5D);
        double dy = player.getY() - (pos.getY() + 0.5D);
        double dz = player.getZ() - (pos.getZ() + 0.5D);
        MinecraftServer server = player.level().getServer();
        int range = server == null ? 5 : BedServerConfig.get(server).interactionRangeBlocks;
        return ((dx * dx) + (dy * dy) + (dz * dz)) <= (double) range * range;
    }

    private static ServerLevel getLevel(net.minecraft.server.MinecraftServer server, String dimension) {
        try {
            Identifier dimensionId = Identifier.parse(dimension);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            return server.getLevel(dimensionKey);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isBedBroken(net.minecraft.server.MinecraftServer server, BedSavedData.SavedBed bed) {
        ServerLevel level = getLevel(server, bed.dimension());
        return level == null || !(level.getBlockState(bed.pos()).getBlock() instanceof BedBlock);
    }

    public static void refreshBedList(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
        sendBedList(server, player, false);
    }

    private static void sendBedList(net.minecraft.server.MinecraftServer server, ServerPlayer player, boolean openManager) {
        BedSavedData data = BedSavedData.get(server);
        BedServerConfig rules = BedServerConfig.get(server);
        BedSelectionSavedData selection = BedSelectionSavedData.get(server);
        UUID active = selection.getSelected(player.getUUID());
        String playerDimension = player.level().dimension().identifier().toString();
        List<BedSavedData.SavedBed> ownedBeds = data.list(player.getUUID());

        List<BedListPayload.BedEntry> entries = new ArrayList<>(ownedBeds.size());
        for (BedSavedData.SavedBed bed : ownedBeds) {
            boolean sameDimension = playerDimension.equals(bed.dimension());
            ServerLevel bedLevel = getLevel(server, bed.dimension());
            boolean chunkLoaded = bedLevel != null && bedLevel.getChunkSource().hasChunk(bed.pos().getX() >> 4, bed.pos().getZ() >> 4);
            boolean broken = false;
            int status;
            if (bedLevel == null || !chunkLoaded) {
                status = BedListPayload.BedEntry.STATUS_NOT_LOADED;
            } else {
                broken = !(bedLevel.getBlockState(bed.pos()).getBlock() instanceof BedBlock);
                if (broken) {
                    status = BedListPayload.BedEntry.STATUS_BROKEN;
                } else if (!sameDimension) {
                    status = BedListPayload.BedEntry.STATUS_OTHER_DIMENSION;
                } else {
                    status = com.dowwrey.beds.teleport.BedTeleportService.hasSafeDestination(player, bedLevel, bed.pos(), false)
                            ? BedListPayload.BedEntry.STATUS_AVAILABLE
                            : BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE;
                }
            }
            int distanceBlocks = sameDimension && !broken && status != BedListPayload.BedEntry.STATUS_NOT_LOADED
                    ? (int) Math.min(Integer.MAX_VALUE, Math.round(player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(bed.pos()))))
                    : -1;
            entries.add(new BedListPayload.BedEntry(
                    bed.id(),
                    bed.name(),
                    bed.dimension(),
                    bed.pos(),
                    bed.id().equals(active),
                    broken,
                    bed.color(),
                    bed.icon(),
                    bed.createdAtGameTime(),
                    bed.lastUsedGameTime(),
                    status,
                    distanceBlocks
            ));
        }
        ServerPlayNetworking.send(player, new BedListPayload(entries, openManager));
        ServerPlayNetworking.send(player, new BedServerRulesPayload(
                rules.maxBeds,
                rules.friendBedsEnabled,
                rules.teleportEnabled,
                rules.brokenBedTeleportEnabled,
                rules.teleportCooldownSeconds,
                rules.slowTeleportSeconds,
                rules.slowTeleportCost,
                rules.fastTeleportSeconds,
                rules.fastTeleportCost,
                rules.instantTeleportSeconds,
                rules.instantTeleportCost,
                rules.brokenTeleportSeconds,
                rules.brokenTeleportCost
        ));
        BedTeleportService.syncCooldown(server, player);
    }

    private static void sendGiftRequests(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
        BedGiftRequestSavedData data = BedGiftRequestSavedData.get(server);
        long now = server.overworld().getGameTime();
        data.cleanupExpired(now);
        List<BedGiftRequestsPayload.RequestEntry> entries = data.listFor(player.getUUID()).stream()
                .map(request -> {
                    long remainingTicks = request.expiresAtGameTime() <= 0L
                            ? 0L
                            : Math.max(0L, request.expiresAtGameTime() - now);
                    int remainingSeconds = remainingTicks <= 0L ? 0 : (int) Math.ceil(remainingTicks / 20.0D);
                    return new BedGiftRequestsPayload.RequestEntry(request.id(), request.senderName(), request.bedName(), request.color(), request.icon(), remainingSeconds);
                })
                .toList();
        ServerPlayNetworking.send(player, new BedGiftRequestsPayload(entries));
    }

    public static void openSaveScreen(ServerPlayer player, BlockPos pos) {
        ServerPlayNetworking.send(player, new OpenSaveBedPayload(pos));
    }

    public static void notifyInfo(ServerPlayer player, String translationKey, String... args) {
        sendNotification(player, translationKey, 0, args);
    }

    public static void notifySuccess(ServerPlayer player, String translationKey, String... args) {
        sendNotification(player, translationKey, 1, args);
    }

    public static void notifyError(ServerPlayer player, String translationKey, String... args) {
        sendNotification(player, translationKey, 2, args);
    }

    private static void sendNotification(ServerPlayer player, String translationKey, int style, String... args) {
        String arg1 = args.length > 0 ? args[0] : "";
        String arg2 = args.length > 1 ? args[1] : "";
        ServerPlayNetworking.send(player, new BedNotificationPayload(translationKey, arg1, arg2, style));
    }
}
