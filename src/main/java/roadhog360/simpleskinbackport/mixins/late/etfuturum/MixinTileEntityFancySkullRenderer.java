package roadhog360.simpleskinbackport.mixins.late.etfuturum;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import ganymedes01.etfuturum.client.model.ModelHead;
import ganymedes01.etfuturum.client.renderer.tileentity.TileEntityFancySkullRenderer;
import roadhog360.simpleskinbackport.core.DefaultPlayerSkin;

/**
 * Et Futurum replaces the vanilla skull renderer (placed and held heads) and always draws player heads with its 64x32
 * model. Skull skins are 64x64 now, so use its 64x64 model for them, like {@code MixinSkullRenderer} does for vanilla.
 */
@Mixin(value = TileEntityFancySkullRenderer.class, remap = false)
public class MixinTileEntityFancySkullRenderer {

    @Shadow
    @Final
    private ModelHead model2;

    @WrapOperation(
        method = "renderSkull",
        at = @At(value = "INVOKE", target = "Lganymedes01/etfuturum/client/model/ModelHead;render(F)V"))
    private void ssb$playerHeadModel(ModelHead model, float rotation, Operation<Void> original,
        @Local(argsOnly = true, ordinal = 1) int type) {
        boolean modernSkin = type == 3
            && GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT) == 64;
        original.call(modernSkin ? model2 : model, rotation);
    }

    @Redirect(
        method = "renderSkull",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/entity/AbstractClientPlayer;locationStevePng:Lnet/minecraft/util/ResourceLocation;",
            remap = true))
    private ResourceLocation ssb$modernDefaultHead() {
        return DefaultPlayerSkin.STEVE.getResource();
    }
}
