package com.ldtteam.equivalency.recipe;

import com.ldtteam.equivalency.api.compound.container.ICompoundContainer;
import com.ldtteam.equivalency.api.recipe.IEquivalencyRecipe;
import net.minecraft.tags.Tag;

import java.util.Set;

public class TagEquivalencyRecipe<T> implements IEquivalencyRecipe
{
    private final Tag<T>                     tag;
    private final Set<ICompoundContainer<?>> inputs;
    private final Set<ICompoundContainer<?>> outputs;

    public TagEquivalencyRecipe(
      final Tag<T> tag,
      final Set<ICompoundContainer<?>> inputs,
      final Set<ICompoundContainer<?>> outputs)
    {
        this.tag = tag;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public Tag<T> getTag()
    {
        return tag;
    }

    @Override
    public Set<ICompoundContainer<?>> getInputs()
    {
        return inputs;
    }

    @Override
    public Set<ICompoundContainer<?>> getOutputs()
    {
        return outputs;
    }

    @Override
    public Double getOffsetFactor()
    {
        return 1D;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TagEquivalencyRecipe))
        {
            return false;
        }

        final TagEquivalencyRecipe that = (TagEquivalencyRecipe) o;

        if (getTag() != null ? !getTag().equals(that.getTag()) : that.getTag() != null)
        {
            return false;
        }
        if (getInputs() != null ? !getInputs().equals(that.getInputs()) : that.getInputs() != null)
        {
            return false;
        }
        return getOutputs() != null ? getOutputs().equals(that.getOutputs()) : that.getOutputs() == null;
    }

    @Override
    public int hashCode()
    {
        int result = getTag() != null ? getTag().hashCode() : 0;
        result = 31 * result + (getInputs() != null ? getInputs().hashCode() : 0);
        result = 31 * result + (getOutputs() != null ? getOutputs().hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "OreDictionaryEquivalencyRecipe{" +
                 "oreDictionaryName='" + getTag().getId() + '\'' +
                 ", inputs=" + inputs +
                 ", outputs=" + outputs +
                 '}';
    }
}
