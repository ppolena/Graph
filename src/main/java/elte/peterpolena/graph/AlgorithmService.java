package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.flow.mincost.CapacityScalingMinimumCostFlow;
import org.jgrapht.alg.flow.mincost.MinimumCostFlowProblem.MinimumCostFlowProblemImpl;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static elte.peterpolena.graph.Config.maxXCoordinate;
import static elte.peterpolena.graph.Config.maxYCoordinate;
import static elte.peterpolena.graph.Config.minXCoordinate;
import static elte.peterpolena.graph.Config.minYCoordinate;
import static java.awt.Color.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.intersection;
import static org.apache.commons.collections4.ListUtils.partition;

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

    @Autowired
    private Window window;
    private Set<Vertex> m1 = new HashSet<>();
    private Set<Vertex> m2 = new HashSet<>();
    private Set<Vertex> m = new HashSet<>();

    public boolean mainAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph,
                                 int maxCenters,
                                 int maxClientsPerCenter,
                                 int maxFailedCenters,
                                 boolean isConservative) {

        graph.vertexSet().forEach(vertex -> vertex.clearData());
        System.out.println("K: " + maxCenters);
        System.out.println("L: " + maxClientsPerCenter);

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

        System.out.println("Subgraphs: " + subGraphs.size());

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

        graph.vertexSet().forEach(vertex -> vertex.clearData());
        ConnectivityInspector<Vertex, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(graph);

        List<Set<Vertex>> connectedComponents = connectivityInspector.connectedSets();

        List<Integer> componentNodeCount = getComponentNodeCount(connectedComponents);

        List<Integer> requiredCentersPerComponent = getRequiredCentersPerComponent(maxClientsPerCenter, componentNodeCount);

        int requiredCenters = getRequiredCenters(requiredCentersPerComponent);

        System.out.println("Kw: " + requiredCenters);

        if (requiredCenters > maxCenters) {
            return false;
        }

        Set<Graph<Vertex, DefaultWeightedEdge>> subGraphs =
                connectedComponents
                        .stream()
                        .map(vertices -> getSubGraph(graph, vertices))
                        .collect(toSet());

        System.out.println("Subgraph connected components: " + subGraphs.size());

        if (isConservative) {
            subGraphs.forEach(x -> callConservativeAlgorithms(x, maxCenters, maxClientsPerCenter, maxFailedCenters));
        } else {
            subGraphs.forEach(x -> callNonConservativeAlgorithms(x, maxClientsPerCenter, maxFailedCenters));
        }

        long centers = graph.vertexSet().stream().filter(vertex -> vertex.getColor().equals(RED)).count();

        System.out.println("Allocated centers: " + centers);
        if (centers > maxCenters) {
            return false;
        }

        return true;
    }

    private void callNonConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                               int maxClientsPerCenter,
                                               int maxFailedCenters) {

        m1.clear();
        m2.clear();
        m.clear();
        nonConservativeSelectMonarchsAlgorithm(subGraph, maxFailedCenters);
        nonConservativeAssignDomainsAlgorithm(subGraph, maxClientsPerCenter);
        nonConservativeReAssignAlgorithm(subGraph, maxClientsPerCenter, maxFailedCenters);
        //nonConservativeReAssignByFailedAlgorithm(subGraph);
    }

    private void callConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                            int maxCenters,
                                            int maxClientsPerCenter,
                                            int maxFailedCenters) {

        m1.clear();
        m2.clear();
        m.clear();
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
            Utils.getAdjacentVerticesUpToDistance(subGraph, vertex, 2).forEach(adjacentVertex -> {
                if (!adjacentVertex.isMarked()) {
                    adjacentVertex.setMarked();
                    vertex.addToEmpire(adjacentVertex);
                }
            });
            intersection(vertex.getEmpire(), Utils.getAdjacentVerticesAtDistance(subGraph, vertex, 2))
                    .forEach(u ->
                            Utils.getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(w -> {
                                if (!w.isMarked() && !unmarkedNodes.contains(w)) {
                                    w.setParent(vertex);
                                    w.setDeputy(u);
                                    unmarkedNodes.add(w);
                                }
                            }));
        }

        System.out.println("M1 size: " + m1.size());

        m1.forEach(major -> {
            List<Vertex> minors = Utils.shuffleAndReduceToSize(
                    Utils.getAdjacentVerticesAtDistance(subGraph, major, 1)
                            .stream()
                            .filter(vertex -> !vertex.equals(major.getDeputy()))
                            .collect(toList()),
                    maxFailedCenters - 1);

            major.addMinors(minors);
            m2.addAll(minors);
            minors.forEach(minor -> minor.setMajor(major));
            major.setMajor(major);
        });

        System.out.println("M2 size: " + m2.size());

        // M = M1 UNION M2
        m.addAll(m1);
        m.addAll(m2);
    }

    private void nonConservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {

        MinCostMaxFlowService minCost = new MinCostMaxFlowService();
        Map<Vertex, Set<Vertex>> flow = minCost
                .getFlow(subGraph, m, maxClientsPerCenter);
        flow.forEach((from, to) -> {
            from.setColor(RED);
            from.setClients(to);
            from.getClients().forEach(client -> client.setCenter(from));
        });

        //for displaying purposes, the graph we constructed was:
        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        subGraph.vertexSet().forEach(bipartiteGraph::addVertex);
        //copy monarch set into bipartite graph
        //Set<Vertex> monarchs = new HashSet<>();
        Map<Vertex, Vertex> monarchCopies = new HashMap<>();
        m.forEach(x -> {
            int diffPlacement = -50;
            if(x.getX() > (maxXCoordinate - minXCoordinate))
                diffPlacement += 50;
            monarchCopies.put(x, new Vertex(x.getX() + diffPlacement , x.getY(), GREEN));
        });
        monarchCopies.values().forEach(bipartiteGraph::addVertex);
        //E'
        monarchCopies.forEach((original, monarch) -> Utils.getAdjacentVerticesUpToDistance(subGraph, original.getMajor(), 2)
                .forEach(adjacentVertex -> {
                    //if (!monarch.equals(adjacentVertex)) {
                    bipartiteGraph.addEdge(monarch, adjacentVertex);
                    //}
                }));

        //add s and t
        Vertex source = new Vertex(minXCoordinate + 10, minYCoordinate + 10, BLUE);
        Vertex target = new Vertex(maxXCoordinate - 10, maxYCoordinate - 10, BLUE);
        bipartiteGraph.addVertex(source);
        bipartiteGraph.addVertex(target);

        //for m ∈ M add edge (s, m) and set (s, m) capacity to L
        monarchCopies.values().forEach(monarch -> {
            bipartiteGraph.addEdge(source, monarch);
            bipartiteGraph.getEdge(source, monarch).setCapacity(maxClientsPerCenter);
        });

        //for v ∈ V add edge (v, t) and set (s, m) capacity to 1
        subGraph.vertexSet().forEach(vertex -> {
            bipartiteGraph.addEdge(vertex, target);
            bipartiteGraph.getEdge(vertex, target).setCapacity(1);
        });

        //for m ∈ M and v ∈ V set (m, v) capacity to 1 and if m = v set (m,v) weight to 0
        monarchCopies.forEach((orig, monarch) -> subGraph.vertexSet()
                .forEach(vertex -> {
                    if (bipartiteGraph.getEdge(monarch, vertex) != null) {
                        bipartiteGraph.getEdge(monarch, vertex).setCapacity(1);
                        if (orig.equals(vertex)) {
                            bipartiteGraph.setEdgeWeight(monarch, vertex, 0);
                        }
                    }
                }));

        //Construct directed bipartite graph
        /*Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        subGraph.vertexSet().forEach(bipartiteGraph::addVertex);
        //copy monarch set into bipartite graph
        //Set<Vertex> monarchs = new HashSet<>();
        Map<Vertex, Vertex> monarchCopies = new HashMap<>();
        m.forEach(x -> monarchCopies.put(x, new Vertex(x.getX(), x.getY(), GREEN)));
        monarchCopies.values().forEach(bipartiteGraph::addVertex);
        //E'
        monarchCopies.forEach((original, monarch) -> getAdjacentVerticesUpToDistance(subGraph, original.getMajor(), 2)
                .forEach(adjacentVertex -> {
                    //if (!monarch.equals(adjacentVertex)) {
                        bipartiteGraph.addEdge(monarch, adjacentVertex);
                    //}
                }));

        //add s and t
        Vertex source = new Vertex(minXCoordinate + 10, minYCoordinate + 10, BLUE);
        Vertex target = new Vertex(maxXCoordinate - 10, maxYCoordinate - 10, BLUE);
        bipartiteGraph.addVertex(source);
        bipartiteGraph.addVertex(target);

        //for m ∈ M add edge (s, m) and set (s, m) capacity to L
        monarchCopies.values().forEach(monarch -> {
            bipartiteGraph.addEdge(source, monarch);
            bipartiteGraph.getEdge(source, monarch).setCapacity(maxClientsPerCenter);
        });

        //for v ∈ V add edge (v, t) and set (s, m) capacity to 1
        subGraph.vertexSet().forEach(vertex -> {
            bipartiteGraph.addEdge(vertex, target);
            bipartiteGraph.getEdge(vertex, target).setCapacity(1);
        });

        //for m ∈ M and v ∈ V set (m, v) capacity to 1 and if m = v set (m,v) weight to 0
        monarchCopies.forEach((orig, monarch) -> subGraph.vertexSet()
                .forEach(vertex -> {
                    if (bipartiteGraph.getEdge(monarch, vertex) != null) {
                        bipartiteGraph.getEdge(monarch, vertex).setCapacity(1);
                        if (orig.equals(vertex)) {
                            bipartiteGraph.setEdgeWeight(monarch, vertex, 0);
                        }
                    }
                }));

        //calculate minimum cost maximum flow
        Map<WeightedEdgeWithCapacity, Double> flowMap = new CapacityScalingMinimumCostFlow<Vertex, WeightedEdgeWithCapacity>()
                .getMinimumCostFlow(new MinimumCostFlowProblemImpl<>(
                        bipartiteGraph,
                        vertex -> getVertexSupply(source, target, vertex),
                        edge -> getEdgeCapacity(bipartiteGraph, edge), //max directed edge capacity
                        edge -> 0)) //min directed edge capacity
                .getFlowMap();

        //for m ∈ M add v to dom(m) if v receives one unit of flow from m
        monarchCopies.forEach((orig, monarch) -> {
            orig.setColor(RED);
            orig.setClients(getClients(bipartiteGraph, flowMap, monarch));
            orig.getClients().forEach(client -> client.setCenter(orig));
            System.out.println("monarch clients: " + orig.getClients().size());
        });*/
    }

    private void nonConservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter, int maxFailedCenters) {
/*
unassigned(m) => foreach m ∈ m1: foreach v ∈ m.getEmpire(): v.getCenter() == null
free node => node.getColor().equals(BLACK)
 */

        Map<Vertex, Set<Vertex>> unassigned = new HashMap<>();
        Map<Vertex, Set<Vertex>> passed = new HashMap<>();

        m1.forEach(major ->
                unassigned.put(
                        major,
                        major.getEmpire()
                                .stream()
                                .filter(client -> client.getCenter() == null)
                                .collect(toSet())));

        Set<Vertex> monarchTree = new HashSet<>(m1);
        monarchTree.forEach(major -> passed.put(major, new HashSet<>()));

        while(!monarchTree.isEmpty()) {
            Vertex m = Utils.getALeaf(monarchTree);

            int unassignedAndPassed = unassigned.get(m).size() + passed.get(m).size();
            int k = unassignedAndPassed / maxClientsPerCenter;
            int e = unassignedAndPassed % maxClientsPerCenter;
            System.out.println("unassignedAndPassed: " + unassignedAndPassed + " - k: " + k + " -  e: " + e);

            //center => RED
            //client => BLACK

            //select k' centers from m.getEmpire()
            List<Vertex> centers = Utils.shuffleAndReduceToSize(m.getEmpire().stream().filter(x -> x.getColor() != RED)
                    .collect(toList()), k);
            centers.forEach(center -> {
                center.setColor(RED);
                //center.setCenter(center); not sure about this
            });

            //select k'L free nodes from unassigned(m) + passed(m)
            List<Vertex> unassignedAndPassedVertices = new ArrayList<>(unassigned.get(m));
            unassignedAndPassedVertices.addAll(passed.get(m));
            List<Vertex> freeNodes = getFreeNodes(m.getEmpire());
            System.out.println("Free nodes: " + freeNodes);
            List<Vertex> nodesToAssignToCenters = Utils.shuffleAndReduceToSize(freeNodes, k * maxClientsPerCenter);

            //select e free nodes
            freeNodes.removeAll(nodesToAssignToCenters);
            List<Vertex> nodesToAssignToM = Utils.shuffleAndReduceToSize(freeNodes, e);

            //create L sized sublist from k'L nodes
            List<List<Vertex>> partitionedNodesToAssignToCenters = partition(nodesToAssignToCenters, maxClientsPerCenter);

            //assign k'L free nodes to k' centers
            for (int i = 0; i < partitionedNodesToAssignToCenters.size(); ++i) {
                Vertex center = centers.get(i);
                Set<Vertex> clientsForCenter = new HashSet<>(partitionedNodesToAssignToCenters.get(i));
                center.addClients(clientsForCenter);
                clientsForCenter.forEach(client -> client.setCenter(center));
            }

            //release e nodes from dom(m) and assign e free nodes to m
            List<Vertex> releasedClients = Utils.shuffleAndReduceToSize(new ArrayList<>(m.getClients()), e);
            m.getClients().removeAll(releasedClients);
            releasedClients.forEach(client -> client.setCenter(null));
            m.addClients(new HashSet<>(nodesToAssignToM));

            //add releasedClients to passed(Parent(m)) if m.getParent() != null
            //else
            //create a new center from freeNodes and assign releasedClients to it
            if (m.getParent() != null) {
                passed.get(m.getParent()).addAll(releasedClients);
            } else {
                if(!releasedClients.isEmpty()) {
                    freeNodes = getFreeNodes(m.getEmpire());
                    Vertex center = freeNodes.stream().findAny().get();
                    center.setColor(RED);
                    center.addClients(new HashSet<>(releasedClients));
                    center.getClients().forEach(client -> client.setCenter(center));
                }
            }

            monarchTree.remove(m);
        }

        //M' = all centers allocated so far
        long centers = subGraph.vertexSet().stream().filter(vertex -> vertex.getColor().equals(RED)).count();

        //ceil(n/L) + α
        long requiredCenters = (long) (Math.ceil(subGraph.vertexSet().size() / maxClientsPerCenter) + maxFailedCenters);

        System.out.println("Centers before end: " + centers + " - Required: " + requiredCenters);
        //if |M'| < ceil(n/L) + α
        if (centers < requiredCenters) {
            int centersNeeded = (int) (requiredCenters - centers);
            List<Vertex> freeNodes = getFreeNodes(new ArrayList<>(subGraph.vertexSet()));
            Utils.shuffleAndReduceToSize(freeNodes, centersNeeded).forEach(center -> center.setColor(RED));
        }
    }

    private void nonConservativeReAssignByFailedAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, Set<Vertex> failedCenters, int maxClientsPerCenter) {
        //TODO we need that subgraph when the original algorithm was successful
        Map<Pair<Vertex, Vertex>, Vertex> X = new HashMap<>();

        failedCenters.forEach(failed -> {
            m1.forEach(monarch -> {
                Set<Vertex> team = new HashSet<>(monarch.getMinors());
                team.add(monarch);
                if(!team.contains(failed)) {
                    //select a different non-faulty center r from team(m)
                    Vertex r = team.stream().filter(x -> !failedCenters.contains(x)).findAny().get();
                    X.put(new Pair<>(failed, monarch), r);
                }
            });
        });

        //for each node v that was served by some f
        failedCenters.forEach(failed -> {
            failed.getClients().forEach(client -> {
                //unique free place
                Vertex freeV = subGraph.vertexSet().stream().filter(x -> !failedCenters.contains(x)
                    && x.getColor() == RED && x.getClients().size() < maxClientsPerCenter).findAny().get();
                //also add client to freev?

                //MP = (m1, ... mj) path in T tree from failed to freeV's major
                List<Vertex> MP = getTreePathTo(failed.getMajor(), freeV.getMajor());

                Vertex releasedNode = client;
                for(int i = 1; i < MP.size(); i++) {
                    Vertex currentNode = X.get(new Pair<>(failed, MP.get(i)));
                    X.put(new Pair<>(failed, MP.get(i)), releasedNode);
                    releasedNode = currentNode;
                }

                if(releasedNode != null) {
                    freeV.getClients().add(releasedNode);
                    releasedNode.setCenter(freeV);
                }

            });
        });
    }

    private void conservativeSelectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxFailedCenters) {
        List<Vertex> unmarkedNodes = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>(subGraph.vertexSet());
        unmarkedNodes.add(vertices.stream().findAny().get());

        while(Utils.hasUnmarkedNodesFurther(subGraph, m1, 10)) {
            Vertex vertex;
            if(m1.isEmpty()) {
                vertex = unmarkedNodes.stream().findAny().get();
            }
            else {
                vertex = Utils.getRandomVertexFromDistance(subGraph, m1, unmarkedNodes, 10);
            }
            m1.add(vertex); //major monarch
            vertex.setMonarch();
            vertex.setMarked();
            Utils.getAdjacentVerticesUpToDistance(subGraph, vertex, 5).forEach(adjacentVertex -> {
                if(!adjacentVertex.isMarked()) {
                    vertex.addToEmpire(adjacentVertex);
                    adjacentVertex.setMarked();
                }
            });

            intersection(vertex.getEmpire(), Utils.getAdjacentVerticesAtDistance(subGraph, vertex, 5))
                    .forEach(u ->
                            Utils.getAdjacentVerticesAtDistance(subGraph, vertex, 5).forEach(w -> {
                                if (!w.isMarked() && !unmarkedNodes.contains(w)) {
                                    w.setParent(vertex);
                                    w.setDeputy(u);
                                    unmarkedNodes.add(w);
                                }
                            }));

        }
        m1.forEach(m -> {
            Utils.shuffleAndReduceToSize(Utils.getAdjacentVerticesAtDistance(subGraph, m, 1), maxFailedCenters).forEach(v -> {
                //TODO make them backup centers
            });
        });

        unmarkedNodes.clear();
        m1.forEach(m -> {
            intersection(m.getEmpire(), Utils.getAdjacentVerticesAtDistance(subGraph, m, 5))
                .forEach(u -> {
                    Utils.getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(neighbor -> {
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
            Utils.getAdjacentVerticesUpToDistance(subGraph, vertex, 5).forEach(adjacentVertex -> {
                if (!adjacentVertex.isMarked()) {
                    adjacentVertex.setMarked();
                    vertex.addToEmpire(adjacentVertex);
                }
            });
            intersection(vertex.getEmpire(), Utils.getAdjacentVerticesAtDistance(subGraph, vertex, 5))
                    .forEach(u ->
                            Utils.getAdjacentVerticesAtDistance(subGraph, u, 1).forEach(neighbor -> {
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
            Vertex m = Utils.getALeaf(monarchTree); //TODO remove a leaf node from T

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

    private List<Vertex> getFreeNodes(List<Vertex> vertices) {
        return vertices
                .stream()
                .filter(node -> node.getColor().equals(BLACK))
                .collect(toList());
    }

    private List<Vertex> getTreePathTo(Vertex from, Vertex to) {
        List<Vertex> ret = new ArrayList<>();
        if(from == to) {
            ret.add(from);
            return ret;
        }

        List<Vertex> fromPath = new ArrayList<>();
        Vertex iter = from;
        fromPath.add(iter);
        while(iter.getParent() != null) {
            fromPath.add(iter);
            iter = iter.getParent();
        }
        Collections.reverse(fromPath);

        List<Vertex> toPath = new ArrayList<>();
        iter = from;
        toPath.add(iter);
        while(iter.getParent() != null) {
            toPath.add(iter);
            iter = iter.getParent();
        }
        Collections.reverse(toPath);

        while(!fromPath.isEmpty() && !toPath.isEmpty() &&fromPath.get(0) == toPath.get(0)) {
            fromPath.remove(0);
            toPath.remove(0);
        }
        Collections.reverse(fromPath);
        ret.addAll(fromPath);
        if(!toPath.isEmpty() && toPath.get(0).getParent() != null)
            ret.add(toPath.get(0).getParent());
        ret.addAll(toPath);

        return ret;
    }
}
