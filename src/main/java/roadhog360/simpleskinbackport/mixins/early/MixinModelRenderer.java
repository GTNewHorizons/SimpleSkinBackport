package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import roadhog360.simpleskinbackport.client.SkinLayers;
import roadhog360.simpleskinbackport.ducks.ISkinLayer;

@Mixin(ModelRenderer.class)
public abstract class MixinModelRenderer implements ISkinLayer {

    @Shadow
    private ModelBase baseModel;

    @Unique
    private SkinLayers.Part ssb$skinLayer;

    @Override
    public boolean ssb$isBipedPart() {
        return baseModel instanceof ModelBiped;
    }

    @Override
    public void ssb$setSkinLayer(SkinLayers.Part part) {
        ssb$skinLayer = part;
    }

    @WrapOperation(
        method = { "render", "renderWithRotation" },
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false))
    private void ssb$drawSkinLayer(int displayList, Operation<Void> original, float scale) {
        if (ssb$skinLayer == null || !SkinLayers.render((ModelRenderer) (Object) this, ssb$skinLayer, scale)) {
            original.call(displayList);
        }
    }
}
