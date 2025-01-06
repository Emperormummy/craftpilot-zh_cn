package mmmfrieddough.craftpilot.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.EntityAttributes;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @SuppressWarnings("resource")
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        Camera camera = client.gameRenderer.getCamera();

        // Get look vector
        Vec3d cameraPos = camera.getPos();
        float reach = (float) client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        Vec3d lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        Vec3d endPos = cameraPos.add(lookVec.multiply(reach));

        // Do our own raytrace
        IWorldManager manager = CraftPilot.getInstance().getWorldManager();
        BlockPos nearestPos = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : manager.getGhostBlocks().keySet()) {
            Box box = new Box(pos);
            Optional<Vec3d> hit = box.raycast(cameraPos, endPos);
            if (hit.isPresent()) {
                double dist = cameraPos.squaredDistanceTo(hit.get());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestPos = pos;
                }
            }
        }

        if (nearestPos != null) {
            BlockState ghostState = manager.getGhostBlocks().get(nearestPos);
            ItemStack stack = ghostState.getBlock().getPickStack(client.world, nearestPos, ghostState);
            if (!stack.isEmpty()) {
                PlayerInventory inventory = client.player.getInventory();
                if (client.player.getAbilities().creativeMode) {
                    inventory.addPickBlock(stack);
                    client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND),
                            36 + inventory.selectedSlot);
                } else {
                    int slot = inventory.getSlotWithStack(stack);
                    if (slot != -1) {
                        if (PlayerInventory.isValidHotbarIndex(slot)) {
                            inventory.selectedSlot = slot;
                        } else {
                            client.interactionManager.pickFromInventory(slot);
                        }
                    }
                }
                ci.cancel();
            }
        }
    }

    @SuppressWarnings("resource")
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        Vec3d lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        Vec3d endPos = cameraPos.add(lookVec.multiply(reach));

        // Do raytrace for ghost blocks
        IWorldManager manager = CraftPilot.getInstance().getWorldManager();
        BlockPos nearestPos = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos pos : manager.getGhostBlocks().keySet()) {
            Box box = new Box(pos);
            Optional<Vec3d> hit = box.raycast(cameraPos, endPos);
            if (hit.isPresent()) {
                double dist = cameraPos.squaredDistanceTo(hit.get());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestPos = pos;
                }
            }
        }

        // Remove the ghost block if we hit one
        if (nearestPos != null) {
            manager.clearBlockState(nearestPos);
            client.player.swingHand(Hand.MAIN_HAND);
            cir.setReturnValue(true);
        }
    }
}