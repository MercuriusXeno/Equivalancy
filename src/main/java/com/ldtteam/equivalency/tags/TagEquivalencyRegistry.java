package com.ldtteam.equivalency.tags;

import com.google.common.collect.Sets;
import com.ldtteam.equivalency.api.tags.ITagEquivalencyRegistry;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TagEquivalencyRegistry implements ITagEquivalencyRegistry
{
    private static final TagEquivalencyRegistry INSTANCE = new TagEquivalencyRegistry();

    public static TagEquivalencyRegistry getInstance()
    {
        return INSTANCE;
    }

    private final Set<ITag.INamedTag<?>> tags = Sets.newConcurrentHashSet();

    private TagEquivalencyRegistry()
    {
    }

    @Override
    public ITagEquivalencyRegistry addTag(@NotNull final ITag.INamedTag<?> tag)
    {
        this.tags.add(tag);
        return this;
    }

    public Set<ITag.INamedTag<?>> get()
    {
        return tags;
    }
}
