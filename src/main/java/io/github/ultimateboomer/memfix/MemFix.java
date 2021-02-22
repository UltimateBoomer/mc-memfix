package io.github.ultimateboomer.memfix;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.ultimateboomer.memfix.config.MemFixConfig;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class MemFix implements ModInitializer {
    public static final String MOD_NAME = "MemFix";
    public static final String MOD_ID = "memfix";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    private static MemFixConfig config;

    private static final Map<Identifier, SpriteAtlasTexture.Data> dataMap = Maps.newConcurrentMap();

    public static final Set<NativeImage> nativeImageList = Sets.newConcurrentHashSet();

    public static final Set<NativeImage> closeOnReload = Sets.newConcurrentHashSet();

    private static AtomicBoolean textureLoaded = new AtomicBoolean();

    private static MemoryPool sharedMemoryPool;

    private static CompletableFuture<Void> exportImagesFuture = null;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    @Override
    public void onInitialize() {
        AutoConfig.register(MemFixConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(MemFixConfig.class).getConfig();

        sharedMemoryPool = new MemoryPool((long) config.poolSize << 20);

        KeyBinding keyDebug = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.memfix.test",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.memfix"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyDebug.wasPressed()) {
                if (client.player != null) {
                    test(client);
                }
            }
        });
    }

    public static void test(MinecraftClient client) {
//        NativeImagePool.PooledNativeImage last = nativeImagePool.pooledNativeImageSet.last();
//        exportTextures(client.runDirectory)
//        sharedMemoryPool.compress();

        client.player.sendMessage(new LiteralText(
                String.format("NativeImage count: %d", nativeImageList.size())), false);
        client.player.sendMessage(new LiteralText(
                String.format("Pool fill: %d", getSharedMemoryPool().getFill())), false);
        client.player.sendMessage(new LiteralText(
                String.format("Pool usage: %d", getSharedMemoryPool().getUsage())), false);
        client.player.sendMessage(new LiteralText(
                String.format("Pool handle count: %d", getSharedMemoryPool().getHandleCount())), false);
//        exportTextures(client.runDirectory);
    }

    public static CompletableFuture<Void> exportTextures(File runDirectory) {
        if (exportImagesFuture != null && !exportImagesFuture.isDone()) {
            return exportImagesFuture;
        }

        exportImagesFuture = CompletableFuture.runAsync(() -> {
            File imageDir = new File(runDirectory, String.format("memfix/%s",
                    DATE_FORMAT.format(new Date())));
            if (!imageDir.mkdirs()) {
                LOGGER.warn("Failed to create directory {}", imageDir);
                return;
            }
            LOGGER.info("Exporting to {}", imageDir);

            nativeImageList.parallelStream().forEach(image -> {
                File imageFile = new File(imageDir, image.toString() + ".png");
                try {
                    imageFile.createNewFile();
                    image.writeFile(imageFile);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

            });

            LOGGER.info("Export Complete");
        }, Util.getMainWorkerExecutor());
        return exportImagesFuture;
    }

    public static MemoryPool getSharedMemoryPool() {
        return sharedMemoryPool;
    }

    public static AtomicBoolean getTextureLoaded() {
        return textureLoaded;
    }
}
