package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AlgorithmService {

    public boolean mainAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph, int maxCenters, int maxClientsPerCenter) {
        List<DefaultWeightedEdge> edges = new ArrayList<>(graph.edgeSet());
        Comparator<DefaultWeightedEdge> byWeight = getDefaultWeightedEdgeComparator(graph);
        edges.sort(byWeight);

        List<Graph<Vertex, DefaultWeightedEdge>> subGraphs = new ArrayList<>();
        edges.forEach(edge -> {
            double maxWeight = graph.getEdgeWeight(edge);
            Graph<Vertex, DefaultWeightedEdge> subGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
            graph.vertexSet().forEach(subGraph::addVertex);
            addEdgesUpToMaxWeightToSubGraph(graph, subGraph, maxWeight);
            subGraphs.add(subGraph);
        });

        for (Graph<Vertex, DefaultWeightedEdge> subGraph : subGraphs) {
            if (assignCentersAlgorithm(subGraph, maxCenters, maxClientsPerCenter)) {
                return true;
            }
        }
        return false;
    }

    private boolean assignCentersAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph, int maxCenters, int maxClientsPerCenter) {
        ConnectivityInspector<Vertex, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(graph);

        List<Set<Vertex>> connectedComponents = connectivityInspector.connectedSets();

        List<Integer> componentNodeCount = getComponentNodeCount(connectedComponents);

        List<Integer> requiredCentersPerComponent = getRequiredCentersPerComponent(maxClientsPerCenter, componentNodeCount);

        int requiredCenters = getRequiredCenters(requiredCentersPerComponent);

        if (requiredCenters > maxCenters) {
            return false;
        }

        Set<Graph<Vertex, DefaultWeightedEdge>> subGraphs =
                connectedComponents
                        .stream()
                        .map(vertices -> getSubGraph(graph, vertices))
                        .collect(Collectors.toSet());

        subGraphs.forEach(subGraph -> {
            selectMonarchsAlgorithm(subGraph);
            assignDomains(subGraph);
            reAssign(subGraph);
        });

        return true;
    }

    private void selectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void assignDomains(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void reAssign(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private Graph<Vertex, DefaultWeightedEdge> getSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> vertices) {
        Graph<Vertex, DefaultWeightedEdge> subgraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        vertices.forEach(subgraph::addVertex);
        List<Vertex> vertexList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices.size() - 1; ++i) {
            for (int j = i + 1; j < vertices.size(); ++j) {
                Vertex vertex1 = vertexList.get(i);
                Vertex vertex2 = vertexList.get(j);
                if (graph.containsEdge(vertex1, vertex2)) {
                    subgraph.addEdge(vertex1, vertex2, graph.getEdge(vertex1, vertex2));
                }
            }
        }
        return subgraph;
    }

    private void addEdgesUpToMaxWeightToSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Graph<Vertex, DefaultWeightedEdge> subGraph, double maxWeight) {
        graph.edgeSet()
                .stream()
                .filter(edgeToFilter -> graph.getEdgeWeight(edgeToFilter) <= maxWeight)
                .collect(Collectors.toList())
                .forEach(edgeToAdd -> subGraph
                        .addEdge(
                                graph.getEdgeSource(edgeToAdd),
                                graph.getEdgeTarget(edgeToAdd),
                                edgeToAdd));
    }

    private Comparator<DefaultWeightedEdge> getDefaultWeightedEdgeComparator(Graph<Vertex, DefaultWeightedEdge> graph) {
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

    private List<Integer> getComponentNodeCount(List<Set<Vertex>> connectedComponents) {
        return connectedComponents
                .stream()
                .map(Set::size)
                .collect(Collectors.toList());
    }

    private List<Integer> getRequiredCentersPerComponent(int maxClientsPerCenter, List<Integer> componentNodeCount) {
        return componentNodeCount
                .stream()
                .map(cnc ->
                        (int) Math.ceil((double) cnc / maxClientsPerCenter))
                .collect(Collectors.toList());
    }

    private int getRequiredCenters(List<Integer> requiredCentersPerComponent) {
        int requiredCenters = 0;
        for (int requiredCenterForComponent : requiredCentersPerComponent) {
            requiredCenters += requiredCenterForComponent;
        }
        return requiredCenters;
    }

    private List<Vertex> getAdjacentVerticesUpToDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        List<Vertex> adjacentVertices = Graphs.neighborListOf(graph, source);
        List<Vertex> vertices = new ArrayList<>(adjacentVertices);
        if (distance > 1) {
            adjacentVertices.forEach(adjacentVertex ->
                    vertices.addAll(getAdjacentVerticesUpToDistance(graph, adjacentVertex, distance - 1)));
        }
        return vertices;
    }

    private List<Vertex> getAdjacentVerticesAtDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        List<Vertex> vertices = new ArrayList<>();
        List<Vertex> adjacentVertices = Graphs.neighborListOf(graph, source);
        if (distance == 0) {
            vertices.addAll(adjacentVertices);
        } else {
            adjacentVertices.forEach(vertex -> vertices.addAll(getAdjacentVerticesAtDistance(graph, vertex, distance - 1)));
        }
        return vertices;
    }
}
