package mmmfrieddough.craftpilot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;

/**
 * Mixin to handle ghost block interactions in the Minecraft client.
 * Provides functionality for picking, breaking, and placing ghost blocks.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow
    private static MinecraftClient instance;
    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        // Extract necessary information
        FeatureSet enabledFeatures = instance.player.getWorld().getEnabledFeatures();
        boolean creativeMode = instance.interactionManager.getCurrentGameMode().isCreative();
        PlayerInventory inventory = instance.player.getInventory();
        ClientPlayNetworkHandler networkHandler = instance.getNetworkHandler();
        ScreenHandler screenHandler = instance.player.currentScreenHandler;

        if (GhostBlockService.handleGhostBlockPick(enabledFeatures, creativeMode, inventory, networkHandler,
                screenHandler)) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        // Extract necessary information
        IWorldManager worldManager = CraftPilot.getInstance().getWorldManager();
        ClientPlayerEntity player = instance.player;

        if (GhostBlockService.handleGhostBlockBreak(worldManager, player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        ModConfig config = CraftPilot.getConfig();
        if (!config.general.enableEasyPlace) {
            return;
        }

        if (GhostBlockService.handleGhostBlockPlace(instance)) {
            this.itemUseCooldown = 4;
            ci.cancel();
        }
    }
}