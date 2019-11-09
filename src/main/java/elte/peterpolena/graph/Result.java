package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Result {

    private Graph<Vertex, DefaultWeightedEdge> originalGraphInMain;
    private List<Graph<Vertex, DefaultWeightedEdge>> subGraphsOfOriginalGraphByWeightInMain;
    private List<Set<Graph<Vertex, DefaultWeightedEdge>>> connectedComponentsOfSubGraphsInAssignCenters;
    private List<Set<Vertex>> majorMonarchsInSelectMonarchs;
    private List<Set<Vertex>> minorMonarchsInSelectMonarchs;
    private List<Set<Vertex>> monarchsInSelectMonarchs;
    private List<Graph<Vertex, WeightedEdgeWithCapacity>> bipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains;
    private Graph<Vertex, DefaultWeightedEdge> result;

    public Result() {
        this.originalGraphInMain = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.subGraphsOfOriginalGraphByWeightInMain = new ArrayList<>();
        this.connectedComponentsOfSubGraphsInAssignCenters = new ArrayList<>();
        this.majorMonarchsInSelectMonarchs = new ArrayList<>();
        this.minorMonarchsInSelectMonarchs = new ArrayList<>();
        this.monarchsInSelectMonarchs = new ArrayList<>();
        this.bipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains = new ArrayList<>();
        this.result = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    }

    public Graph<Vertex, DefaultWeightedEdge> getOriginalGraphInMain() {
        return originalGraphInMain;
    }

    public List<Graph<Vertex, DefaultWeightedEdge>> getSubGraphsOfOriginalGraphByWeightInMain() {
        return subGraphsOfOriginalGraphByWeightInMain;
    }

    public List<Set<Graph<Vertex, DefaultWeightedEdge>>> getConnectedComponentsOfSubGraphsInAssignCenters() {
        return connectedComponentsOfSubGraphsInAssignCenters;
    }

    public List<Set<Vertex>> getMajorMonarchsInSelectMonarchs() {
        return majorMonarchsInSelectMonarchs;
    }

    public List<Set<Vertex>> getMinorMonarchsInSelectMonarchs() {
        return minorMonarchsInSelectMonarchs;
    }

    public List<Set<Vertex>> getMonarchsInSelectMonarchs() {
        return monarchsInSelectMonarchs;
    }

    public List<Graph<Vertex, WeightedEdgeWithCapacity>> getBipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains() {
        return bipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains;
    }

    public Graph<Vertex, DefaultWeightedEdge> getResult() {
        return this.result;
    }

    public void setOriginalGraph(Graph<Vertex, DefaultWeightedEdge> originalGraph) {
        originalGraph.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(originalGraph.getEdgeSource(edge));
            Vertex target = new Vertex(originalGraph.getEdgeTarget(edge));
            this.originalGraphInMain.addVertex(source);
            this.originalGraphInMain.addVertex(target);
            this.originalGraphInMain.addEdge(source, target);
            this.originalGraphInMain.setEdgeWeight(source, target, originalGraph.getEdgeWeight(edge));
        });
    }

    public void addSubGraphOfOriginalGraphByWeight(Graph<Vertex, DefaultWeightedEdge> subGraphOfOriginalGraphByWeight) {
        Graph<Vertex, DefaultWeightedEdge> subGraphCopy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        subGraphOfOriginalGraphByWeight.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeSource(edge));
            Vertex target = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeTarget(edge));
            subGraphCopy.addVertex(source);
            subGraphCopy.addVertex(target);
            subGraphCopy.addEdge(source, target);
            subGraphCopy.setEdgeWeight(source, target, subGraphOfOriginalGraphByWeight.getEdgeWeight(edge));
        });
        this.subGraphsOfOriginalGraphByWeightInMain.add(subGraphCopy);
    }

    public void addConnectedComponentsOfSubGraph(Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponentsOfSubGraph) {
        Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponentsOfSubGraphCopy = new HashSet<>();
        connectedComponentsOfSubGraph.forEach(connectedComponent -> {
            Graph<Vertex, DefaultWeightedEdge> connectedComponentCopy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
            connectedComponent.edgeSet().forEach(edge -> {
                Vertex source = new Vertex(connectedComponent.getEdgeSource(edge));
                Vertex target = new Vertex(connectedComponent.getEdgeTarget(edge));
                connectedComponentCopy.addVertex(source);
                connectedComponentCopy.addVertex(target);
                connectedComponentCopy.addEdge(source, target);
                connectedComponentCopy.setEdgeWeight(source, target, connectedComponent.getEdgeWeight(edge));
            });
            connectedComponentsOfSubGraphCopy.add(connectedComponentCopy);
        });
        this.connectedComponentsOfSubGraphsInAssignCenters.add(connectedComponentsOfSubGraphCopy);
    }

    public void addMajorMonarchs(Set<Vertex> majorMonarchs) {
        Set<Vertex> majorMonarchsCopy = new HashSet<>();
        majorMonarchs.forEach(majorMonarch -> majorMonarchsCopy.add(new Vertex(majorMonarch)));
        this.majorMonarchsInSelectMonarchs.add(majorMonarchsCopy);
    }

    public void addMinorMonarchs(Set<Vertex> minorMonarchs) {
        Set<Vertex> minorMonarchsCopy = new HashSet<>();
        minorMonarchs.forEach(minorMonarch -> minorMonarchsCopy.add(new Vertex(minorMonarch)));
        this.minorMonarchsInSelectMonarchs.add(minorMonarchsCopy);
    }

    public void addMonarchs(Set<Vertex> monarchs) {
        Set<Vertex> monarchsCopy = new HashSet<>();
        monarchs.forEach(monarch -> monarchsCopy.add(new Vertex(monarch)));
        this.monarchsInSelectMonarchs.add(monarchsCopy);
    }

    public void addBipartiteGraphFromMonarchsAndSubGraph(Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraphFromMonarchsAndSubGraph) {
        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraphFromMonarchsAndSubGraphCopy = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        bipartiteGraphFromMonarchsAndSubGraph.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(bipartiteGraphFromMonarchsAndSubGraph.getEdgeSource(edge));
            Vertex target = new Vertex(bipartiteGraphFromMonarchsAndSubGraph.getEdgeTarget(edge));
            bipartiteGraphFromMonarchsAndSubGraphCopy.addVertex(source);
            bipartiteGraphFromMonarchsAndSubGraphCopy.addVertex(target);
            bipartiteGraphFromMonarchsAndSubGraphCopy.addEdge(source, target);
            bipartiteGraphFromMonarchsAndSubGraphCopy.setEdgeWeight(source, target, bipartiteGraphFromMonarchsAndSubGraph.getEdgeWeight(edge));
        });
        this.bipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains.add(bipartiteGraphFromMonarchsAndSubGraphCopy);
    }

    public void setResult(Graph<Vertex, DefaultWeightedEdge> result) {
        result.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(result.getEdgeSource(edge));
            Vertex target = new Vertex(result.getEdgeTarget(edge));
            this.result.addVertex(source);
            this.result.addVertex(target);
            this.result.addEdge(source, target);
            this.result.setEdgeWeight(source, target, result.getEdgeWeight(edge));
        });
    }

//    public void reset(){
//        this.originalGraphInMain = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//        this.subGraphsOfOriginalGraphByWeightInMain.clear();
//        this.connectedComponentsOfSubGraphsInAssignCenters.clear();
//        this.majorMonarchsInSelectMonarchs.clear();
//        this.minorMonarchsInSelectMonarchs.clear();
//        this.monarchsInSelectMonarchs.clear();
//        this.bipartiteGraphsFromMonarchsAndSubGraphsInAssignDomains.clear();
//        this.result = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//    }
}
