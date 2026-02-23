package moe.caramel.chat.mixin;

import moe.caramel.chat.controller.EditBoxController;
import moe.caramel.chat.wrapper.AbstractIMEWrapper;
import moe.caramel.chat.wrapper.WrapperEditBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RecipeBook Component Mixin
 */
@Mixin(RecipeBookComponent.class)
public abstract class MixinRecipeBookComponent {

    @Shadow @Nullable private EditBox searchBox;
    @Shadow private void checkSearchStringUpdate() { throw new AssertionError(); }

    @Inject(method = "initVisuals", at = @At("TAIL"))
    private void initVisuals(final CallbackInfo ci) {
        final WrapperEditBox wrapper = EditBoxController.getWrapper(this.searchBox);
        wrapper.setInsertCallback(() -> {
            if (wrapper.getStatus() == AbstractIMEWrapper.InputStatus.PREVIEW) {
                this.checkSearchStringUpdate();
            }
        });
    }
}
