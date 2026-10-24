package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import roadhog360.simpleskinbackport.client.SkinLayerRenderContext;

@Mixin(RendererLivingEntity.class)
public abstract class MixinLivingRenderer {

    @Shadow
    protected ModelBase mainModel;

    @WrapMethod(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V")
    private void ssb$renderLayers(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTick,
        Operation<Void> original) {
        ModelRenderer headwear = mainModel instanceof ModelBiped ? ((ModelBiped) mainModel).bipedHeadwear : null;
        boolean visible = headwear != null && headwear.showModel;
        try {
            SkinLayerRenderContext.at(x, y, z, () -> original.call(entity, x, y, z, yaw, partialTick));
        } finally {
            // Equipment visibility belongs to one render, including cancelled and nested renders.
            if (headwear != null) headwear.showModel = visible;
        }
    }
}
