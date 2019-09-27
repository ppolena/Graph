package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import java.awt.*;

import static elte.peterpolena.graph.Config.vertexRadius;
import static java.awt.Color.BLACK;

public class GraphPainter extends JPanel {

    private Graph<Vertex, DefaultWeightedEdge> graph;
    private boolean showEdgeWeight;

    public GraphPainter(Graph graph, boolean showEdgeWeight) {
        super();
        this.graph = graph;
        this.showEdgeWeight = showEdgeWeight;
        this.setBackground(Color.WHITE);
        this.setName("GraphPainter");
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        graph.edgeSet().forEach(edge -> {
            Vertex source = graph.getEdgeSource(edge);
            Vertex target = graph.getEdgeTarget(edge);
            g.setColor(BLACK);
            g.drawLine(
                    source.getX() + vertexRadius,
                    source.getY() + vertexRadius,
                    target.getX() + vertexRadius,
                    target.getY() + vertexRadius);
            if (showEdgeWeight) {
                g.drawString(
                        String.valueOf(graph.getEdgeWeight(edge)),
                        getMiddleXOfEdge(source.getX(), target.getX()),
                        getMiddleYOfEdge(source.getY(), target.getY()));
            }
        });

        graph.vertexSet().forEach(vertex -> {
            g.setColor(vertex.getColor());
            g.fillOval(
                    vertex.getX(),
                    vertex.getY(),
                    vertexRadius * 2,
                    vertexRadius * 2);
        });
    }

    private int getMiddleXOfEdge(int x1, int x2){
        return (int) Math.round((x1 + x2) / 2.0);
    }

    private int getMiddleYOfEdge(int y1, int y2){
        return (int) Math.round((y1 + y2) / 2.0);
    }
}
