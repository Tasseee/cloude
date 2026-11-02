package dev.nightfall.gui;

import dev.nightfall.Nightfall;
import dev.nightfall.config.Config;
import dev.nightfall.module.Category;
import dev.nightfall.module.Module;
import dev.nightfall.module.ModuleManager;
import dev.nightfall.module.setting.*;
import dev.nightfall.util.Keys;
import dev.nightfall.util.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immediate-Mode ClickGUI. Maus und Tasten werden per GLFW-Polling mit Flankenerkennung ausgewertet.
 * Linksklick Modul = an/aus | Rechtsklick = Einstellungen | Mittelklick = Keybind (Backspace = loeschen)
 * Header ziehen = verschieben, Rechtsklick Header = einklappen | Suchleiste oben: anklicken und tippen.
 */
public class ClickGuiScreen extends Screen {
    private static final int PW = 126, HH = 18, MH = 16, SH = 14;
    private static final Set<Module> EXPANDED = new HashSet<>();
    private static String search = "";

    private final List<Category> cats = new ArrayList<>();
    private final Keys.Poller poller = new Keys.Poller();
    private boolean prevL, prevR, prevM, clickL, clickR, clickM;
    private boolean searchFocus;
    private Module listening;
    private String notice = "";
    private long noticeUntil;
    private Category dragging;
    private int dragDx, dragDy;
    private NumberSetting slider;
    private int sliderX, sliderW;

    public ClickGuiScreen() {
        super(Text.literal("Nightfall"));
        for (Category c : Category.values()) if (!ModuleManager.byCategory(c).isEmpty()) cats.add(c);
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        prevL = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        prevM = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        poller.sync(h);
    }

    private int[] state(Category c) {
        return Config.panels.computeIfAbsent(c.name(), k -> {
            int idx = cats.indexOf(c);
            int cols = Math.max(1, (MinecraftClient.getInstance().getWindow().getScaledWidth() - 10) / (PW + 8));
            return new int[]{10 + (idx % cols) * (PW + 8), 30 + (idx / cols) * 160, 1};
        });
    }

