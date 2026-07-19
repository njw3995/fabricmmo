package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.diagnostic.UiTraceLogger;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Temporary trace hook for command feedback sent through a server command source. */
@Mixin(ServerCommandSource.class)
abstract class ServerCommandSourceUiTraceMixin {
    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void fabricmmo$traceCommandMessageStart(Text message, CallbackInfo callback) {
        ServerPlayerEntity player = ((ServerCommandSource) (Object) this).getPlayer();
        if (player != null) {
            UiTraceLogger.beginCommandMessage(player, message);
        }
    }

    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void fabricmmo$traceCommandMessageEnd(Text message, CallbackInfo callback) {
        if (((ServerCommandSource) (Object) this).getPlayer() != null) {
            UiTraceLogger.endCommandMessage();
        }
    }
}
