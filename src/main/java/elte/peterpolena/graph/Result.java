package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;
import java.util.Set;

public class Result {

    private Graph<Vertex, DefaultWeightedEdge> originalGraphInMain;
    private List<Graph<Vertex, DefaultWeightedEdge>> subgraphsByWeightOfOriginalGraphInMain;
    private List<Set<Graph<Vertex, DefaultWeightedEdge>>> connectedComponentsOfSubgraphsInAssignCenters;
    private List<Set<Vertex>> majorMonarchsInSelectMonarchs;
    private List<Set<Vertex>> minorMonarchsInSelectMonarchs;
    private List<Set<Vertex>> monarchsInSelectMonarchs;
    private List<Graph<Vertex, WeightedEdgeWithCapacity>> bipartiteGraphsFromMonarchsAndSubgraphsInAssignDomains;
    
}
