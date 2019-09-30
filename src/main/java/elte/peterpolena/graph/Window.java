package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        JButton executeMainAlgorithmButton = new JButton("Execute Main Algorithm");
        executeMainAlgorithmButton.addActionListener(e -> mainAlgorithm());

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
        optionsPanel.add(executeMainAlgorithmButton);

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

    private boolean mainAlgorithm() {
        List<DefaultWeightedEdge> edges = new ArrayList<>(this.graph.edgeSet());
        Comparator<DefaultWeightedEdge> byWeight = getDefaultWeightedEdgeComparator();
        edges.sort(byWeight);

        List<Graph<Vertex, DefaultWeightedEdge>> subGraphs = new ArrayList<>();
        edges.forEach(edge -> {
            double maxWeight = this.graph.getEdgeWeight(edge);
            Graph<Vertex, DefaultWeightedEdge> subGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
            this.graph.vertexSet().forEach(subGraph::addVertex);
            this.graph.edgeSet()
                    .stream()
                    .filter(edgeToFilter -> this.graph.getEdgeWeight(edgeToFilter) <= maxWeight)
                    .collect(Collectors.toList())
                    .forEach(edgeToAdd -> subGraph
                            .addEdge(
                                    this.graph.getEdgeSource(edgeToAdd),
                                    this.graph.getEdgeTarget(edgeToAdd),
                                    edgeToAdd));
            subGraphs.add(subGraph);
        });

        for (Graph<Vertex, DefaultWeightedEdge> subGraph : subGraphs) {
            if (assignCentersAlgorithm(subGraph)) {
                return true;
            }
        }
        return false;
    }

    private Comparator<DefaultWeightedEdge> getDefaultWeightedEdgeComparator() {
        return (DefaultWeightedEdge edge1, DefaultWeightedEdge edge2) -> {
            if (this.graph.getEdgeWeight(edge1) < this.graph.getEdgeWeight(edge2)) {
                return -1;
            }
            if (this.graph.getEdgeWeight(edge1) > this.graph.getEdgeWeight(edge2)) {
                return 1;
            }
            return 0;
        };
    }

    private boolean assignCentersAlgorithm(Graph<Vertex, DefaultWeightedEdge> graph) {
        ConnectivityInspector<Vertex, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(graph);
        List<Set<Vertex>> connectedComponents = connectivityInspector.connectedSets();
        return false;
    }
}
