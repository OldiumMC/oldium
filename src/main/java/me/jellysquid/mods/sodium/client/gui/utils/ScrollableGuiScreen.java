package me.jellysquid.mods.sodium.client.gui.utils;

import org.lwjgl.input.Mouse;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;

public abstract class ScrollableGuiScreen extends Screen {
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        handleMouseScroll(mouseX, mouseY, partialTicks);
    }

    public abstract List<? extends Element> children();

    protected void handleMouseScroll(int mouseX, int mouseY, float partialTicks) {
        for (; !this.client.options.touchscreen && Mouse.next(); this.client.currentScreen.handleMouse()) {
            int dWheel = Mouse.getEventDWheel();

            if (dWheel != 0) {
                for(Element child : children()) {
                    child.mouseScrolled(mouseX, mouseY, dWheel);
                }
            }
        }
    }
}