package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Result {

	private Graph<Vertex, DefaultWeightedEdge> originalGraph;
	private Map<Graph<Vertex, DefaultWeightedEdge>, Set<Graph<Vertex, DefaultWeightedEdge>>> subGraphsAndConnectedComponents;
	private List<Set<Vertex>> majorMonarchs;
	private List<Set<Vertex>> minorMonarchs;
	private List<Set<Vertex>> monarchs;
	private List<Graph<Vertex, WeightedEdgeWithCapacity>> bipartiteGraphs;
    private Graph<Vertex, DefaultWeightedEdge> result;

    public Result() {
		this.originalGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		this.subGraphsAndConnectedComponents = new HashMap<>();
		this.majorMonarchs = new ArrayList<>();
		this.minorMonarchs = new ArrayList<>();
		this.monarchs = new ArrayList<>();
		this.bipartiteGraphs = new ArrayList<>();
        this.result = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    }

	public Graph<Vertex, DefaultWeightedEdge> getOriginalGraph() {
		return originalGraph;
    }

	public Map<Graph<Vertex, DefaultWeightedEdge>, Set<Graph<Vertex, DefaultWeightedEdge>>> getSubGraphsAndConnectedComponents() {
		return subGraphsAndConnectedComponents;
    }

	public List<Set<Vertex>> getMajorMonarchs() {
		return majorMonarchs;
    }

	public List<Set<Vertex>> getMinorMonarchs() {
		return minorMonarchs;
    }

	public List<Set<Vertex>> getMonarchs() {
		return monarchs;
    }

	public List<Graph<Vertex, WeightedEdgeWithCapacity>> getBipartiteGraphs() {
		return bipartiteGraphs;
    }

    public Graph<Vertex, DefaultWeightedEdge> getResult() {
        return this.result;
    }

	public void setOriginalGraph(final Graph<Vertex, DefaultWeightedEdge> originalGraph) {
        originalGraph.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(originalGraph.getEdgeSource(edge));
            Vertex target = new Vertex(originalGraph.getEdgeTarget(edge));
			this.originalGraph.addVertex(source);
			this.originalGraph.addVertex(target);
			this.originalGraph.addEdge(source, target);
			this.originalGraph.setEdgeWeight(source, target, originalGraph.getEdgeWeight(edge));
        });
    }

//    public void addSubGraphOfOriginalGraphByWeight(final Graph<Vertex, DefaultWeightedEdge> subGraphOfOriginalGraphByWeight) {
//        Graph<Vertex, DefaultWeightedEdge> subGraphCopy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//        subGraphOfOriginalGraphByWeight.edgeSet().forEach(edge -> {
//            Vertex source = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeSource(edge));
//            Vertex target = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeTarget(edge));
//            subGraphCopy.addVertex(source);
//            subGraphCopy.addVertex(target);
//            subGraphCopy.addEdge(source, target);
//            subGraphCopy.setEdgeWeight(source, target, subGraphOfOriginalGraphByWeight.getEdgeWeight(edge));
//        });
//        this.subGraphsOfOriginalGraphByWeight.add(subGraphCopy);
//    }

	public void addConnectedComponentsOfSubGraph(final Graph<Vertex, DefaultWeightedEdge> subGraphOfOriginalGraphByWeight,
												 final Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponentsOfSubGraph) {

        Graph<Vertex, DefaultWeightedEdge> subGraphCopy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        subGraphOfOriginalGraphByWeight.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeSource(edge));
            Vertex target = new Vertex(subGraphOfOriginalGraphByWeight.getEdgeTarget(edge));
            subGraphCopy.addVertex(source);
            subGraphCopy.addVertex(target);
            subGraphCopy.addEdge(source, target);
            subGraphCopy.setEdgeWeight(source, target, subGraphOfOriginalGraphByWeight.getEdgeWeight(edge));
        });

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
		this.subGraphsAndConnectedComponents.put(subGraphCopy, connectedComponentsOfSubGraphCopy);
    }

	public void addMajorMonarchs(final Set<Vertex> majorMonarchs) {
        Set<Vertex> majorMonarchsCopy = new HashSet<>();
        majorMonarchs.forEach(majorMonarch -> majorMonarchsCopy.add(new Vertex(majorMonarch)));
		this.majorMonarchs.add(majorMonarchsCopy);
    }

	public void addMinorMonarchs(final Set<Vertex> minorMonarchs) {
        Set<Vertex> minorMonarchsCopy = new HashSet<>();
        minorMonarchs.forEach(minorMonarch -> minorMonarchsCopy.add(new Vertex(minorMonarch)));
		this.minorMonarchs.add(minorMonarchsCopy);
    }

	public void addMonarchs(final Set<Vertex> monarchs) {
        Set<Vertex> monarchsCopy = new HashSet<>();
        monarchs.forEach(monarch -> monarchsCopy.add(new Vertex(monarch)));
		this.monarchs.add(monarchsCopy);
    }

	public void addBipartiteGraphFromMonarchsAndSubGraph(final Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraphFromMonarchsAndSubGraph) {
        Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraphFromMonarchsAndSubGraphCopy = new SimpleDirectedWeightedGraph<>(WeightedEdgeWithCapacity.class);
        bipartiteGraphFromMonarchsAndSubGraph.edgeSet().forEach(edge -> {
            Vertex source = new Vertex(bipartiteGraphFromMonarchsAndSubGraph.getEdgeSource(edge));
            Vertex target = new Vertex(bipartiteGraphFromMonarchsAndSubGraph.getEdgeTarget(edge));
            bipartiteGraphFromMonarchsAndSubGraphCopy.addVertex(source);
            bipartiteGraphFromMonarchsAndSubGraphCopy.addVertex(target);
            bipartiteGraphFromMonarchsAndSubGraphCopy.addEdge(source, target);
            bipartiteGraphFromMonarchsAndSubGraphCopy.setEdgeWeight(source, target, bipartiteGraphFromMonarchsAndSubGraph.getEdgeWeight(edge));
        });
		this.bipartiteGraphs.add(bipartiteGraphFromMonarchsAndSubGraphCopy);
    }

	public void setResult(final Graph<Vertex, DefaultWeightedEdge> result) {
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
//        this.originalGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//        this.subGraphsOfOriginalGraphByWeightInMain.clear();
//        this.connectedComponentsOfSubGraphsInAssignCenters.clear();
//        this.majorMonarchs.clear();
//        this.minorMonarchs.clear();
//        this.monarchs.clear();
//        this.bipartiteGraphs.clear();
//        this.result = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
//    }
}
