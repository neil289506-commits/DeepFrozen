package backport.org;

import backport.org.worldgen.IceCaveFeature;
import backport.org.worldgen.IceCrevasseFeature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class ModFeatures {
	private ModFeatures() {}

	public static final MapCodec<? extends Feature> ICE_CAVE = Registry.register(
		BuiltInRegistries.FEATURE_TYPE, DeepFrozen.id("ice_cave"), IceCaveFeature.CODEC);
	public static final MapCodec<? extends Feature> ICE_CREVASSE = Registry.register(
		BuiltInRegistries.FEATURE_TYPE, DeepFrozen.id("crevasse"), IceCrevasseFeature.CODEC);

	public static void init() {}
}
