package com.ldtteam.equivalency.analyzer;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.ldtteam.equivalency.analyzer.jgrapht.ContainerWrapperGraphNode;
import com.ldtteam.equivalency.analyzer.jgrapht.IAnalysisGraphNode;
import com.ldtteam.equivalency.analyzer.jgrapht.RecipeGraphNode;
import com.ldtteam.equivalency.analyzer.jgrapht.SourceGraphNode;
import com.ldtteam.equivalency.api.EquivalencyApi;
import com.ldtteam.equivalency.api.compound.ICompoundInstance;
import com.ldtteam.equivalency.api.compound.container.wrapper.ICompoundContainerWrapper;
import com.ldtteam.equivalency.api.recipe.IEquivalencyRecipeRegistry;
import com.ldtteam.equivalency.api.util.EquivalencyLogger;
import com.ldtteam.equivalency.compound.SimpleCompoundInstance;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;
import java.util.stream.Collectors;

public class JGraphTBasedCompoundAnalyzer
{

    public Map<ICompoundContainerWrapper<?>, Set<ICompoundInstance>> calculate(@NotNull final IEquivalencyRecipeRegistry registry)
    {
        Validate.notNull(registry);
        final Map<ICompoundContainerWrapper<?>, Set<ICompoundInstance>> resultingCompounds = new TreeMap<>();

        final Graph<IAnalysisGraphNode, DefaultWeightedEdge> recipeGraph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        final Map<ICompoundContainerWrapper<?>, IAnalysisGraphNode> nodes = Maps.newConcurrentMap();

        registry
          .getRecipes()
          .forEach(recipe -> {
              final IAnalysisGraphNode recipeGraphNode = new RecipeGraphNode(recipe);

              recipeGraph.addVertex(recipeGraphNode);

              //Process inputs
              recipe.getInputs().forEach(input -> {
                  final ICompoundContainerWrapper<?> unitInputWrapper = createUnitWrapper(input);
                  nodes.putIfAbsent(unitInputWrapper, new ContainerWrapperGraphNode(unitInputWrapper));

                  final IAnalysisGraphNode inputWrapperGraphNode = nodes.get(unitInputWrapper);

                  resultingCompounds.putIfAbsent(unitInputWrapper, new TreeSet<>());
                  recipeGraph.addVertex(inputWrapperGraphNode);

                  recipeGraph.addEdge(inputWrapperGraphNode, recipeGraphNode);
                  recipeGraph.setEdgeWeight(inputWrapperGraphNode, recipeGraphNode, input.getContentsCount());
              });

              //Process outputs
              recipe.getOutputs().forEach(output -> {
                  final ICompoundContainerWrapper<?> unitOutputWrapper = createUnitWrapper(output);
                  nodes.putIfAbsent(unitOutputWrapper, new ContainerWrapperGraphNode(unitOutputWrapper));

                  final IAnalysisGraphNode outputWrapperGraphNode = nodes.get(unitOutputWrapper);

                  resultingCompounds.putIfAbsent(unitOutputWrapper, new TreeSet<>());
                  recipeGraph.addVertex(outputWrapperGraphNode);

                  recipeGraph.addEdge(recipeGraphNode, outputWrapperGraphNode);
                  recipeGraph.setEdgeWeight(recipeGraphNode, outputWrapperGraphNode, output.getContentsCount());
              });
          });

        final Set<ContainerWrapperGraphNode> rootNodes = findRootNodes(recipeGraph);

        final Set<ContainerWrapperGraphNode> notDefinedGraphNodes = rootNodes
          .stream()
          .filter(n -> getLockedInformationInstances(n.getWrapper()).isEmpty())
          .collect(Collectors.toSet());

        rootNodes.removeAll(notDefinedGraphNodes);

        notDefinedGraphNodes
          .forEach(node -> {
              EquivalencyLogger.bigWarning(String.format("Missing root information for: %s. Removing from recipe graph.", node.getWrapper()));
              recipeGraph.removeVertex(node);
          });

        removeDanglingNodes(recipeGraph, rootNodes);

        final SourceGraphNode source = new SourceGraphNode();
        recipeGraph.addVertex(source);

        rootNodes
          .forEach(rootNode -> {
              recipeGraph.addEdge(source, rootNode);
              recipeGraph.setEdgeWeight(source, rootNode, 1d);
          });

        processRecipeGraphUsingBreathFirstSearch(recipeGraph);

        recipeGraph
          .vertexSet()
          .stream()
          .filter(v -> v instanceof ContainerWrapperGraphNode)
          .map(v-> (ContainerWrapperGraphNode) v)
          .forEach(
            v -> resultingCompounds.get(v.getWrapper()).addAll(v.getCompoundInstances())
          );

        return resultingCompounds;
    }

