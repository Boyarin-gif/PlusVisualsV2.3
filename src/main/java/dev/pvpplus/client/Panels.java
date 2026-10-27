package dev.pvpplus.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.ItemCooldownManager;

/** Небольшие перетаскиваемые окошки: FPS/пинг, эффекты, цель, комбо, убийства, откаты, клавиши, уведомления. */
public final class Panels {
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    // ---------- FPS и пинг
    public static void fpsPing(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("fps")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        String fps = mc.getCurrentFps() + " FPS";
        int ping = 0;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String ps = ping + " мс";

        int w = tr.getWidth(fps) + tr.getWidth(ps) + 14;
        int h = 13;
        int[] p = HudPanels.place("fps", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 110);
        ctx.drawText(tr, fps, p[0] + 3, p[1] + 3, 0xFFFFFFFF, true);
        int pc = ping < 80 ? 0xFF55FF55 : ping < 150 ? 0xFFFFFF55 : 0xFFFF5555;
        ctx.drawText(tr, ps, p[0] + 3 + tr.getWidth(fps) + 8, p[1] + 3, pc, true);
    }

    // ---------- Эффекты зелий
    private static String fmtTime(int ticks) {
        int s = Math.max(0, ticks) / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public static void potions(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("potions")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity pl = mc.player;
        if (pl == null) return;
        Collection<StatusEffectInstance> effects = pl.getStatusEffects();
        if (effects.isEmpty()) return;
        TextRenderer tr = mc.textRenderer;

        List<String> names = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Boolean> low = new ArrayList<>();
        int shown = 0;
        for (StatusEffectInstance e : effects) {
            if (shown++ >= 8) break;
            String name = e.getEffectType().value().getName().getString();
            int amp = e.getAmplifier();
            if (amp > 0) name += " " + (amp < ROMAN.length ? ROMAN[amp] : String.valueOf(amp + 1));
            names.add(name);
            times.add(e.isInfinite() ? "∞" : fmtTime(e.getDuration()));
            colors.add(e.getEffectType().value().getColor() | 0xFF000000);
            low.add(!e.isInfinite() && e.getDuration() < 200);
        }

        int maxW = 0;
        for (int i = 0; i < names.size(); i++) {
            maxW = Math.max(maxW, tr.getWidth(names.get(i)) + 8 + tr.getWidth(times.get(i)));
        }
        int w = maxW + 8;
        int h = names.size() * 10 + 4;
        int[] p = HudPanels.place("potions", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 110);
        for (int i = 0; i < names.size(); i++) {
            int y = p[1] + 3 + i * 10;
            ctx.drawText(tr, names.get(i), p[0] + 4, y, colors.get(i), true);
            int tw = tr.getWidth(times.get(i));
            ctx.drawText(tr, times.get(i), p[0] + w - 4 - tw, y, low.get(i) ? 0xFFFF5555 : 0xFFFFFFFF, true);
        }
    }

    // ---------- Цель
    public static void target(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("target")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity pl = mc.player;
        if (pl == null || mc.world == null) return;

        LivingEntity t = null;
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            t = le;
        } else if (CombatTracker.tickCount - CombatTracker.lastAttackTick < 100) {
            Entity e = mc.world.getEntityById(CombatTracker.lastTargetId);
            if (e instanceof LivingEntity le2 && le2.isAlive()) t = le2;
        }
        if (t == null) return;

        TextRenderer tr = mc.textRenderer;
        String name = t.getName().getString();
        float hp = t.getHealth();
        float max = Math.max(1f, t.getMaxHealth());
        int armor = t.getArmor();
        double dist = pl.distanceTo(t);

        int w = Math.max(100, tr.getWidth(name) + 10);
        int h = 42;
        int[] p = HudPanels.place("target", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 130);
        ctx.drawText(tr, name, p[0] + 4, p[1] + 3, 0xFFFFFFFF, true);

        int barW = w - 8;
        float frac = Math.max(0f, Math.min(1f, hp / max));
        int col = frac > 0.5f ? 0xFF55FF55 : frac > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
        ctx.fill(p[0] + 4, p[1] + 14, p[0] + 4 + barW, p[1] + 19, 0xFF202020);
        ctx.fill(p[0] + 4, p[1] + 14, p[0] + 4 + Math.round(barW * frac), p[1] + 19, col);

        String hpText = (Math.round(hp * 10) / 10.0) + "/" + (int) max;
        ctx.drawText(tr, hpText, p[0] + 4, p[1] + 22, 0xFFFFFFFF, true);
        String ar = "Броня " + armor;
        ctx.drawText(tr, ar, p[0] + w - 4 - tr.getWidth(ar), p[1] + 22, 0xFFB0B0B0, true);
        ctx.drawText(tr, (Math.round(dist * 10) / 10.0) + " м", p[0] + 4, p[1] + 32, 0xFFB0B0B0, true);
    }

