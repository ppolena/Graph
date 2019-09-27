package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

import static elte.peterpolena.graph.Config.centersSliderStartValue;
import static elte.peterpolena.graph.Config.clientsSliderStartValue;
import static elte.peterpolena.graph.Config.frameHeight;
import static elte.peterpolena.graph.Config.frameWidth;
import static elte.peterpolena.graph.Config.sliderMaxValue;
import static elte.peterpolena.graph.Config.sliderMinValue;
import static elte.peterpolena.graph.Config.sliderPanelHeight;
import static elte.peterpolena.graph.Config.sliderPanelWidth;
import static java.awt.event.ItemEvent.SELECTED;

public class Window {

    private JFrame frame;
    private Graph<Vertex, DefaultWeightedEdge> graph;
    private GraphPainter graphPainter;
    private boolean randomizedPlacement = false;
    private boolean showEdgeWeight = true;

    public Window(){
        this.frame = new JFrame("Graph");
        this.frame.setSize(frameWidth, frameHeight);
        this.frame.setLocationRelativeTo(null);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        this.graphPainter = new GraphPainter(graph, showEdgeWeight);

        GraphGenerator graphGenerator = new GraphGenerator();

        JSlider centerSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, centersSliderStartValue);
        centerSlider.setMinorTickSpacing(1);
        centerSlider.setMajorTickSpacing(1);
        centerSlider.setPaintTicks(true);
        centerSlider.setPaintLabels(true);
        centerSlider.setSnapToTicks(true);
        centerSlider.setName("CentersSlider");
        JLabel centersLabel = new JLabel("Centers");

        JSlider clientSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, clientsSliderStartValue);
        clientSlider.setMinorTickSpacing(1);
        clientSlider.setMajorTickSpacing(1);
        clientSlider.setPaintTicks(true);
        clientSlider.setPaintLabels(true);
        clientSlider.setSnapToTicks(true);
        clientSlider.setName("ClientsSlider");
        JLabel clientsLabel = new JLabel("Clients");

        JCheckBox randomizedPlacementCheckBox = new JCheckBox("Randomized placement", randomizedPlacement);
        randomizedPlacementCheckBox.addItemListener(e -> {
            randomizedPlacement = e.getStateChange() == SELECTED;
            drawGraph(graphGenerator.generate(centerSlider.getValue(), clientSlider.getValue(), randomizedPlacement));
        });

        JCheckBox showEdgeWeightCheckbox = new JCheckBox("Show edge weight", showEdgeWeight);
        showEdgeWeightCheckbox.addItemListener(e -> {
            showEdgeWeight = e.getStateChange() == SELECTED;
            drawGraph(this.graph);
        });

        JButton reloadButton = new JButton("Reload");
        reloadButton.addActionListener(e -> drawGraph(graphGenerator.generate(centerSlider.getValue(), clientSlider.getValue(), randomizedPlacement)));

        ChangeListener optionsChangeListener = e -> {
            JSlider slider = (JSlider) e.getSource();
            if (!slider.getValueIsAdjusting()) {
                drawGraph(graphGenerator.generate(centerSlider.getValue(), clientSlider.getValue(), randomizedPlacement));
            }
        };
        centerSlider.addChangeListener(optionsChangeListener);
        clientSlider.addChangeListener(optionsChangeListener);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new FlowLayout());
        optionsPanel.setSize(sliderPanelWidth, sliderPanelHeight);
        optionsPanel.add(centersLabel);
        optionsPanel.add(centerSlider);
        optionsPanel.add(clientsLabel);
        optionsPanel.add(clientSlider);
        optionsPanel.add(randomizedPlacementCheckBox);
        optionsPanel.add(showEdgeWeightCheckbox);
        optionsPanel.add(reloadButton);

        this.frame.add(optionsPanel, BorderLayout.SOUTH);

        this.frame.validate();
        this.frame.repaint();
        this.frame.setVisible(true);
    }

    public void drawGraph(Graph<Vertex, DefaultWeightedEdge> graph) {
        this.frame.remove(graphPainter);
        this.frame.validate();
        this.frame.repaint();

        this.graph = graph;
        this.graphPainter = new GraphPainter(graph, showEdgeWeight);

        this.frame.add(graphPainter, BorderLayout.CENTER);
        this.frame.validate();
        this.frame.repaint();
    }
}
