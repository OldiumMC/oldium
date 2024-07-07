package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public enum OptionImpact {
    LOW(Formatting.GREEN, new TranslatableText("sodium.option_impact.low").asFormattedString()),
    MEDIUM(Formatting.YELLOW, new TranslatableText("sodium.option_impact.medium").asFormattedString()),
    HIGH(Formatting.GOLD, new TranslatableText("sodium.option_impact.high").asFormattedString()),
    EXTREME(Formatting.RED, new TranslatableText("sodium.option_impact.extreme").asFormattedString()),
    VARIES(Formatting.WHITE, new TranslatableText("sodium.option_impact.varies").asFormattedString());

    private final Formatting color;
    private final String text;

    OptionImpact(Formatting color, String text) {
        this.color = color;
        this.text = text;
    }

    public String toDisplayString() {
        return this.color + this.text;
    }
}