    // ---------- Комбо
    public static void combo(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("combo")) return;
        if (CombatTracker.combo < 2 || CombatTracker.tickCount - CombatTracker.lastHitTick > 40) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        String s = "Комбо x" + CombatTracker.combo;
        int w = tr.getWidth(s) + 10;
        int h = 13;
        int[] p = HudPanels.place("combo", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 130);
        ctx.drawText(tr, s, p[0] + 5, p[1] + 3, 0xFFFFD055, true);
    }

    // ---------- Лента убийств
    public static void killFeed(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("killfeed")) return;
        CombatTracker.feed.removeIf(f -> CombatTracker.tickCount - f.tick() > 100);
        if (CombatTracker.feed.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int maxW = 0;
        for (CombatTracker.FeedEntry f : CombatTracker.feed) maxW = Math.max(maxW, tr.getWidth(f.text()));
        int w = maxW + 10;
        int h = CombatTracker.feed.size() * 11 + 4;
        int[] p = HudPanels.place("killfeed", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 110);
        for (int i = 0; i < CombatTracker.feed.size(); i++) {
            ctx.drawText(tr, CombatTracker.feed.get(i).text(), p[0] + 5, p[1] + 3 + i * 11, 0xFFFFFFFF, true);
        }
    }

    // ---------- Откаты предметов (жемчуг, яблоки и т.д.)
    public static void cooldowns(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("cooldowns")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity pl = mc.player;
        if (pl == null) return;
        ItemCooldownManager cm = pl.getItemCooldownManager();
        PlayerInventory inv = pl.getInventory();

        List<ItemStack> stacks = new ArrayList<>();
        List<Float> progs = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.isEmpty() || !seen.add(st.getItem())) continue;
            float pr = cm.getCooldownProgress(st, 0f);
            if (pr > 0f) {
                stacks.add(st);
                progs.add(pr);
            }
        }
        if (stacks.isEmpty()) return;

        int w = 50;
        int h = stacks.size() * 18 + 4;
        int[] p = HudPanels.place("cooldowns", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 110);
        for (int i = 0; i < stacks.size(); i++) {
            int y = p[1] + 2 + i * 18;
            ctx.drawItem(stacks.get(i), p[0] + 3, y + 1);
            int bx = p[0] + 22;
            int bw = w - 26;
            ctx.fill(bx, y + 7, bx + bw, y + 11, 0xFF202020);
            ctx.fill(bx, y + 7, bx + Math.round(bw * progs.get(i)), y + 11, 0xFFFFB040);
        }
    }

    // ---------- Список клавиш мода
    public static void hotkeys(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("hotkeys")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        List<String> lines = PvpPlusClient.hotkeyLines();
        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));
        int w = maxW + 10;
        int h = lines.size() * 10 + 4;
        int[] p = HudPanels.place("hotkeys", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 110);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), p[0] + 5, p[1] + 3 + i * 10, 0xFFFFFFFF, true);
        }
    }

    // ---------- Уведомления сверху по центру
    public static void notifications(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("notify")) return;
        Notifier.LIST.removeIf(m -> CombatTracker.tickCount - m.tick() > 70);
        if (Notifier.LIST.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int n = Math.min(3, Notifier.LIST.size());
        List<Notifier.Msg> shown = Notifier.LIST.subList(Notifier.LIST.size() - n, Notifier.LIST.size());
        int maxW = 0;
        for (Notifier.Msg m : shown) maxW = Math.max(maxW, tr.getWidth(m.text()));
        int w = maxW + 16;
        int h = n * 12 + 5;
        int[] p = HudPanels.place("notify", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 140);
        for (int i = 0; i < n; i++) {
            String s = shown.get(i).text();
            ctx.drawText(tr, s, p[0] + (w - tr.getWidth(s)) / 2, p[1] + 4 + i * 12, 0xFFFFFFFF, true);
        }
    }

    // ---------- Булава: высота падения и бонус урона
    public static void mace(DrawContext ctx, RenderTickCounter tick) {
        if (!Config.on("mace")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity pl = mc.player;
        if (pl == null || !pl.getMainHandStack().isOf(Items.MACE)) return;
        double fall = pl.fallDistance;
        if (fall <= 0.5) return;
        double bonus = fall <= 3 ? 4 * fall : fall <= 8 ? 12 + 2 * (fall - 3) : 22 + (fall - 8);
        TextRenderer tr = mc.textRenderer;
        String s = "Падение " + (Math.round(fall * 10) / 10.0) + " м  +" + (int) bonus + " урона";
        int w = tr.getWidth(s) + 10;
        int h = 13;
        int[] p = HudPanels.place("mace", w, h);
        Gfx.panel(ctx, p[0], p[1], w, h, 130);
        ctx.drawText(tr, s, p[0] + 5, p[1] + 3, fall >= 1.5 ? 0xFFFFD055 : 0xFFB0B0B0, true);
    }
}
