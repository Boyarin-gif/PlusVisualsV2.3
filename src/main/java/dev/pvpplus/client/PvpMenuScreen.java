package dev.pvpplus.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import dev.pvpplus.client.Features.Feature;
import dev.pvpplus.client.Features.Setting;
import dev.pvpplus.client.Features.Tab;
import dev.pvpplus.client.WaypointManager.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/** Меню в стиле Pulse Visuals: слева настройки пункта, по центру функции, справа метки. */
public class PvpMenuScreen extends Screen {
    private static final String TITLE = "Pulse Visuals";
    private static final int LW = 108, CW = 220, RW = 124, GAP = 6;
    private static final int CONTENT_H = 190;
    private static final int MAX_WP = 6;

    private record Hit(int x, int y, int w, int h, Runnable click, IntConsumer drag) {}

    private final List<Hit> hits = new ArrayList<>();
    private Hit active = null;
    private boolean wasDown = true;
    private boolean dirty = false;

    private Tab tab = Tab.VISUALS;
    private Feature selected = Features.inTab(Tab.VISUALS).get(0);

    private TextFieldWidget search, name, fx, fy, fz;
    private String sSearch = "", sName = "", sX = "", sY = "", sZ = "";
    private String status = "";

    private int leftX, centerX, rightX, top, contentTop;

    public PvpMenuScreen() {
        super(Text.literal(TITLE));
    }

    @Override
    protected void init() {
        if (name != null) remember();

        int total = LW + GAP + CW + GAP + RW;
        int x0 = (width - total) / 2;
        leftX = x0;
        centerX = x0 + LW + GAP;
        rightX = centerX + CW + GAP;
        top = Math.max(2, height / 2 - 120);
        contentTop = top + 26;

        search = new TextFieldWidget(textRenderer, centerX + 156, contentTop + 4, 58, 14, Text.literal("Поиск"));
        search.setMaxLength(16);
        search.setPlaceholder(Text.literal("Поиск"));
        search.setText(sSearch);
        addDrawableChild(search);

        name = new TextFieldWidget(textRenderer, rightX + 6, contentTop + 22, RW - 12, 14, Text.literal("Название"));
        name.setMaxLength(24);
        name.setPlaceholder(Text.literal("Название"));
        name.setText(sName);
        addDrawableChild(name);
        fx = coord(rightX + 6, "X", sX);
        fy = coord(rightX + 43, "Y", sY);
        fz = coord(rightX + 80, "Z", sZ);
    }

