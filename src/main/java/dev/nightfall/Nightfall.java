package dev.nightfall;

import dev.nightfall.config.Config;
import dev.nightfall.gui.ClickGuiScreen;
import dev.nightfall.gui.HudEditorScreen;
import dev.nightfall.gui.MapScreen;
import dev.nightfall.module.ModuleManager;
import dev.nightfall.modules.AutoTpa;
import dev.nightfall.modules.BaseFinder;
import dev.nightfall.scanner.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nightfall implements ClientModInitializer {
    public static final String NAME = "Nightfall";
    public static final String VERSION = "0.5.0";
    public static final Logger LOGGER = LoggerFactory.getLogger("nightfall");

    private boolean prevGui, prevHud, prevMap;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("nightfall", "hud"),
                (ctx, tickCounter) -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options.hudHidden || mc.world == null) return;
                    ModuleManager.renderHud(ctx);
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ModuleManager.get(BaseFinder.class).clear();
            Config.save();
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ModuleManager.get(AutoTpa.class).onMessage(message.getString());
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Commands.register(dispatcher));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            ModuleManager.get(BaseFinder.class).saveMarks();
            WaypointManager.save();
            Config.save();
        });

        LOGGER.info("{} {} geladen", NAME, VERSION);
    }

    private void onTick(MinecraftClient mc) {
        if (mc.getWindow() != null) {
            long h = mc.getWindow().getHandle();
            boolean g = GLFW.glfwGetKey(h, Config.guiKey) == GLFW.GLFW_PRESS;
            boolean hu = GLFW.glfwGetKey(h, Config.hudKey) == GLFW.GLFW_PRESS;
            boolean m = GLFW.glfwGetKey(h, Config.mapKey) == GLFW.GLFW_PRESS;
            if (mc.currentScreen == null && mc.player != null) {
                if (g && !prevGui) open(mc, new ClickGuiScreen());
                else if (hu && !prevHud) open(mc, new HudEditorScreen());
                else if (m && !prevMap) open(mc, new MapScreen());
            }
            prevGui = g; prevHud = hu; prevMap = m;
            ModuleManager.pollKeys(mc);
        }
        ModuleManager.tick();
    }

    private static void open(MinecraftClient mc, Screen s) { mc.setScreen(s); }
}