    private void removeDanglingNodes(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> recipeGraph,
      @NotNull final Set<ContainerWrapperGraphNode> rootNodes)
    {
        @NotNull Set<IAnalysisGraphNode> danglingNodesToDelete = findDanglingNodes(recipeGraph)
          .stream()
          .filter(n -> !rootNodes.contains(n))
          .collect(Collectors.toSet());

        while(!danglingNodesToDelete.isEmpty())
        {
            danglingNodesToDelete
              .forEach(recipeGraph::removeVertex);

            danglingNodesToDelete = findDanglingNodes(recipeGraph)
             .stream()
             .filter(n -> !rootNodes.contains(n))
             .collect(Collectors.toSet());
        }
    }

    private void processRecipeGraphUsingBreathFirstSearch(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> recipeGraph)
    {
        final LinkedHashSet<IAnalysisGraphNode> processingQueue = new LinkedHashSet<>();
        processingQueue.add(new SourceGraphNode());

        processRecipeGraphFromNodeUsingBreathFirstSearch(recipeGraph, processingQueue);
    }

    private void processRecipeGraphFromNodeUsingBreathFirstSearch(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> recipeGraph, final LinkedHashSet<IAnalysisGraphNode> processingQueue)
    {
        final Set<IAnalysisGraphNode> visitedNodes = new LinkedHashSet<>();

        while(!processingQueue.isEmpty())
        {
            final Iterator<IAnalysisGraphNode> nodeIterator = processingQueue.iterator();
            final IAnalysisGraphNode node = nodeIterator.next();
            nodeIterator.remove();

            processRecipeGraphForNodeWithBFS(recipeGraph, processingQueue, visitedNodes, node);
        }
    }

    private void processRecipeGraphForNodeWithBFS(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> recipeGraph, final LinkedHashSet<IAnalysisGraphNode> processingQueue, final Set<IAnalysisGraphNode> visitedNodes, final IAnalysisGraphNode node)
    {
        visitedNodes.add(node);
        final Class<? extends IAnalysisGraphNode> clazz = node.getClass();

        Set<IAnalysisGraphNode> nextIterationNodes = Sets.newHashSet();

        EquivalencyLogger.info(String.format("Processing node: %s", node));

        if(clazz == SourceGraphNode.class)
        {
            final Set<ContainerWrapperGraphNode> neighbors = recipeGraph
                                                               .outgoingEdgesOf(node)
                                                               .stream()
                                                               .map(recipeGraph::getEdgeTarget)
                                                               .filter(v -> v instanceof ContainerWrapperGraphNode)
                                                               .map(v -> (ContainerWrapperGraphNode) v)
                                                               .collect(Collectors.toSet());

            neighbors.forEach(rootNode -> {
                //This root node should have embedded information.
                final ICompoundContainerWrapper<?> unitWrapper = rootNode.getWrapper();
                rootNode.getCompoundInstances().clear();
                rootNode.getCompoundInstances().addAll(getLockedInformationInstances(unitWrapper));
            });

            nextIterationNodes = new HashSet<>(neighbors);
        }
        if (clazz == ContainerWrapperGraphNode.class)
        {
            final Set<RecipeGraphNode> neighbors = recipeGraph
                                                     .outgoingEdgesOf(node)
                                                     .stream()
                                                     .map(recipeGraph::getEdgeTarget)
                                                     .filter(v -> v instanceof RecipeGraphNode)
                                                     .map(v -> (RecipeGraphNode) v)
                                                     .collect(Collectors.toSet());

            neighbors.forEach(neighbor -> neighbor.getAnalyzedInputNodes().add(node));

            nextIterationNodes = neighbors
                                   .stream()
                                   .filter(recipeGraphNode -> recipeGraphNode.getAnalyzedInputNodes().size() == recipeGraphNode.getRecipe().getInputs().size())
                                   .collect(Collectors.toSet());
        }
        if (clazz == RecipeGraphNode.class)
        {
            final Set<ContainerWrapperGraphNode> resultNeighbors = recipeGraph
                                                                     .outgoingEdgesOf(node)
                                                                     .stream()
                                                                     .map(recipeGraph::getEdgeTarget)
                                                                     .filter(v -> v instanceof ContainerWrapperGraphNode)
                                                                     .map(v -> (ContainerWrapperGraphNode) v)
                                                                     .collect(Collectors.toSet());

            final Set<ContainerWrapperGraphNode> inputNeightbors = recipeGraph
                                                                     .incomingEdgesOf(node)
                                                                     .stream()
                                                                     .map(recipeGraph::getEdgeSource)
                                                                     .filter(v -> v instanceof ContainerWrapperGraphNode)
                                                                     .map(v -> (ContainerWrapperGraphNode) v)
                                                                     .collect(Collectors.toSet());

            final Set<ICompoundInstance> summedCompoundInstances = inputNeightbors
                                                                     .stream()
                                                                     .flatMap(inputNeighbor-> inputNeighbor
                                                                                                .getCompoundInstances()
                                                                                                .stream()
                                                                                                .map(compoundInstance -> new HashMap.SimpleEntry<>(compoundInstance.getType(), compoundInstance.getAmount() * recipeGraph.getEdgeWeight(recipeGraph.getEdge(inputNeighbor, node)))))
                                                                     .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (val1, val2)-> val1 + val2))
                                                                     .entrySet()
                                                                     .stream()
                                                                     .map(entry -> new SimpleCompoundInstance(entry.getKey(), entry.getValue()))
                                                                     .collect(Collectors.toSet());

