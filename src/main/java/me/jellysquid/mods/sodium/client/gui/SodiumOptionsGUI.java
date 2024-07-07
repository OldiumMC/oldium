package me.jellysquid.mods.sodium.client.gui;

import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.utils.Drawable;
import me.jellysquid.mods.sodium.client.gui.utils.Element;
import me.jellysquid.mods.sodium.client.gui.utils.ScrollableGuiScreen;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class SodiumOptionsGUI extends ScrollableGuiScreen {

    protected final List<Element> children = new CopyOnWriteArrayList<>();

    protected final List<OptionPage> pages = new ArrayList<>();

    protected final List<ControlElement<?>> controls = new ArrayList<>();
    protected final List<Drawable> drawable = new ArrayList<>();

    public final Screen prevScreen;

    protected OptionPage currentPage;

    protected FlatButtonWidget applyButton, closeButton, undoButton;

    protected boolean hasPendingChanges;
    protected ControlElement<?> hoveredElement;

    public SodiumOptionsGUI(Screen prevScreen) {
        super();

        this.prevScreen = prevScreen;

        this.pages.add(SodiumGameOptionPages.general());
        this.pages.add(SodiumGameOptionPages.quality());
        this.pages.add(SodiumGameOptionPages.advanced());
        this.pages.add(SodiumGameOptionPages.performance());
    }

    public void setPage(OptionPage page) {
        this.currentPage = page;

        this.rebuildGUI();
    }

    @Override
    public void init() {
        super.init();

        this.rebuildGUI();
    }

    protected void rebuildGUI() {
        this.controls.clear();
        this.children.clear();
        this.drawable.clear();

        if (this.currentPage == null) {
            if (this.pages.isEmpty()) {
                throw new IllegalStateException("No pages are available?!");
            }

            // Just use the first page for now
            this.currentPage = this.pages.get(0);
        }

        this.rebuildGUIPages();
        this.rebuildGUIOptions();

        this.undoButton = new FlatButtonWidget(new Dim2i(this.width - 211, this.height - 26, 65, 20), new TranslatableText("sodium.options.buttons.undo").asFormattedString(), this::undoChanges);
        this.applyButton = new FlatButtonWidget(new Dim2i(this.width - 142, this.height - 26, 65, 20), new TranslatableText("sodium.options.buttons.apply").asFormattedString(), this::applyChanges);
        this.closeButton = new FlatButtonWidget(new Dim2i(this.width - 73, this.height - 26, 65, 20), new TranslatableText("gui.done").asFormattedString(), this::onClose);

        this.children.add(this.undoButton);
        this.children.add(this.applyButton);
        this.children.add(this.closeButton);

        for (Element element : this.children) {
            if (element instanceof Drawable) {
                this.drawable.add((Drawable) element);
            }
        }
    }

    private void rebuildGUIPages() {
        int x = 6;
        int y = 6;

        for (OptionPage page : this.pages) {
            int width = 12 + this.textRenderer.getStringWidth(page.getNewName().asFormattedString());

            FlatButtonWidget button = new FlatButtonWidget(new Dim2i(x, y, width, 18), page.getNewName(), () -> this.setPage(page));
            button.setSelected(this.currentPage == page);

            x += width + 6;

            this.children.add(button);
        }
    }

    private void rebuildGUIOptions() {
        int x = 6;
        int y = 28;

        for (OptionGroup group : this.currentPage.getGroups()) {
            // Add each option's control element
            for (Option<?> option : group.getOptions()) {
                Control<?> control = option.getControl();
                ControlElement<?> element = control.createElement(new Dim2i(x, y, 200, 18));

                this.controls.add(element);
                this.children.add(element);

                // Move down to the next option
                y += 18;
            }

            // Add padding beneath each option group
            y += 4;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        super.renderBackground();

        this.updateControls();

        for (Drawable drawable : this.drawable) {
            drawable.render(mouseX, mouseY, delta);
        }

        if (this.hoveredElement != null) {
            this.renderOptionTooltip(this.hoveredElement);
        }
    }

    @Override
    public List<? extends Element> children() {
        return children;
    }

    private void updateControls() {
        ControlElement<?> hovered = this.getActiveControls()
                .filter(ControlElement::isHovered)
                .findFirst()
                .orElse(null);

        boolean hasChanges = this.getAllOptions()
                .anyMatch(Option::hasChanged);

        for (OptionPage page : this.pages) {
            for (Option<?> option : page.getOptions()) {
                if (option.hasChanged()) {
                    hasChanges = true;
                }
            }
        }

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        this.hasPendingChanges = hasChanges;
        this.hoveredElement = hovered;
    }

    private Stream<Option<?>> getAllOptions() {
        return this.pages.stream()
                .flatMap(s -> s.getOptions().stream());
    }

    private Stream<ControlElement<?>> getActiveControls() {
        return this.controls.stream();
    }

    private void renderOptionTooltip(ControlElement<?> element) {
        Dim2i dim = element.getDimensions();

        int textPadding = 3;
        int boxPadding = 3;

        int boxWidth = 200;

        int boxY = dim.getOriginY();
        int boxX = dim.getLimitX() + boxPadding;

        Option<?> option = element.getOption();
        List<String> tooltip = new ArrayList<>(this.textRenderer.wrapLines(option.getTooltip().asFormattedString(), boxWidth - (textPadding * 2)));

        OptionImpact impact = option.getImpact();

        if (impact != null) {
            tooltip.add(Formatting.GRAY + I18n.translate("sodium.options.performance_impact_string", impact.toDisplayString()));
        }

        int boxHeight = (tooltip.size() * 12) + boxPadding;
        int boxYLimit = boxY + boxHeight;
        int boxYCutoff = this.height - 40;

        // If the box is going to be cutoff on the Y-axis, move it back up the difference
        if (boxYLimit > boxYCutoff) {
            boxY -= boxYLimit - boxYCutoff;
        }

        this.fillGradient(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xE0000000, 0xE0000000);

        for (int i = 0; i < tooltip.size(); i++) {
            this.textRenderer.draw(tooltip.get(i), boxX + textPadding, boxY + textPadding + (i * 12), 0xFFFFFFFF);
        }
    }
    
    private void applyChanges() {
        final HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
        final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);

        this.getAllOptions().forEach((option -> {
            if (!option.hasChanged()) {
                return;
            }

            option.applyChanges();

            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
        }));

        if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {    	
            this.client.worldRenderer.reload();
        }

        if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
            this.client.getSpriteAtlasTexture().setMaxTextureSize(this.client.options.mipmapLevels);
            this.client.reloadResources();
        }

        for (OptionStorage<?> storage : dirtyStorages) {
            storage.save();
        }
    }

    private void undoChanges() {
        this.getAllOptions()
                .forEach(Option::reset);
    }

    private void openDonationPage() {
        URLUtils.open("https://caffeinemc.net/donate");
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_ESCAPE && !shouldCloseOnEsc()) {
            return;
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            onClose();
            return;
        }

        if (keyCode == Keyboard.KEY_P && hasShiftDown()) {
            this.client.setScreen(new VideoOptionsScreen(this.prevScreen, this.client.options));
        }
    }

    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    // We can't override onGuiClosed due to StackOverflow
    public void onClose() {
        this.client.setScreen(this.prevScreen);
        super.removed();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.children.forEach(element -> element.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        this.children.forEach(element -> element.mouseDragged(mouseX, mouseY, clickedMouseButton));
    }
}
