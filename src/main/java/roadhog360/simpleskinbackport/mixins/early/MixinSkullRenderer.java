package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.model.ModelSkeletonHead;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import roadhog360.simpleskinbackport.client.SkinLayerRenderContext;
import roadhog360.simpleskinbackport.core.DefaultPlayerSkin;

@Mixin(TileEntitySkullRenderer.class)
public abstract class MixinSkullRenderer {

    @Shadow
    private ModelSkeletonHead field_147538_h;

    @WrapOperation(
        method = "func_152674_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ModelSkeletonHead;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private void ssb$playerHeadModel(ModelSkeletonHead model, Entity entity, float limbSwing, float limbAmount,
        float age, float yaw, float pitch, float scale, Operation<Void> original,
        @Local(argsOnly = true, ordinal = 1) int skullType) {
        boolean modernSkin = skullType == 3
            && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) == 64;
        original.call(modernSkin ? field_147538_h : model, entity, limbSwing, limbAmount, age, yaw, pitch, scale);
    }

    @Redirect(
        method = "func_152674_a",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/entity/AbstractClientPlayer;locationStevePng:Lnet/minecraft/util/ResourceLocation;"))
    private ResourceLocation ssb$modernDefaultHead() {
        return DefaultPlayerSkin.STEVE.getResource();
    }

    @WrapMethod(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySkull;DDDF)V")
    private void ssb$placedHeadLayers(TileEntitySkull skull, double x, double y, double z, float partialTick,
        Operation<Void> original) {
        SkinLayerRenderContext.at(x, y, z, () -> original.call(skull, x, y, z, partialTick));
    }
}
