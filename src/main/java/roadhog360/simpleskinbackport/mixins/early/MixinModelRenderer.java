package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import roadhog360.simpleskinbackport.client.SkinLayers;
import roadhog360.simpleskinbackport.ducks.ISkinLayer;

@Mixin(ModelRenderer.class)
public abstract class MixinModelRenderer implements ISkinLayer {

    @Unique
    private SkinLayers.Part ssb$skinLayer;

    @Override
    public void ssb$setSkinLayer(SkinLayers.Part part) {
        ssb$skinLayer = part;
    }

    @WrapOperation(
        method = { "render", "renderWithRotation" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false))
    private void ssb$drawSkinLayer(int displayList, Operation<Void> original, @Local(argsOnly = true) float scale) {
        if (ssb$skinLayer == null || !SkinLayers.render((ModelRenderer) (Object) this, ssb$skinLayer, scale)) {
            original.call(displayList);
        }
    }
}
