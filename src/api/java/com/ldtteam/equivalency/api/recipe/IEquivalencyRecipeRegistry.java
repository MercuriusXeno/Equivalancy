package com.ldtteam.equivalency.api.recipe;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface IEquivalencyRecipeRegistry
{

    /**
     * Adds a new recipe to the registry.
     *
     * @param recipe The recipe to add.
     *
     * @return The registry.
     */
    @NotNull
    IEquivalencyRecipeRegistry registerNewRecipe(@NotNull final IEquivalencyRecipe recipe);

    /**
     * The recipes.
     * @return The recipes.
     */
    @NotNull
    Set<IEquivalencyRecipe> getRecipes();
}