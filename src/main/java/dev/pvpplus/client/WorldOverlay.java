package dev.pvpplus.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Рисуется поверх экрана через проекцию точек мира (без настоящей 3D-отрисовки, поэтому видно и сквозь блоки). */
public final class WorldOverlay {
    private static final int SEG = 28;

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        if (!(Config.on("hitrange") || Config.on("hitbubble") || Config.on("jumpcircles") || Config.on("trails")
                || Config.on("targetesp") || Config.on("arrownametag") || Config.on("prediction"))) return;

        float tp = tick.getTickProgress(false);
        Projection.Frame f = Projection.begin(mc, tp);
        int now = CombatTracker.tickCount;

        LivingEntity target = (Config.on("hitrange") || Config.on("targetesp")) ? CombatTracker.currentTarget(mc) : null;
        if (target != null) {
            if (Config.on("hitrange")) {
                ring(ctx, f, new Vec3d(target.getX(), target.getY() + 0.05, target.getZ()),
                        Config.num("hitrange.radius"), Config.color("hitrange.color"));
            }
            if (Config.on("targetesp")) box(ctx, f, target.getBoundingBox(), Config.color("targetesp.color"));
        }

        if (Config.on("hitbubble")) {
            int life = Math.max(1, Config.i("hitbubble.life"));
            for (Effects.Ring r : Effects.bubbles) {
                double t = Math.min(1.0, (now - r.tick()) / (double) life);
                double rad = 0.3 + (Config.num("hitbubble.radius") - 0.3) * t;
                ring(ctx, f, r.pos(), rad, Gfx.alpha(Config.color("hitbubble.color"), (int) (230 * (1 - t))));
            }
        }

        if (Config.on("jumpcircles")) {
            int life = Math.max(1, Config.i("jumpcircles.life"));
            for (Effects.Ring r : Effects.jumpCircles) {
                double t = Math.min(1.0, (now - r.tick()) / (double) life);
                double rad = 0.3 + (Config.num("jumpcircles.radius") - 0.3) * t;
                ring(ctx, f, r.pos(), rad, Gfx.alpha(Config.color("jumpcircles.color"), (int) (230 * (1 - t))));
            }
        }

        if (Config.on("trails") && Effects.trail.size() > 1) {
            int n = Effects.trail.size();
            int i = 0;
            Vec3d prev = null;
            int base = Config.color("trails.color");
            for (Vec3d v : Effects.trail) {
                if (prev != null) {
                    int[] a = f.project(prev), b = f.project(v);
                    if (a != null && b != null) Gfx.line(ctx, a[0], a[1], b[0], b[1], Gfx.alpha(base, 40 + (int) (200.0 * i / n)));
                }
                prev = v;
                i++;
            }
        }

        if (Config.on("arrownametag")) {
            double range = Config.num("arrownametag.range");
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PersistentProjectileEntity ar)) continue;
                if (ar.getVelocity().lengthSquared() < 0.01) continue;
                double d = p.distanceTo(ar);
                if (d > range || d < 2) continue;
                int[] s = f.project(new Vec3d(ar.getX(), ar.getY() + 0.3, ar.getZ()));
                if (s == null) continue;
                Entity owner = ar.getOwner();
                String text = (owner != null ? owner.getName().getString() + " " : "") + Math.round(d) + " м";
                ctx.drawCenteredTextWithShadow(mc.textRenderer, text, s[0], s[1] - 10, 0xFFFFD055);
                ctx.fill(s[0] - 1, s[1] - 1, s[0] + 2, s[1] + 2, 0xFFFFD055);
            }
        }

        if (Config.on("prediction")) predict(ctx, f, mc, p);
    }

    private static void ring(DrawContext ctx, Projection.Frame f, Vec3d c, double r, int color) {
        int[] prev = null;
        for (int i = 0; i <= SEG; i++) {
            double a = 2 * Math.PI * i / SEG;
            int[] s = f.project(new Vec3d(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r));
            if (prev != null && s != null) Gfx.line(ctx, prev[0], prev[1], s[0], s[1], color);
            prev = s;
        }
    }

    private static void box(DrawContext ctx, Projection.Frame f, Box bb, int color) {
        double[] xs = {bb.minX, bb.maxX}, ys = {bb.minY, bb.maxY}, zs = {bb.minZ, bb.maxZ};
        int[][] pts = new int[8][];
        for (int i = 0; i < 8; i++) pts[i] = f.project(new Vec3d(xs[i & 1], ys[(i >> 1) & 1], zs[(i >> 2) & 1]));
        int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            int[] a = pts[e[0]], b = pts[e[1]];
            if (a != null && b != null) Gfx.line(ctx, a[0], a[1], b[0], b[1], color);
        }
    }

    private static void predict(DrawContext ctx, Projection.Frame f, MinecraftClient mc, PlayerEntity p) {
        ItemStack st = p.getMainHandStack();
        double speed, grav;
        if (st.isOf(Items.BOW) && p.isUsingItem() && p.getActiveItem().isOf(Items.BOW)) {
            float t = p.getItemUseTime() / 20.0f;
            float power = Math.min(1.0f, (t * t + t * 2.0f) / 3.0f);
            if (power < 0.1f) return;
            speed = power * 3.0;
            grav = 0.05;
        } else if (st.isOf(Items.ENDER_PEARL) || st.isOf(Items.SNOWBALL) || st.isOf(Items.EGG)) {
            speed = 1.5;
            grav = 0.03;
        } else {
            return;
        }

        int color = Config.color("prediction.color");
        Vec3d pos = f.eye.add(0, -0.1, 0);
        Vec3d vel = f.fwd.multiply(speed);
        int[] prev = f.project(pos);
        for (int i = 0; i < 150; i++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            boolean stop = hit.getType() != HitResult.Type.MISS;
            Vec3d end = stop ? hit.getPos() : next;
            int[] s = f.project(end);
            if (prev != null && s != null) Gfx.line(ctx, prev[0], prev[1], s[0], s[1], color);
            prev = s;
            if (stop) {
                if (s != null) ctx.fill(s[0] - 2, s[1] - 2, s[0] + 3, s[1] + 3, color);
                break;
            }
            pos = next;
            vel = vel.multiply(0.99).add(0, -grav, 0);
        }
    }
}
