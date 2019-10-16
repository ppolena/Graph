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
import static elte.peterpolena.graph.Config.maxCenters;
import static elte.peterpolena.graph.Config.maxClientsPerCenter;
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
    private int maxCentersValue;
    private int maxClientsPerCentersValue;

    public Window(){

        AlgorithmService algorithmService = new AlgorithmService();

        this.frame = new JFrame("Graph");
        this.frame.setSize(frameWidth, frameHeight);
        this.frame.setLocationRelativeTo(null);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        this.graphPainter = new GraphPainter(graph, showEdgeWeight);

        this.maxCentersValue = 1;
        this.maxClientsPerCentersValue = 1;

        GraphGenerator graphGenerator = new GraphGenerator();

        JSlider centerSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, centersSliderStartValue);
        centerSlider.setMinorTickSpacing(1);
        centerSlider.setMajorTickSpacing(1);
        centerSlider.setPaintTicks(true);
        centerSlider.setPaintLabels(true);
        centerSlider.setSnapToTicks(true);
        centerSlider.setName("CentersSlider");
        JLabel centersLabel = new JLabel("Centers");

        JSlider nodesSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, clientsSliderStartValue);
        nodesSlider.setMinorTickSpacing(1);
        nodesSlider.setMajorTickSpacing(1);
        nodesSlider.setPaintTicks(true);
        nodesSlider.setPaintLabels(true);
        nodesSlider.setSnapToTicks(true);
        nodesSlider.setName("NodesSlider");
        JLabel NodesLabel = new JLabel("Nodes");

        JCheckBox randomizedPlacementCheckBox = new JCheckBox("Randomized placement", randomizedPlacement);
        randomizedPlacementCheckBox.addItemListener(e -> {
            randomizedPlacement = e.getStateChange() == SELECTED;
            drawGraph(graphGenerator.generate(centerSlider.getValue(), nodesSlider.getValue(), randomizedPlacement));
        });

        JCheckBox showEdgeWeightCheckbox = new JCheckBox("Show edge weight", showEdgeWeight);
        showEdgeWeightCheckbox.addItemListener(e -> {
            showEdgeWeight = e.getStateChange() == SELECTED;
            drawGraph(this.graph);
        });

        JButton reloadButton = new JButton("Reload");
        reloadButton.addActionListener(e -> drawGraph(graphGenerator.generate(centerSlider.getValue(), nodesSlider.getValue(), randomizedPlacement)));

        JSpinner maxCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        ((JSpinner.DefaultEditor) maxCentersSpinner.getEditor()).getTextField().setEditable(false);
        maxCentersSpinner.addChangeListener(e -> this.maxCentersValue = (int) maxCentersSpinner.getValue());
        JLabel maxCentersLabel = new JLabel("Centers");

        JSpinner maxClientsPerCenterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxClientsPerCenter, 1));
        ((JSpinner.DefaultEditor) maxClientsPerCenterSpinner.getEditor()).getTextField().setEditable(false);
        maxClientsPerCenterSpinner.addChangeListener(e -> this.maxClientsPerCentersValue = (int) maxClientsPerCenterSpinner.getValue());
        JLabel maxClientsPerCenterLabel = new JLabel("Max Clients Per Centers");

        JButton executeMainAlgorithmButton = new JButton("Execute Main Algorithm");
        executeMainAlgorithmButton.addActionListener(e -> algorithmService.mainAlgorithm(this.graph, this.maxCentersValue, this.maxClientsPerCentersValue, 2, false));

        ChangeListener optionsChangeListener = e -> {
            JSlider slider = (JSlider) e.getSource();
            if (!slider.getValueIsAdjusting()) {
                drawGraph(graphGenerator.generate(centerSlider.getValue(), nodesSlider.getValue(), randomizedPlacement));
            }
        };
        centerSlider.addChangeListener(optionsChangeListener);
        nodesSlider.addChangeListener(optionsChangeListener);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new FlowLayout());
        optionsPanel.setSize(sliderPanelWidth, sliderPanelHeight);
//        optionsPanel.add(centersLabel);
//        optionsPanel.add(centerSlider);
        optionsPanel.add(NodesLabel);
        optionsPanel.add(nodesSlider);
        optionsPanel.add(randomizedPlacementCheckBox);
        optionsPanel.add(showEdgeWeightCheckbox);
        optionsPanel.add(reloadButton);
        optionsPanel.add(maxCentersLabel);
        optionsPanel.add(maxCentersSpinner);
        optionsPanel.add(maxClientsPerCenterLabel);
        optionsPanel.add(maxClientsPerCenterSpinner);
        optionsPanel.add(executeMainAlgorithmButton);

        this.frame.add(optionsPanel, BorderLayout.SOUTH);

        this.frame.validate();
        this.frame.repaint();
        this.frame.setVisible(true);
    }

    private void drawGraph(Graph<Vertex, DefaultWeightedEdge> graph) {
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
