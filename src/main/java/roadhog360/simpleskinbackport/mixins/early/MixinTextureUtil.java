package roadhog360.simpleskinbackport.mixins.early;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.texture.TextureUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import roadhog360.simpleskinbackport.client.SkinLayers;

@Mixin(TextureUtil.class)
public abstract class MixinTextureUtil {

    @Inject(method = "uploadTextureImageSub", at = @At("RETURN"))
    private static void ssb$skinUploaded(int texture, BufferedImage image, int x, int y, boolean blur, boolean clamp,
        CallbackInfoReturnable<Integer> cir) {
        SkinLayers.upload(texture, image, x, y);
    }

    @Inject(method = "deleteTexture", at = @At("HEAD"))
    private static void ssb$textureDeleted(int texture, CallbackInfo ci) {
        SkinLayers.invalidate(texture);
    }

    @Inject(method = "uploadTexture", at = @At("RETURN"))
    private static void ssb$dynamicSkinUploaded(int texture, int[] pixels, int width, int height, CallbackInfo ci) {
        SkinLayers.invalidate(texture);
    }

    @Inject(method = "uploadTextureMipmap", at = @At("RETURN"))
    private static void ssb$mipmapUploaded(int[][] pixels, int width, int height, int x, int y, boolean blur,
        boolean clamp, CallbackInfo ci) {
        SkinLayers.invalidate(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D));
    }
}