    private TextFieldWidget coord(int x, String placeholder, String value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, contentTop + 40, 34, 14, Text.literal(placeholder));
        f.setMaxLength(8);
        f.setPlaceholder(Text.literal(placeholder));
        f.setText(value);
        addDrawableChild(f);
        return f;
    }

    private void remember() {
        sSearch = search.getText();
        sName = name.getText();
        sX = fx.getText();
        sY = fy.getText();
        sZ = fz.getText();
    }

    // ------------------------------------------------------------ метки
    private void fillHere() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        fx.setText(String.valueOf(mc.player.getBlockX()));
        fy.setText(String.valueOf(mc.player.getBlockY()));
        fz.setText(String.valueOf(mc.player.getBlockZ()));
    }

    private void addWaypoint() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        String n = name.getText().trim();
        if (n.isEmpty()) {
            status = "Введите название";
            return;
        }
        try {
            double x = Double.parseDouble(fx.getText().trim().replace(',', '.'));
            double z = Double.parseDouble(fz.getText().trim().replace(',', '.'));
            String ys = fy.getText().trim();
            double y = ys.isEmpty() ? mc.player.getY() : Double.parseDouble(ys.replace(',', '.'));
            Waypoint wp = new Waypoint();
            wp.name = n;
            wp.x = x;
            wp.y = y;
            wp.z = z;
            wp.dim = mc.world.getRegistryKey().getValue().toString();
            WaypointManager.all().add(wp);
            WaypointManager.save();
            name.setText("");
            fx.setText("");
            fy.setText("");
            fz.setText("");
            status = "";
        } catch (NumberFormatException e) {
            status = "Координаты: только числа";
        }
    }

    private static String dimShort(String d) {
        if (d == null) return "";
        return switch (d) {
            case "minecraft:overworld" -> "Мир";
            case "minecraft:the_nether" -> "Незер";
            case "minecraft:the_end" -> "Энд";
            default -> d;
        };
    }

    // ------------------------------------------------------------ рисование
    private void region(int x, int y, int w, int h, Runnable click) {
        hits.add(new Hit(x, y, w, h, click, null));
    }

    private void pill(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, int bg, Runnable click) {
        Ui.rrect(ctx, x, y, w, h, bg);
        ctx.drawCenteredTextWithShadow(tr, label, x + w / 2, y + (h - 8) / 2, Ui.TEXT);
        region(x, y, w, h, click);
    }

    // этот метод пустой, чтобы не было размытия фона (меню рисует себя само)
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        TextRenderer tr = textRenderer;

        ctx.fill(0, 0, width, height, 0xA0000000);
        drawHeader(ctx, tr);
        drawLeft(ctx, tr);
        drawCenter(ctx, tr);
        drawRight(ctx, tr);
        drawBottom(ctx, tr);

        super.render(ctx, mouseX, mouseY, delta);
        poll(mouseX, mouseY);
    }

    private void drawHeader(DrawContext ctx, TextRenderer tr) {
        Text t = Text.literal(TITLE).formatted(Formatting.BOLD);
        int tw = tr.getWidth(t) + 26;
        int x = width / 2 - tw / 2;
        Ui.logo(ctx, x, top + 3, Ui.ACCENT);
        ctx.drawTextWithShadow(tr, t, x + 26, top + 7, Ui.TEXT);
    }

    private static String fmt(Setting s, double v) {
        if (s.kind == Features.Kind.CHOICE) return s.options[Math.floorMod((int) Math.round(v), s.options.length)];
        return s.step >= 1 ? String.valueOf((int) Math.round(v)) : String.format("%.1f", v);
    }

    private void setSlider(Setting s, int mx, int tx, int tw) {
        double t = Math.max(0, Math.min(1, (mx - tx) / (double) tw));
        double v = s.min + t * (s.max - s.min);
        v = Math.round(v / s.step) * s.step;
        v = Math.max(s.min, Math.min(s.max, v));
        Config.setNum(s.key, v);
        dirty = true;
    }

    private static List<String> wrap(TextRenderer tr, String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (tr.getWidth(test) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private void drawLeft(DrawContext ctx, TextRenderer tr) {
        Ui.panel(ctx, leftX, contentTop, LW, CONTENT_H);
        if (selected == null) return;
        ctx.drawTextWithShadow(tr, tr.trimToWidth(selected.name, LW - 12), leftX + 6, contentTop + 6, Ui.ACCENT);
        ctx.fill(leftX + 6, contentTop + 17, leftX + LW - 6, contentTop + 18, 0x30FFFFFF);
        int dy = contentTop + 23;
        for (String line : wrap(tr, Features.desc(selected.id), LW - 12)) {
            ctx.drawTextWithShadow(tr, line, leftX + 6, dy, Ui.DIM);
            dy += 10;
        }
        if (selected.settings.isEmpty()) return;
        ctx.fill(leftX + 6, dy + 1, leftX + LW - 6, dy + 2, 0x30FFFFFF);
        int y = dy + 7;
        for (Setting s : selected.settings) {
            if (s.kind == Features.Kind.SLIDER) {
                double v = Config.num(s.key);
                ctx.drawTextWithShadow(tr, s.label, leftX + 6, y, Ui.DIM);
                String vs = fmt(s, v);
                ctx.drawTextWithShadow(tr, vs, leftX + LW - 6 - tr.getWidth(vs), y, Ui.TEXT);
                final int tx = leftX + 6, tw = LW - 12, ty = y + 13;
                double t = Math.max(0, Math.min(1, (v - s.min) / (s.max - s.min)));
                int fill = (int) (tw * t);
                Ui.rrect(ctx, tx, ty, tw, 4, Ui.OFF);
                Ui.rrect(ctx, tx, ty, Math.max(2, fill), 4, Ui.ACCENT);
                Ui.rrect(ctx, tx + fill - 2, ty - 2, 5, 8, 0xFFFFFFFF);
                hits.add(new Hit(tx - 2, ty - 5, tw + 4, 14, null, mx -> setSlider(s, mx, tx, tw)));
                y += 26;
            } else if (s.kind == Features.Kind.CHOICE) {
                final int idx = Math.floorMod(Config.i(s.key), s.options.length);
                ctx.drawTextWithShadow(tr, s.label, leftX + 6, y, Ui.DIM);
                Ui.rrect(ctx, leftX + 6, y + 11, LW - 12, 13, Ui.ROW);
                ctx.drawCenteredTextWithShadow(tr, "< " + s.options[idx] + " >", leftX + LW / 2, y + 14, Ui.TEXT);
                region(leftX + 6, y + 11, LW - 12, 13, () -> {
                    Config.setNum(s.key, (idx + 1) % s.options.length);
                    dirty = true;
                });
                y += 26;
            } else {
                boolean on = Config.b(s.key);
                ctx.drawTextWithShadow(tr, s.label, leftX + 6, y + 1, Ui.DIM);
                Ui.toggle(ctx, leftX + LW - 26, y, on);
                region(leftX + 6, y - 2, LW - 12, 14, () -> {
                    Config.setNum(s.key, Config.b(s.key) ? 0 : 1);
                    dirty = true;
                });
                y += 22;
            }
        }
    }

    private List<Feature> visibleFeatures() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        if (q.isEmpty()) return Features.inTab(tab);
        List<Feature> l = new ArrayList<>();
        for (Feature f : Features.all()) if (f.name.toLowerCase().contains(q)) l.add(f);
        return l;
    }

    private void drawCenter(DrawContext ctx, TextRenderer tr) {
        Ui.panel(ctx, centerX, contentTop, CW, CONTENT_H);

        boolean searching = search != null && !search.getText().trim().isEmpty();
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            final Tab tt = tabs[i];
            int tx = centerX + 6 + i * 50;
            boolean sel = tt == tab && !searching;
            Ui.rrect(ctx, tx, contentTop + 4, 46, 14, sel ? Ui.ACCENT : Ui.ROW);
            ctx.drawCenteredTextWithShadow(tr, tt.title, tx + 23, contentTop + 7, Ui.TEXT);
            region(tx, contentTop + 4, 46, 14, () -> {
                tab = tt;
                search.setText("");
                selected = Features.inTab(tt).get(0);
            });
        }

        List<Feature> shown = visibleFeatures();
        int colW = 101, rowH = 18, listTop = contentTop + 24;
        for (int i = 0; i < Math.min(shown.size(), 18); i++) {
            final Feature f = shown.get(i);
            int col = i % 2, row = i / 2;
            int rx = centerX + 6 + col * (colW + 6);
            int ry = listTop + row * rowH;
            Ui.rrect(ctx, rx, ry, colW, rowH - 2, f == selected ? Ui.ROW_SEL : Ui.ROW);
            ctx.drawTextWithShadow(tr, tr.trimToWidth(f.name, colW - 30), rx + 5, ry + 4, Ui.TEXT);
            Ui.toggle(ctx, rx + colW - 24, ry + 3, Config.on(f.id));
            region(rx, ry, colW - 26, rowH - 2, () -> selected = f);
            region(rx + colW - 24, ry + 3, 20, 10, () -> {
                Config.setOn(f.id, !Config.on(f.id));
                selected = f;
                dirty = true;
            });
        }
    }

    private void drawRight(DrawContext ctx, TextRenderer tr) {
        Ui.panel(ctx, rightX, contentTop, RW, CONTENT_H);
        ctx.drawTextWithShadow(tr, "Метки", rightX + 6, contentTop + 6, Ui.ACCENT);
        ctx.fill(rightX + 6, contentTop + 17, rightX + RW - 6, contentTop + 18, 0x30FFFFFF);

        pill(ctx, tr, rightX + 6, contentTop + 58, 55, 14, "Добавить", Ui.ACCENT, this::addWaypoint);
        pill(ctx, tr, rightX + 65, contentTop + 58, 53, 14, "Моя поз.", Ui.ROW_SEL, this::fillHere);
        if (!status.isEmpty()) ctx.drawTextWithShadow(tr, tr.trimToWidth(status, RW - 12), rightX + 6, contentTop + 76, 0xFFFF6060);

        List<Waypoint> list = WaypointManager.all();
        int shown = Math.min(list.size(), MAX_WP);
        for (int i = 0; i < shown; i++) {
            final Waypoint wp = list.get(i);
            int ry = contentTop + 88 + i * 16;
            Ui.rrect(ctx, rightX + 6, ry, RW - 12, 14, Ui.ROW);
            String line = wp.name + " " + (int) wp.x + " " + (int) wp.y + " " + (int) wp.z + " " + dimShort(wp.dim);
            ctx.drawTextWithShadow(tr, tr.trimToWidth(line, RW - 34), rightX + 9, ry + 3, Ui.TEXT);
            Ui.rrect(ctx, rightX + RW - 22, ry + 1, 14, 12, Ui.OFF);
            ctx.drawCenteredTextWithShadow(tr, "x", rightX + RW - 15, ry + 3, 0xFFFF7070);
            region(rightX + RW - 22, ry + 1, 14, 12, () -> {
                WaypointManager.all().remove(wp);
                WaypointManager.save();
            });
        }
        if (list.size() > MAX_WP) {
            ctx.drawTextWithShadow(tr, "...и ещё " + (list.size() - MAX_WP), rightX + 6, contentTop + 88 + MAX_WP * 16, Ui.DIM);
        }
    }

    private void drawBottom(DrawContext ctx, TextRenderer tr) {
        int y = contentTop + CONTENT_H + 5;
        pill(ctx, tr, centerX, y, 108, 14, "Редактор HUD", Ui.ROW_SEL,
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(this)));
        pill(ctx, tr, centerX + 112, y, 108, 14, "Закрыть", Ui.ROW_SEL, this::close);
    }

    // ------------------------------------------------------------ ввод мышью (опрос состояния кнопки)
    private void poll(int mx, int my) {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (down && !wasDown) {
            active = null;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (mx >= h.x() && mx < h.x() + h.w() && my >= h.y() && my < h.y() + h.h()) {
                    if (h.click() != null) h.click().run();
                    if (h.drag() != null) h.drag().accept(mx);
                    active = h;
                    break;
                }
            }
        } else if (down && active != null && active.drag() != null) {
            active.drag().accept(mx);
        }
        if (!down && wasDown) {
            active = null;
            if (dirty) {
                Config.save();
                dirty = false;
            }
        }
        wasDown = down;
    }

    @Override
    public void removed() {
        Config.save();
    }
}
