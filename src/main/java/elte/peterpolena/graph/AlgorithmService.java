package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

import static elte.peterpolena.graph.Config.*;
import static elte.peterpolena.graph.Utils.*;
import static java.awt.Color.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.intersection;
import static org.apache.commons.collections4.ListUtils.partition;

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
    private Result result = new Result();

    public Result mainAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph,
                                 int maxCenters,
                                 int maxClientsPerCenter,
                                 int maxFailedCenters,
                                 boolean isConservative) {

        System.out.println("\nSTART MAIN ALGORITHM\n");

        graph.vertexSet().forEach(Vertex::clearData);
        System.out.println("\tK: " + maxCenters);
        System.out.println("\tL: " + maxClientsPerCenter);

        result.setOriginalGraph(graph);

        List<Double> weights = graph
                .edgeSet()
                .stream()
                .map(graph::getEdgeWeight)
                .collect(toSet())
                .stream()
                .sorted()
                .collect(toList());

        System.out.println("\tWeights: " + weights.size());

        List<Graph<Vertex, DefaultWeightedEdge>> subGraphs = new ArrayList<>();
        weights.forEach(weight -> {
            Graph<Vertex, DefaultWeightedEdge> subGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
            graph.vertexSet().forEach(subGraph::addVertex);
            addEdgesUpToMaxWeightToSubGraph(graph, subGraph, weight);
            subGraphs.add(subGraph);
        });

        System.out.println("\tSubGraphs: " + subGraphs.size());

        for (Graph<Vertex, DefaultWeightedEdge> subGraph : subGraphs) {
            result.addGraphToDraw("[MAIN] Subgraph", subGraph);
            if (assignCentersAlgorithm(subGraph, maxCenters, maxClientsPerCenter, maxFailedCenters, isConservative)) {
                result.addGraphToDraw("[MAIN] Result", graph);
                System.out.println("\nEND MAIN ALGORITHM\n");
                return result;
            }
        }

        System.out.println("\nEND MAIN ALGORITHM\n");
        return null;
    }

	private boolean assignCentersAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph,
										   int maxCenters,
										   int maxClientsPerCenter,
										   int maxFailedCenters,
										   boolean isConservative) {

        System.out.println("\nSTART ASSIGN CENTERS ALGORITHM\n");

		subGraph.vertexSet().forEach(Vertex::clearData);
		ConnectivityInspector<Vertex, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(subGraph);

        List<Set<Vertex>> connectedComponents = connectivityInspector.connectedSets();

        List<Integer> componentNodeCount = getComponentNodeCount(connectedComponents);

        List<Integer> requiredCentersPerComponent = getRequiredCentersPerComponent(maxClientsPerCenter, componentNodeCount);

        int requiredCenters = getRequiredCenters(requiredCentersPerComponent);

        System.out.println("\tKw: " + requiredCenters);
		System.out.println("\tKw > K: " + (requiredCenters > maxCenters));
        if (requiredCenters > maxCenters) {
            return false;
        }

		Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponentSet =
                connectedComponents
                        .stream()
						.map(vertices -> getSubGraph(subGraph, vertices))
                        .collect(toSet());

		System.out.println("\tSubGraph connected components: " + connectedComponentSet.size());

        if (isConservative) {
            connectedComponentSet.forEach(cc -> {
                result.addGraphToDraw("[ASSIGN CENTERS] Connected Component", cc);
                callConservativeAlgorithms(cc, maxCenters, maxClientsPerCenter, maxFailedCenters);
            });
        } else {
            connectedComponentSet.forEach(cc -> {
                result.addGraphToDraw("[ASSIGN CENTERS] Connected Component", cc);
                callNonConservativeAlgorithms(cc, maxClientsPerCenter, maxFailedCenters);
            });
        }

		long centers = getCentersCount(subGraph);

        boolean centersBelowOrEqualToMaxCenters = centers <= maxCenters;

        System.out.println("\tAllocated centers: " + centers);
        System.out.println("\tMax centers: " + maxCenters);
        System.out.printf("\tAllocated centers <= Max centers: " + centersBelowOrEqualToMaxCenters);
        System.out.println("\n\nEND ASSIGN CENTERS ALGORITHM\n");
        return centersBelowOrEqualToMaxCenters;
    }

	private void callNonConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> connectedComponent,
											   int maxClientsPerCenter,
											   int maxFailedCenters) {

		System.out.println("\n---ITERATION START---\n");
        m1.clear();
        m2.clear();
        m.clear();
		nonConservativeSelectMonarchsAlgorithm(connectedComponent, maxFailedCenters);
		nonConservativeAssignDomainsAlgorithm(connectedComponent, maxClientsPerCenter);
		nonConservativeReAssignAlgorithm(connectedComponent, maxClientsPerCenter, maxFailedCenters);
        //nonConservativeReAssignByFailedAlgorithm(subGraph);
		System.out.println("\tNumber of centers in connected component at the end of iteration: " + getCentersCount(connectedComponent));
		System.out.println("\n---ITERATION END---\n");
    }

    private void callConservativeAlgorithms(Graph<Vertex, DefaultWeightedEdge> subGraph,
                                            int maxCenters,
                                            int maxClientsPerCenter,
                                            int maxFailedCenters) {

        m1.clear();
        m2.clear();
        m.clear();
        conservativeSelectMonarchsAlgorithm(subGraph, maxFailedCenters);
        conservativeAssignDomainsAlgorithm(subGraph, maxClientsPerCenter);
        conservativeReAssignAlgorithm(subGraph, maxClientsPerCenter);
    }

    private void nonConservativeSelectMonarchsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxFailedCenters) {

        System.out.println("\nSTART SELECT MONARCHS ALGORITHM\n");

        List<Vertex> unmarkedNodes = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>(subGraph.vertexSet());
        unmarkedNodes.add(vertices.stream().findAny().get());
        while (!unmarkedNodes.isEmpty()) {
            Vertex vertex = unmarkedNodes.stream().findAny().get();
            unmarkedNodes.remove(vertex);
            vertex.setMonarch();
            vertex.setMarked();
            m1.add(vertex);
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

        System.out.println("\tM1 size: " + m1.size());

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

        System.out.println("\tM2 size: " + m2.size());

        // M = M1 UNION M2
        m.addAll(m1);
        m.addAll(m2);

        result.addGraphWithMonarchsToDraw("[SELECT MONARCHS] Connected Component", subGraph, m2, m1);

        System.out.println("\tM size: " + m.size());
        System.out.println("\nEND SELECT MONARCHS ALGORITHM\n");
    }

    private void nonConservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {

        System.out.println("\nSTART ASSIGN DOMAINS ALGORITHM\n");
        System.out.println("\tConstructing bipartite graph...");

        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);

        Map<Vertex, Vertex> vertexCopies = new HashMap<>();
        subGraph.vertexSet().forEach(x -> {
            vertexCopies.put(x, new Vertex((x.getX() - minXCoordinate) / 2 + minXCoordinate + (maxXCoordinate - minXCoordinate) / 2, (x.getY() - (maxYCoordinate - minYCoordinate) / 2) / 2 + (maxYCoordinate - minYCoordinate) / 2, BLACK));
        });
        vertexCopies.values().forEach(bipartiteGraph::addVertex);
        //copy monarch set into bipartite graph
        Map<Vertex, Vertex> monarchCopies = new HashMap<>();
        m.forEach(x -> {
            /*int diffPlacement = -50 - vertexRadius;
            if (x.getX() > (maxXCoordinate - minXCoordinate) / 2) {
                diffPlacement += 100 + vertexRadius;
            }
            monarchCopies.put(x, new Vertex(x.getX() + diffPlacement , x.getY(), GREEN));*/
            monarchCopies.put(x, new Vertex((x.getX() - minXCoordinate) / 2 + minXCoordinate, (x.getY() - (maxYCoordinate - minYCoordinate) / 2) / 2 + (maxYCoordinate - minYCoordinate) / 2, GREEN));
        });
        monarchCopies.values().forEach(bipartiteGraph::addVertex);
        //E'
        monarchCopies.forEach((original, monarch) -> getAdjacentVerticesUpToDistance(subGraph, original.getMajor(), 2)
                .forEach(adjacentVertex -> bipartiteGraph.addEdge(monarch, vertexCopies.get(adjacentVertex))));

        //add s and t
        Vertex source = new Vertex(minXCoordinate + 10, minYCoordinate + 10, CYAN);
        Vertex target = new Vertex(maxXCoordinate - 10, maxYCoordinate - 10, BLUE);
        bipartiteGraph.addVertex(source);
        bipartiteGraph.addVertex(target);

        //for m ∈ M add edge (s, m) and set (s, m) capacity to L
        monarchCopies.values().forEach(monarch -> {
            bipartiteGraph.addEdge(source, monarch);
            bipartiteGraph.getEdge(source, monarch).setCapacity(maxClientsPerCenter);
        });

        //for v ∈ V add edge (v, t) and set (s, m) capacity to 1
        vertexCopies.values().forEach(vertex -> {
            bipartiteGraph.addEdge(vertex, target);
            bipartiteGraph.getEdge(vertex, target).setCapacity(1);
        });

        //for m ∈ M and v ∈ V set (m, v) capacity to 1 and if m = v set (m,v) weight to 0
        monarchCopies.forEach((original, monarch) -> vertexCopies
                .forEach((origV, vertex) -> {
                    if (bipartiteGraph.getEdge(monarch, vertex) != null) {
                        bipartiteGraph.getEdge(monarch, vertex).setCapacity(1);
                        if (original.equals(origV)) {
                            bipartiteGraph.setEdgeWeight(monarch, vertex, 0);
                        }
                    }
                }));

        result.addBipartiteGraphToDraw("[ASSIGN DOMAINS] Bipartite Graph", bipartiteGraph);

        System.out.println("\tCalculating Minimum Cost Maximum Flow...");
        //Calculating minCostMaxFlow
        MinCostMaxFlowService minCost = new MinCostMaxFlowService();
        Map<Vertex, Set<Vertex>> flow = minCost
                .getFlow(subGraph, m, maxClientsPerCenter, true);
        flow.forEach((from, to) -> {
            from.setColor(RED);
            from.setClients(to);
            from.getClients().forEach(client -> client.setCenter(from));
        });

        System.out.println("\nEND ASSIGN DOMAINS ALGORITHM\n");
    }

    private void nonConservativeReAssignAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter, int maxFailedCenters) {
/*
unassigned(m) => foreach m ∈ m1: foreach v ∈ m.getEmpire(): v.getCenter() == null
free node => node.getColor().equals(BLACK)
 */

        System.out.println("\nSTART REASSIGN ALGORITHM\n");

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
            Vertex m = getALeaf(monarchTree);

            int unassignedAndPassed = unassigned.get(m).size() + passed.get(m).size();
            int k = unassignedAndPassed / maxClientsPerCenter;
            int e = unassignedAndPassed % maxClientsPerCenter;
            System.out.println("\tUnassigned + passed nodes: " + unassignedAndPassed);
            System.out.println("\tk': " + k);
            System.out.println("\te: " + e);

            //center => RED
            //client => BLACK

            //select k' centers from m.getEmpire()
            List<Vertex> centers = shuffleAndReduceToSize(getFreeNodes(m.getEmpire()), k);
            centers.forEach(center -> center.setColor(RED));

            //select k'L free nodes from unassigned(m) + passed(m)
            List<Vertex> unassignedAndPassedVertices = new ArrayList<>(unassigned.get(m));
            unassignedAndPassedVertices.addAll(passed.get(m));
            List<Vertex> nodesToAssignToCenters = shuffleAndReduceToSize(unassignedAndPassedVertices, k * maxClientsPerCenter);

            //select e free nodes
            unassignedAndPassedVertices.removeAll(nodesToAssignToCenters);
            List<Vertex> nodesToAssignToM = shuffleAndReduceToSize(unassignedAndPassedVertices, e);

            //create L sized sublist from k'L nodes
            List<List<Vertex>> partitionedNodesToAssignToCenters = partition(nodesToAssignToCenters, maxClientsPerCenter);

            //assign k'L free nodes to k' centers
            for (int i = 0; i < partitionedNodesToAssignToCenters.size(); ++i) {
                Vertex center = centers.get(i);
                Set<Vertex> clientsForCenter = new HashSet<>(partitionedNodesToAssignToCenters.get(i));
                center.addClients(clientsForCenter);
                clientsForCenter.forEach(client -> client.setCenter(center));
            }

            //add e nodes to dom(m) so that dom(m) size is at most L, and add the remaining nodes to releasedNodes
            int numberOfAssignableNodesToM = maxClientsPerCenter - m.getClients().size();
            int numberOfNodesToRelease = nodesToAssignToM.size() - numberOfAssignableNodesToM;
            List<Vertex> releasedClients = shuffleAndReduceToSize(nodesToAssignToM, numberOfNodesToRelease);
            nodesToAssignToM.removeAll(releasedClients);
            m.addClients(new HashSet<>(nodesToAssignToM));
            m.getClients().forEach(client -> client.setCenter(m));

            //add releasedClients to passed(Parent(m)) if m.getParent() != null
            //else
            //create a new center from freeNodes and assign releasedClients to it
            if (m.getParent() != null) {
                passed.get(m.getParent()).addAll(releasedClients);
            } else {
                if(!releasedClients.isEmpty()) {
                    Vertex center = getFreeNodes(m.getEmpire()).stream().findAny().get();
                    center.setColor(RED);
                    center.addClients(new HashSet<>(releasedClients));
                    center.getClients().forEach(client -> client.setCenter(center));
                }
            }

            monarchTree.remove(m);
        }

        //M' = all centers allocated so far
		long centers = getCentersCount(subGraph);

        //ceil(n/L) + α
        long requiredCenters = (long) (Math.ceil(subGraph.vertexSet().size() / maxClientsPerCenter) + maxFailedCenters);

        System.out.println("\tCenters before end: " + centers);
        System.out.println("\tRequired: " + requiredCenters);
        //if |M'| < ceil(n/L) + α
        if (centers < requiredCenters) {
            int centersNeeded = (int) (requiredCenters - centers);
            List<Vertex> freeNodes = getFreeNodes(new ArrayList<>(subGraph.vertexSet()));
            shuffleAndReduceToSize(freeNodes, centersNeeded).forEach(center -> center.setColor(RED));
			long centersAfterRandomAssign = getCentersCount(subGraph);
			System.out.println("\tNumber of free nodes to choose from: " + freeNodes.size());
			System.out.println("\tCenters after random assign: " + centersAfterRandomAssign);
        }

        result.addGraphToDraw("[RE-ASSIGN DOMAINS] Connected Component", subGraph);

        System.out.println("\nEND REASSIGN ALGORITHM\n");
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

        while (hasUnmarkedNodesFurther(subGraph, m1, 10)) {
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

    private void conservativeAssignDomainsAlgorithm(Graph<Vertex, DefaultWeightedEdge> subGraph, int maxClientsPerCenter) {
        System.out.println("\nSTART ASSIGN DOMAINS ALGORITHM\n");
        System.out.println("\tConstructing bipartite graph...");

        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraph = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        subGraph.vertexSet().forEach(bipartiteGraph::addVertex);
        //copy monarch set into bipartite graph
        Map<Vertex, Vertex> monarchCopies = new HashMap<>();
        m.forEach(x -> {
            int diffPlacement = -50 - vertexRadius;
            if (x.getX() > (maxXCoordinate - minXCoordinate) / 2) {
                diffPlacement += 100 + vertexRadius;
            }
            monarchCopies.put(x, new Vertex(x.getX() + diffPlacement, x.getY(), GREEN));
        });
        monarchCopies.values().forEach(bipartiteGraph::addVertex);
        //E'
        monarchCopies.forEach((original, monarch) -> getAdjacentVerticesUpToDistance(subGraph, original, 2)
                .forEach(adjacentVertex -> bipartiteGraph.addEdge(monarch, adjacentVertex)));

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
        monarchCopies.forEach((original, monarch) -> subGraph.vertexSet()
                .forEach(vertex -> {
                    if (bipartiteGraph.getEdge(monarch, vertex) != null) {
                        bipartiteGraph.getEdge(monarch, vertex).setCapacity(1);
                        if (original.equals(vertex)) {
                            bipartiteGraph.setEdgeWeight(monarch, vertex, 0);
                        }
                    }
                }));

		result.addBipartiteGraphToDraw("[ASSIGN DOMAINS] Bipartite Graph", bipartiteGraph);

        System.out.println("\tCalculating Minimum Cost Maximum Flow...");
        //Calculating minCostMaxFlow
        MinCostMaxFlowService minCost = new MinCostMaxFlowService();
        Map<Vertex, Set<Vertex>> flow = minCost
                .getFlow(subGraph, m, maxClientsPerCenter, false);
        flow.forEach((from, to) -> {
            from.setColor(RED);
            from.setClients(to);
            from.getClients().forEach(client -> client.setCenter(from));
        });

        System.out.println("\nEND ASSIGN DOMAINS ALGORITHM\n");
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
            Vertex mon = getALeaf(monarchTree);

            int levelOfM = Utils.levelOfNode(mon);
            Utils.nodesAtLevel(monarchTree, levelOfM - 5).forEach(u -> {
                int passedNum = passed.get(u).size();
                int k = passedNum / maxClientsPerCenter;
                int e = passedNum % maxClientsPerCenter;

                List<Vertex> passedVertices = new ArrayList<>(passed.get(u));
                if (k > 0) {
                    List<Vertex> nodesToAssignToCenters = shuffleAndReduceToSize(passedVertices, k * maxClientsPerCenter);

                    passedVertices.removeAll(nodesToAssignToCenters);
                    List<Vertex> centers = shuffleAndReduceToSize(getFreeNodes(subGraph.vertexSet().stream().filter(x -> x.getDeputy() == u).collect(toList())), k);
                    centers.forEach(center -> center.setColor(RED));

                    //create L sized sublist from k'L nodes
                    List<List<Vertex>> partitionedNodesToAssignToCenters = partition(nodesToAssignToCenters, maxClientsPerCenter);

                    //assign k'L free nodes to k' centers
                    for (int i = 0; i < partitionedNodesToAssignToCenters.size(); ++i) {
                        Vertex center = centers.get(i);
                        Set<Vertex> clientsForCenter = new HashSet<>(partitionedNodesToAssignToCenters.get(i));
                        center.addClients(clientsForCenter);
                        clientsForCenter.forEach(client -> client.setCenter(center));
                    }

                }
                //add the remaining e
                passed.get(mon).addAll(passedVertices);
            });


            int unassignedAndPassed = unassigned.get(mon).size() + passed.get(mon).size();
            int k = unassignedAndPassed / maxClientsPerCenter;
            int e = unassignedAndPassed % maxClientsPerCenter;
            System.out.println("\tUnassigned + passed nodes: " + unassignedAndPassed);
            System.out.println("\tk': " + k);
            System.out.println("\te: " + e);

            //center => RED
            //client => BLACK

            //select k' centers from m.getEmpire()
            List<Vertex> centers = shuffleAndReduceToSize(getFreeNodes(mon.getEmpire()), k);
            centers.forEach(center -> center.setColor(RED));

            //select k'L free nodes from unassigned(m) + passed(m)
            List<Vertex> unassignedAndPassedVertices = new ArrayList<>(unassigned.get(mon));
            unassignedAndPassedVertices.addAll(passed.get(mon));
            List<Vertex> nodesToAssignToCenters = shuffleAndReduceToSize(unassignedAndPassedVertices, k * maxClientsPerCenter);

            //select e free nodes
            unassignedAndPassedVertices.removeAll(nodesToAssignToCenters);
            List<Vertex> nodesToAssignToM = shuffleAndReduceToSize(unassignedAndPassedVertices, e);

            //create L sized sublist from k'L nodes
            List<List<Vertex>> partitionedNodesToAssignToCenters = partition(nodesToAssignToCenters, maxClientsPerCenter);

            //assign k'L free nodes to k' centers
            for (int i = 0; i < partitionedNodesToAssignToCenters.size(); ++i) {
                Vertex center = centers.get(i);
                Set<Vertex> clientsForCenter = new HashSet<>(partitionedNodesToAssignToCenters.get(i));
                center.addClients(clientsForCenter);
                clientsForCenter.forEach(client -> client.setCenter(center));
            }

            //add e nodes to dom(m) so that dom(m) size is at most L, and add the remaining nodes to releasedNodes
            int numberOfAssignableNodesToM = maxClientsPerCenter - mon.getClients().size();
            int numberOfNodesToRelease = nodesToAssignToM.size() - numberOfAssignableNodesToM;
            List<Vertex> releasedClients = shuffleAndReduceToSize(nodesToAssignToM, numberOfNodesToRelease);
            nodesToAssignToM.removeAll(releasedClients);
            mon.addClients(new HashSet<>(nodesToAssignToM));
            mon.getClients().forEach(client -> client.setCenter(mon));

            //add releasedClients to passed(Parent(m)) if m.getParent() != null
            //else
            //create a new center from freeNodes and assign releasedClients to it
            if (mon.getParent() != null) {
                passed.get(mon.getDeputy()).addAll(releasedClients);
            } else {
                if (!releasedClients.isEmpty()) {
                    Vertex center = getFreeNodes(mon.getEmpire()).stream().findAny().get();
                    center.setColor(RED);
                    center.addClients(new HashSet<>(releasedClients));
                    center.getClients().forEach(client -> client.setCenter(center));
                }
            }

            monarchTree.remove(mon);

        }
    }

    //Will not be implemented...
    /*private void chainRoutingProblem(Graph<Vertex, WeightedEdgeWithCapacity> subGraph, Map<WeightedEdgeWithCapacity, Integer> load) {
        boolean toContinue = false;
        double lambda = 5;
        double gamma = 2;
        int numberOfEdges = subGraph.edgeSet().size();
        do {
            Graph<Vertex, WeightedEdgeWithCapacity> calcGraph = new SimpleWeightedGraph<>(WeightedEdgeWithCapacity.class);
            subGraph.vertexSet().forEach(calcGraph::addVertex);
            Map<WeightedEdgeWithCapacity, WeightedEdgeWithCapacity> edgeToEdge = new HashMap<>();
            for(WeightedEdgeWithCapacity e : subGraph.edgeSet()) {
                WeightedEdgeWithCapacity newEdge = calcGraph.addEdge(subGraph.getEdgeSource(e), subGraph.getEdgeTarget(e));
                newEdge.setCapacity(e.getCapacity());
                edgeToEdge.put(newEdge, e);
                //TODO replace 88 with di(k)
                calcGraph.setEdgeWeight(newEdge,
                        Math.pow(gamma, load.get(e) / (newEdge.getCapacity() * lambda))
                                * (Math.pow(gamma, 88 / (newEdge.getCapacity() * lambda)) - 1));
            };

            //TODO calculate shortest path tour
            List<WeightedEdgeWithCapacity> shortestpathtour = new ArrayList<>();

            for(WeightedEdgeWithCapacity e : shortestpathtour) {
                int edgeLoad = load.get(edgeToEdge.get(e));
                int trafficInWalk = 12; //TODO
                if(edgeLoad + trafficInWalk >= lambda * (Math.log(4 * numberOfEdges) / Math.log(gamma))) {
                    lambda = lambda * 2;
                    toContinue = true;
                    break;
                }
            }
        } while(toContinue);

    }*/


}
