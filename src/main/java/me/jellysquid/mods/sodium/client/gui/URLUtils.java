package me.jellysquid.mods.sodium.client.gui;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.util.Util;

import java.io.IOException;

public class URLUtils {

    private static String[] getURLOpenCommand(String url) {
        switch (Util.getOperatingSystem()) {
            case WINDOWS:
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            case MACOS:
                return new String[]{"open", url};
            case OTHER:
            case LINUX:
            case SOLARIS:
                return new String[]{"xdg-open", url};
            default:
                throw new IllegalArgumentException("Unexpected OS Type");
        }
    }

    public static void open(String url) {
        try {
            Runtime.getRuntime().exec(getURLOpenCommand(url));
        } catch (IOException exception) {
            SodiumClientMod.logger().error("Couldn't open url '{}'", url, exception);
        }

    }

}