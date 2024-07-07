package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.text.TranslatableText;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableText("options.guiScale.auto").asFormattedString() : new TranslatableText(v + "x").asFormattedString();
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> {
            if (v == 260) return new TranslatableText("options.framerateLimit.max").asFormattedString();
            else return v + " FPS";
        };
    }


    static ControlValueFormatter chunks() {
        return (v) -> {
            if (v < 4) {
                return new TranslatableText("options.renderDistance.tiny").asFormattedString();
            } else if (v < 8) {
                return new TranslatableText("options.renderDistance.short").asFormattedString();
            } else if (v < 16) {
                return new TranslatableText("options.renderDistance.normal").asFormattedString();
            } else if (v <= 24) {
                return new TranslatableText("options.renderDistance.far").asFormattedString();
            }
            return v + " chunks";
        };
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableText("options.gamma.min").asFormattedString();
            } else if (v == 100) {
                return new TranslatableText("options.gamma.max").asFormattedString();
            } else {
                return v + "%";
            }
        };
    }

    String format(int value);

    static ControlValueFormatter percentage() {
        return (v) -> v + "%";
    }

    static ControlValueFormatter blocks() {
        return (v) -> v + " blocks";
    }

    static ControlValueFormatter multiplier() {
        return (v) -> new TranslatableText(v + "x").asFormattedString();
    }

    static ControlValueFormatter quantity(String name) {
        return (v) -> new TranslatableText(name, v).asFormattedString();
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> new TranslatableText(v == 0 ? disableText : name, v).asFormattedString();
    }

    static ControlValueFormatter number() {
        return String::valueOf;
    }
}
