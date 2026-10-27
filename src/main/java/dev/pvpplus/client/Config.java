package dev.pvpplus.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
    public static class Data {
        public Map<String, Boolean> on = new HashMap<>();
        public Map<String, Double> num = new HashMap<>();
        public Map<String, int[]> pos = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data d = new Data();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvpplus_config.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            try (Reader r = Files.newBufferedReader(f)) {
                Data loaded = GSON.fromJson(r, Data.class);
                if (loaded != null) {
                    d = loaded;
                    if (d.on == null) d.on = new HashMap<>();
                    if (d.num == null) d.num = new HashMap<>();
                    if (d.pos == null) d.pos = new HashMap<>();
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(d, w);
        } catch (Exception ignored) {
        }
    }

    public static boolean on(String id) {
        Boolean v = d.on.get(id);
        return v != null ? v : Features.defOn(id);
    }

    public static void setOn(String id, boolean v) { d.on.put(id, v); }

    public static double num(String key) {
        Double v = d.num.get(key);
        return v != null ? v : Features.defNum(key);
    }

    public static void setNum(String key, double v) { d.num.put(key, v); }

    public static int i(String key) { return (int) Math.round(num(key)); }

    public static boolean b(String key) { return num(key) >= 0.5; }

    public static int color(String key) {
        return Features.COLORS[Math.floorMod(i(key), Features.COLORS.length)];
    }

    public static boolean isOn(String id) { return on(id); }
}
