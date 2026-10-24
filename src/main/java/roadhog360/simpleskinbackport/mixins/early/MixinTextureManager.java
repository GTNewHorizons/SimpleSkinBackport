package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import roadhog360.simpleskinbackport.client.SkinLayers;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager {

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void ssb$reloadSkins(IResourceManager resources, CallbackInfo ci) {
        SkinLayers.clear();
    }
}
