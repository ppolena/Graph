package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem.MinimumCostFlowProblemImpl;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elte.peterpolena.graph.Config.maxXCoordinate;
import static elte.peterpolena.graph.Config.maxYCoordinate;
import static elte.peterpolena.graph.Config.minXCoordinate;
import static elte.peterpolena.graph.Config.minYCoordinate;
import static java.awt.Color.BLUE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.intersection;

@Service
public class AlgorithmService {

/*
maxCenters = K
requiredCenters = Kw
maxClientsPerCenter = L
unmarkedNodes = Q
maxFailedCenters = α
source = s
target = t
getAdjacentVerticesUpToDistance(Gw, v, i) = Γi(v)
getAdjacentVerticesAtDistance(Gw, v, i) = Ni(v)
 */


    private Set<Vertex> m1 = new HashSet<>();
    private Set<Vertex> m2 = new HashSet<>();
    private Set<Vertex> m = new HashSet<>();

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

        Set<Graph<Vertex, DefaultWeightedEdge>> subGraphs =
                connectedComponents
                        .stream()
                        .map(vertices -> getSubGraph(graph, vertices))
                        .collect(toSet());

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
        nonConservativeAssignDomainsAlgorithm(subGraph, maxClientsPerCenter);
        nonConservativeReAssignAlgorithm(subGraph, maxClientsPerCenter);
        nonConservativeReAssignByFailedAlgorithm(subGraph);
    }

    private void callConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                            int maxCenters,
                                            int maxClientsPerCenter,
                                            int maxFailedCenters) {
        conservativeSelectMonarchsAlgorithm(subGraph, maxFailedCenters);
        conservativeAssignDomainsAlgorithm(subGraph);
        conservativeReAssignAlgorithm(subGraph, maxClientsPerCenter);
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
                            getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(w -> {
                                if (!w.isMarked() && !unmarkedNodes.contains(w)) {
                                    w.setParent(vertex);
                                    w.setDeputy(u);
                                    unmarkedNodes.add(w);
                                }
                            }));
        }

        m1.forEach(major -> {
            List<Vertex> minors = shuffleAndReduceToSize(
                    getAdjacentVerticesAtDistance(subGraph, major, 1)
                            .stream()
                            .filter(vertex -> !vertex.equals(major.getDeputy()))
                            .collect(toList()),
                    maxFailedCenters - 1);

            major.addMinors(minors);
            m2.addAll(minors);
            minors.forEach(minor -> minor.setMajor(major));
            major.setMajor(major);
        });

        // M = M1 UNION M2
        m.addAll(m1);
        m.addAll(m2);
    }

    private void nonConservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {

//        Refactor
//        The default color of generated nodes is BLACK, the color of centers should be RED, white is not very well visible.

//        Graph<Vertex, DefaultWeightedEdge> G2 = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//        //construct bipartite graph
//        for(Vertex v : subGraph.vertexSet())
//            G2.addVertex(v);
//        for(Vertex m : m)
//            G2.addVertex(new Vertex(m.getX(), m.getY(), Color.WHITE)); //we need every element of M twice in this graph
//        //white vertexes = elements at M side
//
//        //edges
//        for(Vertex m : m) {
//            for(Vertex v : getAdjacentVerticesUpToDistance(subGraph, m, 2))
//                G2.addEdge(new Vertex(v.getX(), v.getY(), Color.WHITE), v); //TODO capacities 1, cost 1, or 0 if m==v
//        }

        //Construct directed bipartite graph
        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        subGraph.vertexSet().forEach(bipartiteGraph::addVertex);
        //copy monarch set into bipartite graph
        Set<Vertex> monarchs = new HashSet<>(m);
        monarchs.forEach(bipartiteGraph::addVertex);
        //E'
        monarchs.forEach(monarch -> getAdjacentVerticesUpToDistance(subGraph, monarch.getMajor(), 2)
                .forEach(adjacentVertex -> bipartiteGraph.addEdge(monarch, adjacentVertex)));

//        G2.addVertex(s);
//        G2.addVertex(t);
//        for(Vertex m : m)
//            G2.addEdge(s, new Vertex(m.getX(), m.getY(), Color.WHITE)); //TODO capacities L
//        for(Vertex v : subGraph.vertexSet())
//            G2.addEdge(v, t); //TODO capacities 1
//
//        //TODO compute a min-cost maximum integral flow on G2
//        for(Vertex v : m) {
//            //TODO set dom(m) = {v | v receives one unit of flow from m in G2}
//        }

        //add s and t
        Vertex source = new Vertex(minXCoordinate + 10, minYCoordinate + 10, BLUE);
        Vertex target = new Vertex(maxXCoordinate - 10, maxYCoordinate - 10, BLUE);
        bipartiteGraph.addVertex(source);
        bipartiteGraph.addVertex(target);

        //for m ∈ M add edge (s, m) and set (s, m) capacity to L
        monarchs.forEach(monarch -> {
            bipartiteGraph.addEdge(source, monarch);
            bipartiteGraph.getEdge(source, monarch).setCapacity(maxClientsPerCenter);
        });

        //for v ∈ V add edge (v, t) and set (s, m) capacity to 1
        subGraph.vertexSet().forEach(vertex -> {
            bipartiteGraph.addEdge(vertex, target);
            bipartiteGraph.getEdge(vertex, target).setCapacity(1);
        });

        //for m ∈ M and v ∈ V set (m, v) capacity to 1 and if m = v set (m,v) weight to 0
        monarchs.forEach(monarch -> subGraph.vertexSet()
                .forEach(vertex -> {
                    bipartiteGraph.getEdge(monarch, vertex).setCapacity(1);
                    if (monarch.equals(vertex)) {
                        bipartiteGraph.setEdgeWeight(monarch, vertex, 0);
                    }
                }));

        //calculate minimum cost maximum flow
        Map<WeightedEdgeWithCapacity, Double> flowMap = new CapacityScalingMinimumCostFlow<Vertex, WeightedEdgeWithCapacity>()
                .getMinimumCostFlow(new MinimumCostFlowProblemImpl<>(
                        bipartiteGraph,
                        vertex -> getVertexSupply(source, target, vertex),
                        edge -> getEdgeCapacity(bipartiteGraph, edge), //max directed edge capacity
                        edge -> getEdgeCapacity(bipartiteGraph, edge))) //min directed edge capacity
                .getFlowMap();

        //for m ∈ M add v to dom(m) if v receives one unit of flow from m
        m.forEach(monarch -> monarch.setClients(getClients(bipartiteGraph, flowMap, monarch)));
    }

    private void nonConservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {
        Map<Vertex, Set<Vertex>> unassigned = new HashMap<>();
        Map<Vertex, Set<Vertex>> passed = new HashMap<>();
        for(Vertex monarch : m1) {
            Set<Vertex> temp = new HashSet<>();
            temp.add(monarch);
            temp.addAll(monarch.getEmpire());
            m.forEach(v -> temp.removeAll(v.getClients()));
            unassigned.put(monarch, temp);
        }

        Set<Vertex> monarchTree = new HashSet<>();
        monarchTree.addAll(m1);
        monarchTree.forEach(m -> passed.put(m, new HashSet<>()));

        while(!monarchTree.isEmpty()) {
            Vertex m = getALeaf(monarchTree);
            int unassignedAndPassed = unassigned.get(m).size() + passed.get(m).size();
            int k = unassignedAndPassed / maxClientsPerCenter;
            int e = unassignedAndPassed % maxClientsPerCenter;
            //shuffleAndReduceToSize(m.getEmpire(), k).forEach(x -> x.set..); //allocate k new centers at free nodes in m's empire
            //TODO assign K*maxClientsPerCenter nodes to them

            //TODO assign e remaining nodes to m, releasing e nodes in dom(m)
            //add the released nodes to passed(parent(m)) unless m is the root - then ...
            monarchTree.remove(m);
        }
    }

    private void nonConservativeReAssignByFailedAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO
    }

    private void conservativeSelectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxFailedCenters) {
        List<Vertex> unmarkedNodes = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>(subGraph.vertexSet());
        unmarkedNodes.add(vertices.stream().findAny().get());

        while(hasUnmarkedNodesFurther(subGraph, m1, 10)) {
            Vertex vertex;
            if(m1.isEmpty()) {
                vertex = unmarkedNodes.stream().findAny().get();
            }
            else {
                vertex = getRandomVertexFromDistance(subGraph, m1, unmarkedNodes, 10);
            }
            m1.add(vertex); //major monarch
            vertex.setMonarch();
            vertex.setMarked();
            getAdjacentVerticesUpToDistance(subGraph, vertex, 5).forEach(adjacentVertex -> {
                if(!adjacentVertex.isMarked()) {
                    vertex.addToEmpire(adjacentVertex);
                    adjacentVertex.setMarked();
                }
            });

            intersection(vertex.getEmpire(), getAdjacentVerticesAtDistance(subGraph, vertex, 5))
                    .forEach(u ->
                            getAdjacentVerticesAtDistance(subGraph, vertex, 5).forEach(w -> {
                                if (!w.isMarked() && !unmarkedNodes.contains(w)) {
                                    w.setParent(vertex);
                                    w.setDeputy(u);
                                    unmarkedNodes.add(w);
                                }
                            }));

        }
        m1.forEach(m -> {
            shuffleAndReduceToSize(getAdjacentVerticesAtDistance(subGraph, m, 1), maxFailedCenters).forEach(v -> {
                //TODO make them backup centers
            });
        });

        unmarkedNodes.clear();
        m1.forEach(m -> {
            intersection(m.getEmpire(), getAdjacentVerticesAtDistance(subGraph, m, 5))
                .forEach(u -> {
                    getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(neighbor -> {
                        if(!neighbor.isMarked() && neighbor.getParent() == null && !unmarkedNodes.contains(neighbor)) {
                            neighbor.setParent(m);
                            unmarkedNodes.add(neighbor);
                            neighbor.setDeputy(u);
                        }
                    });
                });
        });

        while (!unmarkedNodes.isEmpty()) {
            Vertex vertex = unmarkedNodes.stream().findAny().get();
            unmarkedNodes.remove(vertex);
            vertex.setMonarch(); //minor monarch
            vertex.setMarked();
            m2.add(vertex);
            //vertex.setParent(Parent(v))???
            getAdjacentVerticesUpToDistance(subGraph, vertex, 5).forEach(adjacentVertex -> {
                if (!adjacentVertex.isMarked()) {
                    adjacentVertex.setMarked();
                    vertex.addToEmpire(adjacentVertex);
                }
            });
            intersection(vertex.getEmpire(), getAdjacentVerticesAtDistance(subGraph, vertex, 5))
                    .forEach(u ->
                            getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(neighbor -> {
                                if(!neighbor.isMarked() && neighbor.getParent() == null && !unmarkedNodes.contains(neighbor)) {
                                    neighbor.setParent(vertex);
                                    unmarkedNodes.add(neighbor);
                                    neighbor.setDeputy(u);
                                }
                            }));
        }
        m.addAll(m1);
        m.addAll(m2);
    }

    private void conservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph) {
        //TODO almost the same as nonConservative
    }

    private void conservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {
        Map<Vertex, Set<Vertex>> unassigned = new HashMap<>();
        Map<Vertex, Set<Vertex>> passed = new HashMap<>();
        for(Vertex monarch : m) {
            Set<Vertex> temp = new HashSet<>();
            temp.add(monarch);
            temp.addAll(monarch.getEmpire());
            m.forEach(v -> temp.removeAll(v.getClients()));
            unassigned.put(monarch, temp);
        }


        Set<Vertex> monarchTree = new HashSet<>();
        monarchTree.addAll(m);
        monarchTree.forEach(m -> passed.put(m, new HashSet<>()));

        while(!monarchTree.isEmpty()) {
            Vertex m = getALeaf(monarchTree); //TODO remove a leaf node from T

            //for each node u at level-5 of m do:
                int passedNum = passed.get(m).size();
                int k = passedNum / maxClientsPerCenter;
                int e = passedNum % maxClientsPerCenter;
                //TODO

            int unassignedAndPassed = unassigned.get(m).size() + passed.get(m).size();
            k = unassignedAndPassed / maxClientsPerCenter;
            e = unassignedAndPassed % maxClientsPerCenter;
            //TODO
        }
    }

    private Graph<Vertex, DefaultWeightedEdge> getSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> vertices) {
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

    private void addEdgesUpToMaxWeightToSubGraph(Graph<Vertex, DefaultWeightedEdge> graph, Graph<Vertex, DefaultWeightedEdge> subGraph, double maxWeight) {
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
                .collect(toList());
    }

    private List<Integer> getRequiredCentersPerComponent(int maxClientsPerCenter, List<Integer> componentNodeCount) {
        return componentNodeCount
                .stream()
                .map(cnc ->
                        (int) Math.ceil((double) cnc / maxClientsPerCenter))
                .collect(toList());
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
        Set<Vertex> vertices = new HashSet<>(adjacentVertices);
        if (distance > 1) {
            adjacentVertices.forEach(adjacentVertex ->
                    vertices.addAll(getAdjacentVerticesUpToDistance(graph, adjacentVertex, distance - 1)));
        }
        return new ArrayList<>(vertices);
    }

    private List<Vertex> getAdjacentVerticesAtDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        List<Vertex> adjacentVertices = Graphs.neighborListOf(graph, source);
        Set<Vertex> vertices = new HashSet<>();
        if (distance == 0) {
            vertices.addAll(adjacentVertices);
        } else {
            adjacentVertices.forEach(vertex -> vertices.addAll(getAdjacentVerticesAtDistance(graph, vertex, distance - 1)));
        }
        return new ArrayList<>(vertices);
    }

    private List<Vertex> shuffleAndReduceToSize(List<Vertex> vertices, int size) {
        List<Vertex> list = new ArrayList<>(vertices);
        Collections.shuffle(list);
        if (size > list.size()) {
            return list;
        } else if (size < 0) {
            return new ArrayList<>();
        }
        return list.subList(0, size);
    }

    private int getEdgeCapacity(Graph<Vertex, WeightedEdgeWithCapacity> graph, WeightedEdgeWithCapacity edge) {
        return graph.getEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge)).getCapacity();
    }

    private int getVertexSupply(Vertex source, Vertex target, Vertex vertex) {
        if (vertex.equals(source)) {
            return 1;
        } else if (vertex.equals(target)) {
            return -1;
        } else {
            return 0;
        }
    }

    private Set<Vertex> getClients(Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph, Map<WeightedEdgeWithCapacity, Double> flowMap, Vertex monarch) {
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

    private boolean hasUnmarkedNodesFurther(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> fromSet, int distance) {
        return true; //TODO implement
    }

    private Vertex getRandomVertexFromDistance(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> distanceFrom, List<Vertex> fromList, int distance) {
        return new Vertex(0, 0, BLUE); //TODO implement
    }

    private Vertex getALeaf(Set<Vertex> tree) {
        return tree.stream().filter(x -> !tree.stream().anyMatch(y -> y.getParent() == x)).findAny().get();
    }
}
