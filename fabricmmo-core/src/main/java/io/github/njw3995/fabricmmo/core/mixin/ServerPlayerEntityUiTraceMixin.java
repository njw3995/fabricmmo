package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.diagnostic.UiTraceLogger;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Temporary trace hook for every player-directed chat or action-bar message. */
@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityUiTraceMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void fabricmmo$traceUiMessage(Text message, boolean overlay, CallbackInfo callback) {
        UiTraceLogger.playerMessage((ServerPlayerEntity) (Object) this, message, overlay);
    }
}
