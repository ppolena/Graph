package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.awt.Color.MAGENTA;
import static java.awt.Color.PINK;

public class Result {

	private Graph<Vertex, DefaultWeightedEdge> originalGraph;
	private List<Graph<Vertex, DefaultWeightedEdge>> graphsToDraw;
	private List<String> descriptions;

    public Result() {
		this.originalGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		this.graphsToDraw = new ArrayList<>();
		this.descriptions = new ArrayList<>();
    }

	public Graph<Vertex, DefaultWeightedEdge> getOriginalGraph() {
		return originalGraph;
    }

	public List<Graph<Vertex, DefaultWeightedEdge>> getGraphsToDraw() {
		return this.graphsToDraw;
	}

	public List<String> getDescriptions() {
		return this.descriptions;
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

	public void addGraphToDraw(final String description, final Graph<Vertex, DefaultWeightedEdge> graphToDraw) {
		System.out.println("\t\t\tADDING " + description);
		Graph<Vertex, DefaultWeightedEdge> copyOfGraphToDraw = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		if (!graphToDraw.edgeSet().isEmpty()) {
			graphToDraw.edgeSet().forEach(edge -> {
				Vertex source = new Vertex(graphToDraw.getEdgeSource(edge));
				Vertex target = new Vertex(graphToDraw.getEdgeTarget(edge));
				copyOfGraphToDraw.addVertex(source);
				copyOfGraphToDraw.addVertex(target);
				copyOfGraphToDraw.addEdge(source, target);
				copyOfGraphToDraw.setEdgeWeight(source, target, graphToDraw.getEdgeWeight(edge));
			});
			add(description, copyOfGraphToDraw);
		} else if (!graphToDraw.vertexSet().isEmpty()) {
			graphToDraw.vertexSet().forEach(vertex -> copyOfGraphToDraw.addVertex(new Vertex(vertex)));
			add(description, copyOfGraphToDraw);
		}
	}

	public void addGraphWithMonarchsToDraw(final String description, final Graph<Vertex, DefaultWeightedEdge> graphToDraw, Set<Vertex> minors, Set<Vertex> majors) {
		System.out.println("\t\t\tADDING " + description);
		Graph<Vertex, DefaultWeightedEdge> copyOfGraphToDraw = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		if (!graphToDraw.edgeSet().isEmpty()) {
			graphToDraw.edgeSet().forEach(edge -> {
				Vertex originalEdgeSource = graphToDraw.getEdgeSource(edge);
				Vertex originalEdgeTarget = graphToDraw.getEdgeTarget(edge);
				Vertex source = new Vertex(originalEdgeSource);
				Vertex target = new Vertex(originalEdgeTarget);
				setMonarchColor(minors, majors, originalEdgeSource, source);
				setMonarchColor(minors, majors, originalEdgeTarget, target);
				copyOfGraphToDraw.addVertex(source);
				copyOfGraphToDraw.addVertex(target);
				copyOfGraphToDraw.addEdge(source, target);
				copyOfGraphToDraw.setEdgeWeight(source, target, graphToDraw.getEdgeWeight(edge));
			});
			add(description, copyOfGraphToDraw);
		} else if (!graphToDraw.vertexSet().isEmpty()) {
			graphToDraw.vertexSet().forEach(vertex -> copyOfGraphToDraw.addVertex(new Vertex(vertex)));
			add(description, copyOfGraphToDraw);
		}
	}

	public void addBipartiteGraphToDraw(final String description, final Graph<Vertex, WeightedEdgeWithCapacity> bipartiteGraphToDraw) {
		System.out.println("\t\t\tADDING " + description);
		Graph<Vertex, DefaultWeightedEdge> copyOfBipartiteGraphToDraw = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		if (!bipartiteGraphToDraw.edgeSet().isEmpty()) {
			bipartiteGraphToDraw.edgeSet().forEach(edge -> {
				Vertex source = new Vertex(bipartiteGraphToDraw.getEdgeSource(edge));
				Vertex target = new Vertex(bipartiteGraphToDraw.getEdgeTarget(edge));
				copyOfBipartiteGraphToDraw.addVertex(source);
				copyOfBipartiteGraphToDraw.addVertex(target);
				copyOfBipartiteGraphToDraw.addEdge(source, target);
				copyOfBipartiteGraphToDraw.setEdgeWeight(source, target, bipartiteGraphToDraw.getEdgeWeight(edge));
			});
			add(description, copyOfBipartiteGraphToDraw);
		} else if (!bipartiteGraphToDraw.vertexSet().isEmpty()) {
			bipartiteGraphToDraw.vertexSet().forEach(vertex -> copyOfBipartiteGraphToDraw.addVertex(new Vertex(vertex)));
			add(description, copyOfBipartiteGraphToDraw);
		}
	}

	private void setMonarchColor(Set<Vertex> minors, Set<Vertex> majors, Vertex originalVertex, Vertex copiedVertex) {
		if (minors.contains(originalVertex)) {
			copiedVertex.setColor(PINK);
		} else if (majors.contains(originalVertex)) {
			copiedVertex.setColor(MAGENTA);
		}
	}

	private void add(String description, Graph<Vertex, DefaultWeightedEdge> copyOfGraphToDraw) {
		this.graphsToDraw.add(copyOfGraphToDraw);
		this.descriptions.add(description);
	}
}
