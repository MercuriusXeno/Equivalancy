package com.ldtteam.equivalency.recipe;

import com.ldtteam.equivalency.api.compound.container.ICompoundContainer;
import com.ldtteam.equivalency.api.recipe.IEquivalencyRecipe;

import java.util.Set;
import java.util.stream.Collectors;

public class SimpleEquivalancyRecipe implements IEquivalencyRecipe
{
    private final Set<ICompoundContainer<?>> inputs;
    private final Set<ICompoundContainer<?>> outputs;

    public SimpleEquivalancyRecipe(
      final Set<ICompoundContainer<?>> inputs,
      final Set<ICompoundContainer<?>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * The compound containers that are the input for this recipe.
     *
     * @return The inputs.
     */
    @Override
    public Set<ICompoundContainer<?>> getInputs()
    {
        return inputs;
    }

    /**
     * The compound containers that are the output for this recipe.
     *
     * @return The output.
     */
    @Override
    public Set<ICompoundContainer<?>> getOutputs()
    {
        return outputs;
    }

    /**
     * Returns the offset factor between inputs and outputs.
     */
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
        if (!(o instanceof SimpleEquivalancyRecipe))
        {
            return false;
        }

        final SimpleEquivalancyRecipe that = (SimpleEquivalancyRecipe) o;

        if (getInputs() != null ? !getInputs().equals(that.getInputs()) : that.getInputs() != null)
        {
            return false;
        }
        return getOutputs() != null ? getOutputs().equals(that.getOutputs()) : that.getOutputs() == null;
    }

    @Override
    public int hashCode()
    {
        int result = getInputs() != null ? getInputs().hashCode() : 0;
        result = 31 * result + (getOutputs() != null ? getOutputs().hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "SimpleEquivalancyRecipe{" +
                 "inputs=" + inputs.stream().map(w -> w == null ? "<NULL>" : w.toString()).collect(Collectors.joining(",")) +
                 ", outputs=" + outputs.stream().map(w -> w == null ? "<NULL>" : w.toString()).collect(Collectors.joining(",")) +
                 '}';
    }
}
