package me.jellysquid.mods.sodium.mixin.features.options;

import me.flashyreese.mods.reeses_sodium_options.client.gui.ReeseSodiumVideoOptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SettingsScreen.class)
public class MixinSettingsScreen extends Screen {
    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void open(ButtonWidget button, CallbackInfo ci) {
        if(button.active && button.id == 101) {
            this.client.setScreen(new ReeseSodiumVideoOptionsScreen(this));

            ci.cancel();
        }
    }
}
