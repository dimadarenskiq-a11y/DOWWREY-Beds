package com.dowwrey.beds;

import com.dowwrey.beds.network.BedNetworking;
import com.dowwrey.beds.teleport.BedTeleportService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DowwreyBeds implements ModInitializer {
    public static final String MOD_ID = "dowwrey_beds";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BedNetworking.registerCommon();
        BedTeleportService.register();

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            if (!player.isAlive() || player.isSpectator()) {
                return InteractionResult.PASS;
            }

            if (!(level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock)) {
                return InteractionResult.PASS;
            }

            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                BedNetworking.openSaveScreen(serverPlayer, hitResult.getBlockPos());
            }

            return InteractionResult.SUCCESS;
        });

        LOGGER.info("DOWWREY Beds initialized");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}
