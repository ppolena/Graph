package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

import static java.awt.Color.BLUE;

public class Utils {
    static public List<Vertex> getAdjacentVerticesUpToDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
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

    static public List<Vertex> getAdjacentVerticesAtDistance(Graph<Vertex, DefaultWeightedEdge> graph, Vertex source, int distance) {
        Set<Vertex> vertices = new HashSet<>();
        vertices.addAll(getAdjacentVerticesUpToDistance(graph, source, distance));
        if(distance > 1)
            vertices.removeAll(getAdjacentVerticesUpToDistance(graph, source, distance -1));
        vertices.remove(source);
        return new ArrayList<>(vertices);
    }

    static public List<Vertex> shuffleAndReduceToSize(List<Vertex> vertices, int size) {
        List<Vertex> list = new ArrayList<>(vertices);
        Collections.shuffle(list);
        if (size > list.size()) {
            return list;
        } else if (size < 0) {
            return new ArrayList<>();
        }
        return list.subList(0, size);
    }
    static public  boolean hasUnmarkedNodesFurther(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> fromSet, int distance) {
        return true; //TODO implement
    }

    static public  Vertex getRandomVertexFromDistance(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> distanceFrom, List<Vertex> fromList, int distance) {
        return new Vertex(0, 0, BLUE); //TODO implement
    }

    static public  Vertex getALeaf(Set<Vertex> tree) {
        return tree.stream().filter(x -> tree.stream().noneMatch(y -> y.getParent() == x)).findAny().get();
    }
}
