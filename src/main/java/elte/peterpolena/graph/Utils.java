package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.intersection;

public class Utils {

    public static Graph<Vertex, DefaultWeightedEdge> getSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> vertices) {
        Graph<Vertex, DefaultWeightedEdge> subGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        vertices.forEach(subGraph::addVertex);
        List<Vertex> vertexList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices.size() - 1; ++i) {
            for (int j = i + 1; j < vertices.size(); ++j) {
                Vertex vertex1 = vertexList.get(i);
                Vertex vertex2 = vertexList.get(j);
                if (graph.containsEdge(vertex1, vertex2)) {
                    subGraph.addEdge(vertex1, vertex2, graph.getEdge(vertex1, vertex2));
                }
            }
        }
        return subGraph;
    }

    public static void addEdgesUpToMaxWeightToSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Graph<Vertex, DefaultWeightedEdge> subGraph, double maxWeight) {
        graph.edgeSet()
                .stream()
                .filter(edgeToFilter -> graph.getEdgeWeight(edgeToFilter) <= maxWeight)
                .collect(toList())
                .forEach(edgeToAdd -> subGraph
                        .addEdge(
                                graph.getEdgeSource(edgeToAdd),
                                graph.getEdgeTarget(edgeToAdd),
                                edgeToAdd));
    }

    public static Comparator<DefaultWeightedEdge> getDefaultWeightedEdgeComparator(Graph<Vertex, DefaultWeightedEdge> graph) {
        return (DefaultWeightedEdge edge1, DefaultWeightedEdge edge2) -> {
            if (graph.getEdgeWeight(edge1) < graph.getEdgeWeight(edge2)) {
                return -1;
            }
            if (graph.getEdgeWeight(edge1) > graph.getEdgeWeight(edge2)) {
                return 1;
            }
            return 0;
        };
    }

    public static List<Integer> getComponentNodeCount(List<Set<Vertex>> connectedComponents) {
        return connectedComponents
                .stream()
                .map(Set::size)
                .collect(toList());
    }

    public static List<Integer> getRequiredCentersPerComponent(int maxClientsPerCenter, List<Integer> componentNodeCount) {
        return componentNodeCount
                .stream()
                .map(cnc ->
                        (int) Math.ceil((double) cnc / maxClientsPerCenter))
                .collect(toList());
    }

    public static int getRequiredCenters(List<Integer> requiredCentersPerComponent) {
        int requiredCenters = 0;
        for (int requiredCenterForComponent : requiredCentersPerComponent) {
            requiredCenters += requiredCenterForComponent;
        }
        return requiredCenters;
    }

    public static int getEdgeCapacity(Graph<Vertex, WeightedEdgeWithCapacity> graph, WeightedEdgeWithCapacity edge) {
        return graph.getEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)).getCapacity();
    }

    public static int getVertexSupply(Vertex source, Vertex target, Vertex vertex) {
        if (vertex.equals(source)) {
            return 1;
        } else if (vertex.equals(target)) {
            return -1;
        } else {
            return 0;
        }
    }

    public static Set<Vertex> getClients(Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph, Map<WeightedEdgeWithCapacity, Double> flowMap, Vertex monarch) {
        return flowMap
                .keySet()
                .stream()
                .filter(edge -> bipartiteGraph
                        .getEdgeSource(edge)
                        .equals(monarch) && flowMap.get(edge) != 0)
                .collect(toSet())
                .stream()
                .map(bipartiteGraph::getEdgeTarget)
                .collect(toSet());
    }

    public static List<Vertex> getFreeNodes(List<Vertex> vertices) {
        return vertices
                .stream()
                .filter(node -> node.getColor().equals(BLACK))
                .collect(toList());
    }

    public static List<Vertex> getTreePathTo(Vertex from, Vertex to) {
        List<Vertex> ret = new ArrayList<>();
        if (from == to) {
            ret.add(from);
            return ret;
        }

        List<Vertex> fromPath = new ArrayList<>();
        Vertex iter = from;
        fromPath.add(iter);
        while (iter.getParent() != null) {
            fromPath.add(iter);
            iter = iter.getParent();
        }
        Collections.reverse(fromPath);

        List<Vertex> toPath = new ArrayList<>();
        iter = from;
        toPath.add(iter);
        while (iter.getParent() != null) {
            toPath.add(iter);
            iter = iter.getParent();
        }
        Collections.reverse(toPath);

        while (!fromPath.isEmpty() && !toPath.isEmpty() && fromPath.get(0) == toPath.get(0)) {
            fromPath.remove(0);
            toPath.remove(0);
        }
        Collections.reverse(fromPath);
        ret.addAll(fromPath);
        if (!toPath.isEmpty() && toPath.get(0).getParent() != null)
            ret.add(toPath.get(0).getParent());
        ret.addAll(toPath);

        return ret;
    }

    public static List<Vertex> getAdjacentVerticesUpToDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        List<Vertex> adjacentVertices = Graphs.neighborListOf(graph, source);
        Set<Vertex> vertices = new HashSet<>(adjacentVertices);
        vertices.add(source);
        if (distance > 1) {
            adjacentVertices.forEach(adjacentVertex ->
                    vertices.addAll(getAdjacentVerticesUpToDistance(graph, adjacentVertex, distance - 1)));
        }
        //vertices.remove(source);
        return new ArrayList<>(vertices);
    }

    public static List<Vertex> getAdjacentVerticesAtDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        Set<Vertex> vertices = new HashSet<>();
        vertices.addAll(getAdjacentVerticesUpToDistance(graph, source, distance));
        if(distance > 1)
            vertices.removeAll(getAdjacentVerticesUpToDistance(graph, source, distance -1));
        vertices.remove(source);
        return new ArrayList<>(vertices);
    }

    public static List<Vertex> shuffleAndReduceToSize(List<Vertex> vertices, int size) {
        List<Vertex> list = new ArrayList<>(vertices);
        Collections.shuffle(list);
        if (size > list.size()) {
            return list;
        } else if (size < 0) {
            return new ArrayList<>();
        }
        return list.subList(0, size);
    }

    public static boolean hasUnmarkedNodesFurther(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> fromSet, int distance) {
        Set<Vertex> allVertex = new HashSet<>(graph.vertexSet());
        fromSet.forEach(x -> allVertex.removeAll(getAdjacentVerticesUpToDistance(graph, x, distance)));
        return !allVertex.isEmpty();
    }

    public static Vertex getRandomVertexFromDistance(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> distanceFrom, List<Vertex> fromList, int distance) {
        Set<Vertex> allVertex = new HashSet<>();
        distanceFrom.forEach(x -> allVertex.addAll(getAdjacentVerticesAtDistance(graph, x, distance)));
        distanceFrom.forEach(x -> allVertex.removeAll(getAdjacentVerticesUpToDistance(graph, x, distance - 1)));

        return intersection(allVertex, fromList).stream().findAny().get();
    }

    public static Vertex getALeaf(Set<Vertex> tree) {
        return tree.stream().filter(x -> tree.stream().noneMatch(y -> y.getParent() == x)).findAny().get();
    }

    public static Graph<Vertex, DefaultWeightedEdge> copy(Graph<Vertex, DefaultWeightedEdge> graph) {
        Graph<Vertex, DefaultWeightedEdge> copy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		if (!graph.edgeSet().isEmpty()) {
			graph.edgeSet().forEach(edge -> {
				Vertex source = new Vertex(graph.getEdgeSource(edge));
				Vertex target = new Vertex(graph.getEdgeTarget(edge));
				copy.addVertex(source);
				copy.addVertex(target);
				copy.addEdge(source, target);
				copy.setEdgeWeight(source, target, graph.getEdgeWeight(edge));
			});
		} else if (!graph.vertexSet().isEmpty()) {
			graph.vertexSet().forEach(vertex -> copy.addVertex(new Vertex(vertex)));
		}
        return copy;
    }

    public static Set<Vertex> getCenters(Graph<Vertex, DefaultWeightedEdge> graph) {
        return graph.vertexSet().stream().filter(vertex -> vertex.getColor().equals(RED)).collect(toSet());
    }

	public static long getCentersCount(Graph<Vertex, DefaultWeightedEdge> graph) {
        return getCenters(graph).size();
	}

    public static int levelOfNode(Vertex node) {
        int ret = 0;
        Vertex iter = node;
        while (iter.getParent() != null) {
            ret++;
            iter = iter.getParent();
        }
        return ret;
    }

    public static Set<Vertex> nodesAtLevel(Set<Vertex> tree, int neededLevel) {
        return tree.stream().filter(x -> levelOfNode(x) == neededLevel).collect(toSet());
    }
}
