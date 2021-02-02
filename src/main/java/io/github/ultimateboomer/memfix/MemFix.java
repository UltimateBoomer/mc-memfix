package io.github.ultimateboomer.memfix;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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

public class MemFix implements ModInitializer {
	public static final String MOD_NAME = "MemFix";
	public static final String MOD_ID = "memfix";

	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public static final Map<Identifier, SpriteAtlasTexture.Data> dataMap = Maps.newConcurrentMap();

	public static final Set<NativeImage> nativeImageList = Sets.newConcurrentHashSet();

	public static CompletableFuture<Void> exportImagesFuture = null;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	@Override
	public void onInitialize() {
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
		long nativeImageSize = 0;
		try {
			nativeImageSize = CompletableFuture.supplyAsync(() -> nativeImageList.parallelStream().mapToLong(value ->
					value.getWidth() * value.getHeight()).sum(), Util.getMainWorkerExecutor()).get();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

//		exportTextures(client.runDirectory);

//		try {
//			MemFix.LOGGER.info("Allocating image");
//			NativeImage image = new NativeImage(NativeImage.Format.ABGR, 8192, 8192, false);
//			image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0xFFFFFFFF);
//
//			MemFix.LOGGER.info("Calling getBytes");
//			for (int i = 0; i < 100; i++) {
//				int x = image.getBytes().length;
//			}
//
//			image.close();
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}

//		try {
//			for (NativeImage image : nativeImageList) {
//				image.getBytes();
//			}
//		} catch (Exception e) {
//			throw new IllegalStateException(e);
//		}

//		for (NativeImage image : nativeImageList) {
//			image.close();
//		}

//		nativeImageList.parallelStream().forEach(image -> image.close());

//		try {
//			WritableByteChannel channel = Channels.newChannel(NullOutputStream.NULL_OUTPUT_STREAM);
//			for (NativeImage image : nativeImageList) {
//				image.write(channel);
//			}
//			channel.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

//		MemFix.LOGGER.info("Complete");

		client.player.sendMessage(new LiteralText(
				String.format("NativeImage count: %d", nativeImageList.size())), false);
		client.player.sendMessage(new LiteralText(
				String.format("NativeImage total size: %d", nativeImageSize)), false);
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
}
