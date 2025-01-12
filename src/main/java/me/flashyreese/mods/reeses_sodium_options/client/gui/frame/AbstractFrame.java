package me.flashyreese.mods.reeses_sodium_options.client.gui.frame;

import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.utils.Drawable;
import me.jellysquid.mods.sodium.client.gui.utils.Element;
import me.jellysquid.mods.sodium.client.gui.utils.ParentElement;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractFrame extends AbstractWidget implements ParentElement {
    protected final Dim2i dim;
    protected final List<AbstractWidget> children = new ArrayList<>();
    protected final List<Drawable> drawable = new ArrayList<>();
    protected final List<ControlElement<?>> controlElements = new ArrayList<>();
    protected boolean renderOutline;
    private Element focused;
    private boolean dragging;

    public AbstractFrame(Dim2i dim, boolean renderOutline) {
        this.dim = dim;
        this.renderOutline = renderOutline;
    }

    public void buildFrame() {
        for (Element element : this.children) {
            if (element instanceof AbstractFrame) {
                AbstractFrame abstractFrame = (AbstractFrame)element;
                this.controlElements.addAll(abstractFrame.controlElements);
            }
            if (element instanceof ControlElement<?>) {
                ControlElement<?> controlElement = (ControlElement<?>)element;
                this.controlElements.add(controlElement);
            }
            if (element instanceof Drawable) {
                Drawable drawable = (Drawable)element;
                this.drawable.add(drawable);
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        if (this.renderOutline) {
            this.drawRectOutline(this.dim.getOriginX(), this.dim.getOriginY(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        }
        for (Drawable drawable : this.drawable) {
            drawable.render(mouseX, mouseY, delta);
        }
    }

    public void applyScissor(int x, int y, int width, int height, Runnable action) {
        final double scale = new Window(MinecraftClient.getInstance()).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (MinecraftClient.getInstance().getFramebuffer().viewportHeight - (y + height) * scale), (int) (width * scale), (int) (height * scale));
        action.run();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    protected void drawRectOutline(double x, double y, double w, double h, int color) {
        final float a = (float) (color >> 24 & 255) / 255.0F;
        final float r = (float) (color >> 16 & 255) / 255.0F;
        final float g = (float) (color >> 8 & 255) / 255.0F;
        final float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> {
            addQuad(vertices, x, y, w, y + 1, a, r, g, b);
            addQuad(vertices, x, h - 1, w, h, a, r, g, b);
            addQuad(vertices, x, y, x + 1, h, a, r, g, b);
            addQuad(vertices, w - 1, y, w, h, a, r, g, b);
        });
    }


    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Nullable
    @Override
    public Element getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        this.focused = focused;
    }

    @Override
    public List<? extends Element> children() {
        return this.children;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

}
