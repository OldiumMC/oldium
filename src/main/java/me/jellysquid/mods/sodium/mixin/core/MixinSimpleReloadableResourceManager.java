package me.jellysquid.mods.sodium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.util.List;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;

@Mixin(TranslationStorage.class)
public abstract class MixinSimpleReloadableResourceManager {
    @Shadow
    protected abstract void load(InputStream stream);

    @Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V", at = @At("RETURN"))
    public void oldium$loadLanguage(ResourceManager manager, List<String> locales, CallbackInfo ci) {
        InputStream stream = TranslationStorage.class.getResourceAsStream("/assets/sodium/lang/en_us.lang");
        load(stream);
    }
}
