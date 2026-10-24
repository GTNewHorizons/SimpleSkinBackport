package roadhog360.simpleskinbackport.mixins.early;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import roadhog360.simpleskinbackport.client.SkinLayerRenderContext;

@Mixin(GuiInventory.class)
public abstract class MixinGuiInventory {

    @WrapMethod(method = "func_147046_a")
    private static void ssb$inventoryLayers(int x, int y, int scale, float mouseX, float mouseY,
        EntityLivingBase entity, Operation<Void> original) {
        SkinLayerRenderContext.near(() -> original.call(x, y, scale, mouseX, mouseY, entity));
    }
}
