package org.ponkotate.craftisle.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.ponkotate.craftisle.CraftIsle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Shadow @Mutable
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void craftIsle$addModDataPacks(RepositorySource[] sourcesParam, CallbackInfo ci) {
        boolean hasServerPacks = false;
        boolean hasFolderSource = false;
        for (RepositorySource source : sourcesParam) {
            if (source instanceof ServerPacksSource) hasServerPacks = true;
            if (source instanceof FolderRepositorySource) hasFolderSource = true;
        }

        // Only inject in the Create World screen context: ServerPacksSource present, no FolderRepositorySource.
        // Fabric's own PackRepositoryMixin handles the FolderRepositorySource case (world loading context).
        if (!hasServerPacks || hasFolderSource) return;

        Set<RepositorySource> newSources = new HashSet<>(this.sources);
        newSources.add(onLoad -> FabricLoader.getInstance()
            .getModContainer(CraftIsle.MOD_ID)
            .ifPresent(container -> {
                List<Path> roots = container.getRootPaths();
                if (roots.isEmpty()) return;

                PackLocationInfo locationInfo = new PackLocationInfo(
                    CraftIsle.MOD_ID,
                    Component.literal("Craft Isle"),
                    PackSource.BUILT_IN,
                    Optional.empty()
                );
                Pack.Metadata metadata = new Pack.Metadata(
                    Component.literal("Craft Isle world generation data"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of()
                );
                PackSelectionConfig selectionConfig = new PackSelectionConfig(
                    true,
                    Pack.Position.TOP,
                    false
                );
                Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                    @Override
                    public PackResources openPrimary(PackLocationInfo info) {
                        return new PathPackResources(info, roots.get(0));
                    }
                    @Override
                    public PackResources openFull(PackLocationInfo info, Pack.Metadata meta) {
                        return openPrimary(info);
                    }
                };
                onLoad.accept(new Pack(locationInfo, supplier, metadata, selectionConfig));
            })
        );
        this.sources = newSources;
    }
}
