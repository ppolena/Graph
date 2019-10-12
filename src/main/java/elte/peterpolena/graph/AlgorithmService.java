package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.intersection;

@Service
public class AlgorithmService {

    public boolean mainAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph,
                                 int maxCenters,
                                 int maxClientsPerCenter,
                                 int maxFailedCenters,
                                 boolean isConservative) {
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
            if (assignCentersAlgorithm(subGraph, maxCenters, maxClientsPerCenter, maxFailedCenters, isConservative)) {
                return true;
            }
        }
        return false;
    }

    private boolean assignCentersAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph,
                                           int maxCenters,
                                           int maxClientsPerCenter,
                                           int maxFailedCenters,
                                           boolean isConservative) {
        ConnectivityInspector<Vertex, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(graph);

        List<Set<Vertex>> connectedComponents = connectivityInspector.connectedSets();

        List<Integer> componentNodeCount = getComponentNodeCount(connectedComponents);

        List<Integer> requiredCentersPerComponent = getRequiredCentersPerComponent(maxClientsPerCenter, componentNodeCount);

        int requiredCenters = getRequiredCenters(requiredCentersPerComponent);

        if (requiredCenters > maxCenters) {
            return false;
        }

        m1 = new HashSet<>();
        m2 = new HashSet<>();
        m = new HashSet<>();

        Set<Graph<Vertex, DefaultWeightedEdge>> subGraphs =
                connectedComponents
                        .stream()
                        .map(vertices -> getSubGraph(graph, vertices))
                        .collect(Collectors.toSet());

        if (isConservative) {
            subGraphs.forEach(x -> callConservativeAlgorithms(x, maxCenters, maxClientsPerCenter, maxFailedCenters));
        } else {
            subGraphs.forEach(x -> callNonConservativeAlgorithms(x, maxCenters, maxClientsPerCenter, maxFailedCenters));
        }

        return true;
    }

    private void callNonConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                               int maxCenters,
                                               int maxClientsPerCenter,
                                               int maxFailedCenters) {
        nonConservativeSelectMonarchsAlgorithm(subGraph, maxFailedCenters);
        nonConservativeAssignDomainsAlgorithm(subGraph);
        nonConservativeReAssignAlgorithm(subGraph, maxClientsPerCenter);
    }

    private void callConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                            int maxCenters,
                                            int maxClientsPerCenter,
                                            int maxFailedCenters) {
        conservativeSelectMonarchsAlgorithm(subGraph);
        conservativeAssignDomainsAlgorithm(subGraph);
        conservativeReAssignAlgorithm(subGraph);
    }

    private void nonConservativeSelectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxFailedCenters) {
        List<Vertex> unmarkedNodes = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>(subGraph.vertexSet());
        unmarkedNodes.add(vertices.stream().findAny().get());
        while (!unmarkedNodes.isEmpty()) {
            Vertex vertex = unmarkedNodes.stream().findAny().get();
            unmarkedNodes.remove(vertex);
            vertex.setMonarch();
            vertex.setMarked();
            m1.add(vertex);
            //vertex.setParent(Parent(v))???
            getAdjacentVerticesUpToDistance(subGraph, vertex, 2).forEach(adjacentVertex -> {
                if (!adjacentVertex.isMarked()) {
                    adjacentVertex.setMarked();
                    vertex.addToEmpire(adjacentVertex);
                }
            });
            intersection(vertex.getEmpire(), getAdjacentVerticesAtDistance(subGraph, vertex, 2))
                    .forEach(u ->
                            getAdjacentVerticesAtDistance(subGraph, vertex, 1).forEach(w -> {
                                if (!w.isMarked() && !unmarkedNodes.contains(w)) {
                                    w.setParent(vertex);
                                    w.setDeputy(u);
                                    unmarkedNodes.add(w);
                                }
                            }));
        }
        for(Vertex m : m1) {
            List<Vertex> N1 = getAdjacentVerticesAtDistance(subGraph, m, 1);
            N1.remove(m.getDeputy());
            m.addAllToMinors(getRandomNVertex(N1, maxFailedCenters - 1));
            m2.addAll(m.getMinors());
            for(Vertex v : m.getMinors())
                v.addToMajors(m);
            m.addToMajors(m);
        }
        // M = M1 UNION M2
        m.addAll(m1);
        m.addAll(m2);
    }

    private void nonConservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        DefaultWeightedEdge e;
        Graph<Vertex, DefaultWeightedEdge> G2 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        //construct bipartite graph
        for(Vertex v : subGraph.vertexSet())
            G2.addVertex(v);
        for(Vertex m : m)
            G2.addVertex(new Vertex(m.getX(), m.getY(), Color.WHITE)); //we need every element of M twice in this graph
        //white vertexes = elements at M side

        //edges
        for(Vertex m : m) {
            for(Vertex v : getAdjacentVerticesUpToDistance(subGraph, m, 2))
                G2.addEdge(new Vertex(v.getX(), v.getY(), Color.WHITE), v); //TODO capacities 1, cost 1, or 0 if m==v
        }
        Vertex s = new Vertex(-1000, -1000, Color.BLUE);
        Vertex t = new Vertex(1000, 1000, Color.BLUE);;
        G2.addVertex(s);
        G2.addVertex(t);
        for(Vertex m : m)
            G2.addEdge(s, new Vertex(m.getX(), m.getY(), Color.WHITE)); //TODO capacities L
        for(Vertex v : subGraph.vertexSet())
            G2.addEdge(v, t); //TODO capacities 1

        //TODO compute a min-cost maximum integral flow on G2
        for(Vertex v : m) {
            //TODO set dom(m) = {v | v receives one unit of flow from m in G2}
        }
    }

    private void nonConservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {
        Map<Vertex, Set<Vertex>> unassigned = new HashMap<>();
        Map<Vertex, Set<Vertex>> passed = new HashMap<>();
        for(Vertex m : m1) {
            Set<Vertex> temp = new HashSet<>();
            temp.add(m);
            temp.addAll(m.getEmpire());
            temp.removeAll(m.getDoms());
            unassigned.put(m, temp);
        }

        //TODO set passed empty for each node m in T

        while(true) { //TODO while(T is not empty
            Vertex m = new Vertex(0, 0, Color.WHITE); //TODO remove a leaf node from T
            int unassignedAndPassed = unassigned.get(m).size() + passed.get(m).size();
            int k = unassignedAndPassed / maxClientsPerCenter;
            int e = unassignedAndPassed % maxClientsPerCenter;
            //getRandomNVertex(m.getEmpire(), k).forEach(x -> x.set..); //allocate k new centers at free nodes in m's empire
            //TODO assign K*maxClientsPerCenter nodes to them
        }
    }

    private void nonConservativeReAssignByFailedAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void conservativeSelectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void conservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void conservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
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
    private Collection<Vertex> getRandomNVertex(Collection<Vertex> vertices, int num) {
        List<Vertex> list = new ArrayList<Vertex>(vertices);
        Collections.shuffle(list);
        return list.subList(0, num);
    }


    private Set<Vertex> m1;
    private Set<Vertex> m2;
    private Set<Vertex> m;
}