            final Set<IAnalysisGraphNode> nextNodes = new HashSet<>();

            resultNeighbors
              .forEach(neighbor -> {
                  //As of now. We do not update once calculated.
                  if (!neighbor.getCompoundInstances().isEmpty())
                      return;

                  final Set<ICompoundInstance> compoundInstances = getLockedInformationInstances(neighbor.getWrapper());

                  if (compoundInstances.isEmpty())
                  {
                      neighbor
                        .getCompoundInstances()
                        .addAll(
                          summedCompoundInstances
                            .stream()
                            .map(compoundInstance -> new SimpleCompoundInstance(compoundInstance.getType(), Math.floor(compoundInstance.getAmount() / recipeGraph.getEdgeWeight(recipeGraph.getEdge(node, neighbor)))))
                            .filter(simpleCompoundInstance -> simpleCompoundInstance.getAmount() > 0)
                            .filter(simpleCompoundInstance -> EquivalencyApi.getInstance().getValidCompoundTypeInformationProviderRegistry().isCompoundTypeValidForWrapper(neighbor.getWrapper(), simpleCompoundInstance.getType()))
                            .collect(Collectors.toSet())
                        );
                  }
                  else
                  {
                      neighbor
                        .getCompoundInstances()
                        .addAll(compoundInstances);
                  }

                  nextNodes.add(neighbor);
              });

            nextIterationNodes = nextNodes;
        }

        nextIterationNodes.removeIf(visitedNodes::contains);
        processingQueue.addAll(nextIterationNodes);
    }

    private ICompoundContainerWrapper<?> createUnitWrapper(@NotNull final ICompoundContainerWrapper<?> wrapper)
    {
        if (wrapper.getContentsCount() == 1d)
            return wrapper;

        return EquivalencyApi.getInstance().getCompoundContainerWrapperFactoryRegistry().wrapInContainer(wrapper.getContents(), 1d);
    }

    private Set<ContainerWrapperGraphNode> findRootNodes(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> graph)
    {
        return findDanglingNodes(graph)
          .stream()
          .filter(v -> v instanceof ContainerWrapperGraphNode)
          .map(v -> (ContainerWrapperGraphNode) v)
          .collect(Collectors.toSet());
    }

    private Set<IAnalysisGraphNode> findDanglingNodes(@NotNull final Graph<IAnalysisGraphNode, DefaultWeightedEdge> graph)
    {
        return graph
                 .vertexSet()
                 .stream()
                 .filter(v -> graph.incomingEdgesOf(v).isEmpty())
                 .collect(Collectors.toSet());
    }

    private Set<ICompoundInstance> getLockedInformationInstances(@NotNull final ICompoundContainerWrapper<?> wrapper)
    {
        final Set<ICompoundInstance> lockedInstances = EquivalencyApi
                                                         .getInstance()
                                                         .getLockedCompoundWrapperToTypeRegistry()
                                                         .getLockedInformation()
                                                         .get(createUnitWrapper(wrapper));

        if (lockedInstances != null)
            return lockedInstances;

        return new HashSet<>();
    }
}