    private List<Module> visible(Category c) {
        List<Module> all = ModuleManager.byCategory(c);
        if (search.isEmpty()) return all;
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (m.getName().toLowerCase(Locale.ROOT).contains(search)) out.add(m);
        return out;
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { Config.save(); }

    private void onKey(int k) {
        if (listening != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { listening = null; return; }
            if (k == GLFW.GLFW_KEY_BACKSPACE) { listening.setKey(-1); notice("Keybind entfernt: " + listening.getName()); listening = null; return; }
            String err = ModuleManager.assignKey(listening, k);
            notice(err != null ? err : listening.getName() + " = " + Keys.name(k));
            listening = null;
            return;
        }
        if (!searchFocus) return;
        if (k == GLFW.GLFW_KEY_BACKSPACE) { if (!search.isEmpty()) search = search.substring(0, search.length() - 1); }
        else if (k >= GLFW.GLFW_KEY_A && k <= GLFW.GLFW_KEY_Z) search += (char) ('a' + (k - GLFW.GLFW_KEY_A));
        else if (k >= GLFW.GLFW_KEY_0 && k <= GLFW.GLFW_KEY_9) search += (char) ('0' + (k - GLFW.GLFW_KEY_0));
        else if (k == GLFW.GLFW_KEY_SPACE) search += ' ';
        else if (k == GLFW.GLFW_KEY_ENTER) searchFocus = false;
    }

    private void notice(String s) { notice = s; noticeUntil = System.currentTimeMillis() + 3000; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean md = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        clickL = l && !prevL; clickR = r && !prevR; clickM = md && !prevM;
        prevL = l; prevR = r; prevM = md;

        boolean[] closeFlag = {false};
        poller.poll(h, k -> {
            if (k == Config.guiKey && listening == null && !searchFocus) closeFlag[0] = true;
            else onKey(k);
        });
        if (closeFlag[0]) { close(); return; }

        ctx.fill(0, 0, width, height, Theme.BG_DIM);

        // Suchleiste
        int sbw = 170, sbx = width / 2 - sbw / 2, sby = 6;
        ctx.fill(sbx, sby, sbx + sbw, sby + 16, searchFocus ? 0xF01E1B2B : 0xE6141418);
        ctx.fill(sbx, sby + 15, sbx + sbw, sby + 16, searchFocus ? Theme.ACCENT : Theme.ACCENT_DIM);
        String st = search.isEmpty() && !searchFocus ? "Suche..." : search + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        ctx.drawText(mc.textRenderer, st, sbx + 6, sby + 4, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT, false);
        if (clickL) {
            boolean inBar = mx >= sbx && mx < sbx + sbw && my >= sby && my < sby + 16;
            searchFocus = inBar;
            if (inBar) clickL = false;
        }

        if (!l) { dragging = null; slider = null; }
        if (slider != null) {
            double t = (mx - sliderX) / (double) sliderW;
            slider.set(slider.min + Math.max(0, Math.min(1, t)) * (slider.max - slider.min));
        }
        if (dragging != null) {
            int[] s = state(dragging);
            s[0] = Math.max(0, Math.min(width - PW, mx - dragDx));
            s[1] = Math.max(0, Math.min(height - HH, my - dragDy));
        }

        Module hover = null;
        for (Category c : cats) {
            if (!search.isEmpty() && visible(c).isEmpty()) continue;
            Module hm = drawPanel(ctx, c, mx, my);
            if (hm != null) hover = hm;
        }
        if (hover != null) {
            String d = hover.getDescription();
            int w = mc.textRenderer.getWidth(d) + 10;
            ctx.fill(6, height - 20, 6 + w, height - 6, 0xE6141418);
            ctx.fill(6, height - 20, 8, height - 6, Theme.ACCENT);
            ctx.drawText(mc.textRenderer, d, 12, height - 16, Theme.TEXT, false);
        }
        if (System.currentTimeMillis() < noticeUntil) {
            ctx.drawText(mc.textRenderer, notice, width / 2 - mc.textRenderer.getWidth(notice) / 2, 28, 0xFFFFD27A, true);
        }
        String hint = Nightfall.NAME + " " + Nightfall.VERSION + "  |  L: an/aus  R: Optionen  M-Klick: Keybind";
        ctx.drawText(mc.textRenderer, hint, width - mc.textRenderer.getWidth(hint) - 6, height - 14, Theme.TEXT_DIM, false);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Module drawPanel(DrawContext ctx, Category c, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int[] st = state(c);
        int px = st[0], py = st[1];
        boolean open = st[2] == 1 || !search.isEmpty();
        List<Module> mods = visible(c);

        int bodyH = 0;
        if (open) for (Module m : mods) {
            bodyH += MH;
            if (EXPANDED.contains(m)) bodyH += m.getSettings().size() * SH;
        }

        ctx.fillGradient(px, py, px + PW, py + HH, Theme.HEADER_TOP, Theme.HEADER_BOT);
        ctx.fill(px, py + HH - 1, px + PW, py + HH, Theme.ACCENT);
        ctx.drawText(mc.textRenderer, c.display, px + 6, py + 5, Theme.TEXT, true);
        ctx.drawText(mc.textRenderer, open ? "-" : "+", px + PW - 10, py + 5, Theme.ACCENT, false);
        if (in(mx, my, px, py, PW, HH)) {
            if (clickL) { dragging = c; dragDx = mx - px; dragDy = my - py; clickL = false; }
            else if (clickR) { st[2] = st[2] == 1 ? 0 : 1; clickR = false; }
        }
        if (!open) return null;

        ctx.fill(px, py + HH, px + PW, py + HH + bodyH, Theme.PANEL);
        Module hovered = null;
        int y = py + HH;
        for (Module m : mods) {
            boolean over = in(mx, my, px, y, PW, MH);
            if (m.isEnabled()) {
                ctx.fill(px, y, px + PW, y + MH, Theme.ACCENT_BG);
                ctx.fill(px, y, px + 2, y + MH, Theme.ACCENT);
            }
            if (over) { ctx.fill(px, y, px + PW, y + MH, Theme.ROW_HOVER); hovered = m; }
            ctx.drawText(mc.textRenderer, m.getName(), px + 7, y + 4, m.isEnabled() ? Theme.TEXT : Theme.TEXT_DIM, false);
            String right = listening == m ? "[...]" : m.getKey() >= 0 ? "[" + Keys.name(m.getKey()) + "]"
                    : (m.getSettings().isEmpty() ? "" : (EXPANDED.contains(m) ? "v" : ">"));
            if (!right.isEmpty())
                ctx.drawText(mc.textRenderer, right, px + PW - 6 - mc.textRenderer.getWidth(right), y + 4, Theme.TEXT_DIM, false);
            if (over) {
                if (clickL) { m.toggle(); clickL = false; }
                else if (clickR && !m.getSettings().isEmpty()) {
                    if (!EXPANDED.remove(m)) EXPANDED.add(m);
                    clickR = false;
                } else if (clickM) { listening = m; searchFocus = false; notice("Taste druecken fuer " + m.getName() + " (Backspace = loeschen)"); clickM = false; }
            }
            y += MH;
            if (EXPANDED.contains(m)) for (Setting<?> s : m.getSettings()) { drawSetting(ctx, s, px, y, mx, my); y += SH; }
        }
        return hovered;
    }

    private void drawSetting(DrawContext ctx, Setting<?> s, int px, int y, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean over = in(mx, my, px, y, PW, SH);
        ctx.fill(px + 2, y, px + PW, y + SH, 0x40000000);
        if (over) ctx.fill(px + 2, y, px + PW, y + SH, 0x18FFFFFF);

        if (s instanceof BoolSetting b) {
            ctx.drawText(mc.textRenderer, b.getName(), px + 8, y + 3, Theme.TEXT_DIM, false);
            int bx = px + PW - 16;
            ctx.fill(bx, y + 3, bx + 10, y + 11, b.get() ? Theme.ACCENT : 0xFF2A2A34);
            if (over && clickL) { b.toggle(); clickL = false; }
        } else if (s instanceof NumberSetting n) {
            double t = (n.get() - n.min) / (n.max - n.min);
            ctx.fill(px + 4, y + SH - 3, px + PW - 4, y + SH - 1, 0xFF2A2A34);
            ctx.fill(px + 4, y + SH - 3, px + 4 + (int) ((PW - 8) * t), y + SH - 1, Theme.ACCENT);
            ctx.drawText(mc.textRenderer, n.getName(), px + 8, y + 1, Theme.TEXT_DIM, false);
            String v = n.display();
            ctx.drawText(mc.textRenderer, v, px + PW - 6 - mc.textRenderer.getWidth(v), y + 1, Theme.TEXT, false);
            if (over && clickL) { slider = n; sliderX = px + 4; sliderW = PW - 8; clickL = false; }
        } else if (s instanceof ModeSetting md) {
            ctx.drawText(mc.textRenderer, md.getName(), px + 8, y + 3, Theme.TEXT_DIM, false);
            String v = md.get();
            ctx.drawText(mc.textRenderer, v, px + PW - 6 - mc.textRenderer.getWidth(v), y + 3, Theme.ACCENT, false);
            if (over && clickL) { md.cycle(1); clickL = false; }
            else if (over && clickR) { md.cycle(-1); clickR = false; }
        } else if (s instanceof ColorSetting cs) {
            ctx.drawText(mc.textRenderer, cs.getName(), px + 8, y + 3, Theme.TEXT_DIM, false);
            ctx.fill(px + PW - 18, y + 3, px + PW - 6, y + 11, 0xFF000000 | cs.rgb());
            if (over && clickL) { cs.cycle(1); clickL = false; }
            else if (over && clickR) { cs.cycle(-1); clickR = false; }
        } else if (s instanceof ListSetting ls) {
            String v = ls.get().size() + " (Befehl /nf tpa)";
            ctx.drawText(mc.textRenderer, ls.getName() + ": " + v, px + 8, y + 3, Theme.TEXT_DIM, false);
        }
    }
}
