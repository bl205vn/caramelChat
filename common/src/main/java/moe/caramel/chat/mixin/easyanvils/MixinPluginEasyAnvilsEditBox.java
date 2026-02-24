package moe.caramel.chat.mixin.easyanvils;

import fuzs.easyanvils.client.gui.components.FormattableEditBox;
import moe.caramel.chat.controller.EditBoxController;
import moe.caramel.chat.wrapper.WrapperEditBox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * (Easy Anvils Mixin) Restore IME hooks for FormattableEditBox.
 *
 * <p>
 * FormattableEditBox overrides setValue/insertText without calling super,
 * so MixinEditBox's hooks on those methods are bypassed. However,
 * MixinEditBox's onValueChange HEAD hook still fires (inherited from EditBox)
 * and sets {@code this.value = wrapper.getOrigin()}, causing crashes if
 * origin is stale or corrupted.
 * </p>
 *
 * <p>
 * We must replicate MixinEditBox's key behaviors:
 * </p>
 * <ul>
 * <li>In setValue: when valueChanged (preview), cache cursor and cancel the
 * moveCursorToEnd + onValueChange section. When not preview, update origin
 * before onValueChange.</li>
 * <li>In insertText: update origin before onValueChange.</li>
 * </ul>
 */
@Mixin(FormattableEditBox.class)
public abstract class MixinPluginEasyAnvilsEditBox extends EditBox {

    @Unique
    private int caramelChat$cacheCursorPos, caramelChat$cacheHighlightPos;

    private MixinPluginEasyAnvilsEditBox() {
        super(null, 0, 0, null);
    }

    // ================================ (setValue hooks)

    @Inject(method = "setValue", at = @At("HEAD"))
    private void caramelChat$setValueHead(final String text, final CallbackInfo ci) {
        final WrapperEditBox wrapper = EditBoxController.getWrapper((EditBox) (Object) this);
        if (wrapper != null) {
            if (wrapper.valueChanged) {
                // Preview insertion: cache current cursor positions before setValue overwrites
                // them
                this.caramelChat$cacheCursorPos = this.cursorPos;
                this.caramelChat$cacheHighlightPos = this.highlightPos;
            } else {
                wrapper.setToNoneStatus();
            }
        }
    }

    @Inject(method = "setValue", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lfuzs/easyanvils/client/gui/components/FormattableEditBox;moveCursorToEnd(Z)V"), cancellable = true)
    private void caramelChat$setValueBeforeMoveCursor(final String text, final CallbackInfo ci) {
        final WrapperEditBox wrapper = EditBoxController.getWrapper((EditBox) (Object) this);
        if (wrapper == null)
            return;

        if (wrapper.valueChanged) {
            // During preview: cancel moveCursorToEnd + setHighlightPos + onValueChange,
            // restore cached cursor, clear the flag.
            ci.cancel();
            this.cursorPos = Mth.clamp(this.caramelChat$cacheCursorPos, 0, this.value.length());
            this.highlightPos = Mth.clamp(this.caramelChat$cacheHighlightPos, 0, this.value.length());
            this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
            wrapper.valueChanged = false;
        } else {
            // Normal setValue: update origin before onValueChange fires
            wrapper.setOrigin(this.value);
        }
    }

    // ================================ (insertText hooks)

    @Inject(method = "insertText", at = @At("HEAD"))
    private void caramelChat$insertTextHead(final String text, final CallbackInfo ci) {
        final WrapperEditBox wrapper = EditBoxController.getWrapper((EditBox) (Object) this);
        if (wrapper != null) {
            wrapper.setToNoneStatus();
        }
    }

    @Inject(method = "insertText", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lfuzs/easyanvils/client/gui/components/FormattableEditBox;onValueChange(Ljava/lang/String;)V"))
    private void caramelChat$insertTextBeforeOnValueChange(final String text, final CallbackInfo ci) {
        final WrapperEditBox wrapper = EditBoxController.getWrapper((EditBox) (Object) this);
        if (wrapper != null) {
            wrapper.setOrigin(this.value);
        }
    }
}
