package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class OptionPage {
    private final Text name;
    @Getter
    private final ImmutableList<OptionGroup> groups;
    @Getter
    private final ImmutableList<Option<?>> options;

    public OptionPage(String name, ImmutableList<OptionGroup> groups) {
        this(new LiteralText(name), groups);
    }

    public OptionPage(Text name, ImmutableList<OptionGroup> groups) {
        this.name = name;
        this.groups = groups;

        ImmutableList.Builder<Option<?>> builder = ImmutableList.builder();

        for (OptionGroup group : groups) {
            builder.addAll(group.getOptions());
        }

        this.options = builder.build();
    }

    public Text getNewName() {
        return this.name;
    }

    public Text getName() {
        return this.getNewName();
    }

}
