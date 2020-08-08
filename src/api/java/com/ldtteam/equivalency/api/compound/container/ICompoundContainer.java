package com.ldtteam.equivalency.api.compound.container;

/**
 * Holds a object that is made up out of compounds.
 * @param <T> The type of game object that is held.
 */
public interface ICompoundContainer<T> extends Comparable<ICompoundContainer<?>>
{

    /**
     * The contents of this container.
     * Set to the 1 unit of the content type {@link T}
     *
     * @return The contents.
     */
    T getContents();

    /**
     * The amount of {@link T}s contained in this wrapper.
     * @return The amount.
     */
    Double getContentsCount();
}
