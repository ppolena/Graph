package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static elte.peterpolena.graph.Config.centerX;
import static elte.peterpolena.graph.Config.centerY;
import static elte.peterpolena.graph.Config.graphRadius;
import static elte.peterpolena.graph.Config.maxWeight;
import static elte.peterpolena.graph.Config.maxXCoordinate;
import static elte.peterpolena.graph.Config.maxYCoordinate;
import static elte.peterpolena.graph.Config.minWeight;
import static elte.peterpolena.graph.Config.minXCoordinate;
import static elte.peterpolena.graph.Config.minYCoordinate;
import static java.awt.Color.BLACK;
import static java.awt.Color.RED;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class GraphGenerator {

    public Graph<Vertex, DefaultWeightedEdge> generate(int centers, int clients, boolean randomizedPlacement){
        List<Vertex> vertices = randomizedPlacement ?
                generateVerticesRandomly(centers, clients) :
                generateVerticesInCircle(centers, clients);

        Graph<Vertex, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        vertices.forEach(graph::addVertex);
        for(int i = 0; i < vertices.size(); ++i) {
            for(int j = i + 1; j < vertices.size(); ++j){
                graph.addEdge(vertices.get(i), vertices.get(j), new DefaultWeightedEdge());
                graph.setEdgeWeight(vertices.get(i), vertices.get(j), getRandomIntInRange(minWeight, maxWeight));
            }
        }

        return graph;
    }

    private List<Vertex> generateVerticesInCircle(int centers, int clients) {
        Set<Vertex> vertices = new HashSet<>();
        List<Integer> indices = generateCenterIndices(centers, clients);
        int vertexCount = (centers + clients);
        int index = 0;
        double angle = 360.0 / vertexCount;
        for (double i = 0.0; i < 360.0; i += angle) {
            int x = centerX + (int) round((graphRadius + (graphRadius * vertexCount * 0.025)) * cos(toRadians(i)));
            int y = centerY + (int) round((graphRadius + (graphRadius * vertexCount * 0.025)) * sin(toRadians(i)));
            if(indices.contains(index)){
                vertices.add(new Vertex(x, y, RED));
                indices.remove(Integer.valueOf(index));
            } else {
                vertices.add(new Vertex(x, y, BLACK));
            }
            ++index;
        }
        return new ArrayList<>(vertices);
    }

    private List<Vertex> generateVerticesRandomly(int centers, int clients) {
        Set<Vertex> vertices = new HashSet<>();
        List<Integer> indices = generateCenterIndices(centers, clients);
        int vertexCount = (centers + clients);
        for(int index = 0; index < vertexCount; ++index){
            if(indices.contains(index)) {
                vertices.add(getRandomVertex(RED));
                indices.remove(Integer.valueOf(index));
            } else {
                vertices.add(getRandomVertex(BLACK));
            }
        }
        return new ArrayList<>(vertices);
    }

    private Vertex getRandomVertex(Color color){
        return new Vertex(
                getRandomIntInRange(minXCoordinate, maxXCoordinate),
                getRandomIntInRange(minYCoordinate, maxYCoordinate),
                color);
    }

    private List<Integer> generateCenterIndices(int centers, int clients){
        List<Integer> indices = new ArrayList<>();
        int vertexCount = (centers + clients);
        for (int i = 0; i < centers; ++i) {
            int index = getRandomIntInRange(0, vertexCount - 1);
            while (indices.contains(index)) {
                index = getRandomIntInRange(0, vertexCount - 1);
            }
            indices.add(index);
        }
        return indices;
    }

    private int getRandomIntInRange(int low, int high){
        Random rnd = new Random();
        return rnd.nextInt(high - low) + low;
    }
}
