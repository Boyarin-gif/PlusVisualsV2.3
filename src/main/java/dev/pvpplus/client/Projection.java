package dev.pvpplus.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Перевод точки мира в координаты экрана (по углам камеры и FOV). */
public final class Projection {
    public static final class Frame {
        Vec3d eye, fwd, right, up;
        double t, aspect;
        int w, h;

        /** Возвращает {x, y} в GUI-координатах или null, если точка позади камеры. */
        public int[] project(Vec3d p) {
            Vec3d d = p.subtract(eye);
            double cz = d.dotProduct(fwd);
            if (cz <= 0.05) return null;
            double cx = d.dotProduct(right);
            double cy = d.dotProduct(up);
            double nx = cx / (cz * t * aspect);
            double ny = cy / (cz * t);
            return new int[]{(int) ((nx * 0.5 + 0.5) * w), (int) ((0.5 - ny * 0.5) * h)};
        }
    }

    public static Frame begin(MinecraftClient mc, float tp) {
        Frame f = new Frame();
        Camera cam = mc.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        f.eye = mc.player.getLerpedPos(tp).add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        f.fwd = new Vec3d(-sy * cp, -sp, cy * cp);
        f.right = new Vec3d(-cy, 0, -sy);
        f.up = new Vec3d(-sy * sp, cp, cy * sp);
        f.w = mc.getWindow().getScaledWidth();
        f.h = mc.getWindow().getScaledHeight();
        f.t = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        f.aspect = (double) f.w / (double) f.h;
        return f;
    }
}
