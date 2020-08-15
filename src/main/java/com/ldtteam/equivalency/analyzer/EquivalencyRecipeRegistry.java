package com.ldtteam.equivalency.analyzer;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ldtteam.equivalency.api.recipe.equivalency.IEquivalencyRecipe;
import com.ldtteam.equivalency.api.recipe.equivalency.IEquivalencyRecipeRegistry;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;


import java.util.Map;
import java.util.Set;

public class EquivalencyRecipeRegistry implements IEquivalencyRecipeRegistry
{
    private static final Map<RegistryKey<World>, EquivalencyRecipeRegistry> INSTANCES = Maps.newConcurrentMap();

    public static EquivalencyRecipeRegistry getInstance(@NotNull final RegistryKey<World> worldKey)
    {
        return INSTANCES.computeIfAbsent(worldKey, (dimType) -> new EquivalencyRecipeRegistry());
    }

    private final Set<IEquivalencyRecipe> recipes = Sets.newConcurrentHashSet();

    private EquivalencyRecipeRegistry()
    {
    }

    /**
     * Adds a new recipe to the registry.
     *
     * @param recipe The recipe to add.
     * @return The registry.
     */
    @NotNull
    @Override
    public IEquivalencyRecipeRegistry register(@NotNull final IEquivalencyRecipe recipe)
    {
        recipes.add(recipe);
        return this;
    }

    public void reset()
    {
        recipes.clear();
    }

    @NotNull
    public Set<IEquivalencyRecipe> get()
    {
        return recipes;
    }
}
