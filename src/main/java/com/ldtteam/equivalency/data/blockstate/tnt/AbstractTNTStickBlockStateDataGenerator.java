package com.ldtteam.equivalency.data.blockstate.tnt;

import com.google.common.collect.Maps;
import com.ldtteam.datagenerators.blockstate.BlockstateJson;
import com.ldtteam.datagenerators.blockstate.BlockstateModelJson;
import com.ldtteam.datagenerators.blockstate.BlockstateVariantJson;
import com.ldtteam.equivalency.api.util.Constants;
import com.ldtteam.equivalency.api.util.DataGeneratorUtils;
import com.ldtteam.equivalency.api.util.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractTNTStickBlockStateDataGenerator implements IDataProvider
{
    private final DataGenerator generator;

    protected AbstractTNTStickBlockStateDataGenerator(DataGenerator generator) {this.generator = generator;}


    @Override
    public void act(final DirectoryCache cache) throws IOException
    {
        createBlockStateFile(cache);
    }

    private void createBlockStateFile(final DirectoryCache cache) throws IOException
    {
        if (getBlock() == null)
            return;

        if (getBlock().getRegistryName() == null)
            return;

        final Map<String, BlockstateVariantJson> variants = Maps.newHashMap();
        BlockStateProperties.FACING.getAllowedValues().forEach(direction -> {
            BlockStateProperties.UNSTABLE.getAllowedValues().forEach(stable -> {
                final String variantKey = "facing=" + direction + ",unstable=" + stable;

                String modelFile = new ResourceLocation(Constants.MOD_ID, getModelPath()).toString();
                int rotateX = DataGeneratorUtils.getXRotationFromFacing(direction);
                int rotateY = DataGeneratorUtils.getYRotationFromFacing(direction);

                final BlockstateModelJson model = new BlockstateModelJson(modelFile, rotateX, rotateY);
                variants.put(variantKey, new BlockstateVariantJson(model));
            });
        });

        final BlockstateJson blockstateJson = new BlockstateJson(variants);
        final Path blockstateFolder = this.generator.getOutputFolder().resolve(Constants.DataGenerator.BLOCKSTATE_DIR);
        final Path blockstatePath = blockstateFolder.resolve(getBlock().getRegistryName().getPath() + ".json");

        IDataProvider.save(Constants.DataGenerator.GSON, cache, blockstateJson.serialize(), blockstatePath);
    }

    protected abstract Block getBlock();

    protected abstract String getModelPath();
}
