package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static elte.peterpolena.graph.Config.clientsSliderStartValue;
import static elte.peterpolena.graph.Config.frameHeight;
import static elte.peterpolena.graph.Config.frameWidth;
import static elte.peterpolena.graph.Config.maxCenters;
import static elte.peterpolena.graph.Config.maxClientsPerCenter;
import static elte.peterpolena.graph.Config.sliderMaxValue;
import static elte.peterpolena.graph.Config.sliderMinValue;
import static elte.peterpolena.graph.Config.sliderPanelHeight;
import static elte.peterpolena.graph.Config.sliderPanelWidth;
import static elte.peterpolena.graph.Config.timerDelay;
import static elte.peterpolena.graph.Utils.copy;
import static elte.peterpolena.graph.Utils.getCentersCount;
import static java.awt.event.ItemEvent.SELECTED;
import static java.util.stream.Collectors.toList;

@Service
public class Window {

    private JFrame frame;
    private Graph<Vertex, DefaultWeightedEdge> graph;
    private GraphPainter graphPainter;
    private boolean randomizedPlacement = false;
    private boolean showEdgeWeight = true;
    private int maxCentersValue;
    private int maxClientsPerCentersValue;

    public Window(){

        this.frame = new JFrame("Graph");
        this.frame.setSize(frameWidth, frameHeight);
        this.frame.setLocationRelativeTo(null);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.graphPainter = new GraphPainter(graph, showEdgeWeight);

        this.maxCentersValue = 1;
        this.maxClientsPerCentersValue = 1;

        GraphGenerator graphGenerator = new GraphGenerator();

//        JSlider centerSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, centersSliderStartValue);
//        centerSlider.setMinorTickSpacing(1);
//        centerSlider.setMajorTickSpacing(1);
//        centerSlider.setPaintTicks(true);
//        centerSlider.setPaintLabels(true);
//        centerSlider.setSnapToTicks(true);
//        centerSlider.setName("CentersSlider");
//        JLabel centersLabel = new JLabel("Centers");

        JSlider nodesSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, clientsSliderStartValue);
        nodesSlider.setMinorTickSpacing(1);
        nodesSlider.setMajorTickSpacing(1);
        nodesSlider.setPaintTicks(true);
        nodesSlider.setPaintLabels(true);
        nodesSlider.setSnapToTicks(true);
        nodesSlider.setName("NodesSlider");
        JLabel NodesLabel = new JLabel("Nodes (V)");

        JCheckBox randomizedPlacementCheckBox = new JCheckBox("Randomized placement", randomizedPlacement);
        randomizedPlacementCheckBox.addItemListener(e -> {
            randomizedPlacement = e.getStateChange() == SELECTED;
            Graph<Vertex, DefaultWeightedEdge> generatedGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
            drawGraph(generatedGraph);
        });

        JCheckBox showEdgeWeightCheckbox = new JCheckBox("Show edge weight", showEdgeWeight);
        showEdgeWeightCheckbox.addItemListener(e -> {
            showEdgeWeight = e.getStateChange() == SELECTED;
            drawGraph(this.graph);
        });

        JButton reloadButton = new JButton("Reload");
        reloadButton.addActionListener(e -> {
            Graph<Vertex, DefaultWeightedEdge> generateGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
            drawGraph(generateGraph);
        });

        JSpinner maxCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        ((JSpinner.DefaultEditor) maxCentersSpinner.getEditor()).getTextField().setEditable(false);
        maxCentersSpinner.addChangeListener(e -> this.maxCentersValue = (int) maxCentersSpinner.getValue());
        JLabel maxCentersLabel = new JLabel("Max Centers (K)");

        JSpinner maxClientsPerCenterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxClientsPerCenter, 1));
        ((JSpinner.DefaultEditor) maxClientsPerCenterSpinner.getEditor()).getTextField().setEditable(false);
        maxClientsPerCenterSpinner.addChangeListener(e -> this.maxClientsPerCentersValue = (int) maxClientsPerCenterSpinner.getValue());
        JLabel maxClientsPerCenterLabel = new JLabel("Max Clients Per Centers (L)");

        JButton executeMainAlgorithmButton = new JButton("Execute Main Algorithm");
        executeMainAlgorithmButton.addActionListener(e -> executeMainAlgorithm());

        ChangeListener optionsChangeListener = e -> {
            JSlider slider = (JSlider) e.getSource();
            if (!slider.getValueIsAdjusting()) {
                Graph<Vertex, DefaultWeightedEdge> generateGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
                drawGraph(generateGraph);
            }
        };
//        centerSlider.addChangeListener(optionsChangeListener);
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

    private void executeMainAlgorithm() {

        this.drawGraph(this.graph);

        Result result = new AlgorithmService().mainAlgorithm(
                this.graph,
                this.maxCentersValue,
                this.maxClientsPerCentersValue,
                2,
                false);

        System.out.println("\nSTART DRAWING RESULT\n");
        if (result != null) {
            drawSubGraphs(result);
        } else {
            System.out.println("NOT SOLVABLE");
        }
    }

    private void drawSubGraphs(Result result) {

		Map<Graph<Vertex, DefaultWeightedEdge>, Set<Graph<Vertex, DefaultWeightedEdge>>> subGraphsAndConnectedComponents = result.getSubGraphsAndConnectedComponents();
		List<Graph<Vertex, DefaultWeightedEdge>> graphsToDraw = new ArrayList<>();
        subGraphsAndConnectedComponents.forEach((subGraph, connectedComponents) -> {
            if (!subGraph.vertexSet().isEmpty() && !subGraph.edgeSet().isEmpty()) {
                graphsToDraw.add(subGraph);
			}
            graphsToDraw.addAll(connectedComponents
                    .stream()
					.filter(graph ->
							!graph.vertexSet().isEmpty() &&
									!graph.edgeSet().isEmpty())
					.collect(toList()));
		});

        ActionListener drawSubGraphsListener = new ActionListener() {
			int graphIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                Timer sourceTimer = (Timer) e.getSource();
				if (graphIndex == graphsToDraw.size()) {
                    System.out.println("\tDrawing result");
                    drawGraph(result.getResult());
					System.out.println("\tCenters drawn: " + getCentersCount(result.getResult()));
                    System.out.println("\nEND DRAWING RESULT\n");
                    sourceTimer.stop();
					resetToOriginal(result.getOriginalGraph());
                } else {
					Graph<Vertex, DefaultWeightedEdge> graph = graphsToDraw.get(graphIndex);
//                    double weight = subGraph.edgeSet().stream().mapToDouble(subGraph::getEdgeWeight).max().orElse(0);
//                    System.out.println("\tDrawing subGraph with edge weights up to " + weight);
					int vertexCount = graph.vertexSet().size();
					int edgeCount = graph.edgeSet().size();
					System.out.println("\t(" + graphsToDraw.size() + "/" + (graphIndex + 1) + ") Drawing graph with " + vertexCount + " vertices and " + edgeCount + " edges...");
					drawGraph(graph);
                    //TODO: find a way to draw subGraphs of subGraphs and delay timers inside timers, something like in the commented section
//
//                    Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponents = result.getConnectedComponentsOfSubGraphsInAssignCenters().get(graphIndex);
//                    int delay = connectedComponents.size() * timerDelay;
//                    sourceTimer.setDelay(delay);
//                    drawConnectedComponents(result, connectedComponents);
					++graphIndex;
                }
            }
        };
        Timer drawSubGraphsTimer = new Timer(timerDelay, drawSubGraphsListener);
        drawSubGraphsTimer.setInitialDelay(0);
        drawSubGraphsTimer.start();
    }

//    private void drawConnectedComponents(Result result, Set<Graph<Vertex, DefaultWeightedEdge>> connectedComponents) {
//        ActionListener drawConnectedComponentsListener = new ActionListener() {
//            List<Graph<Vertex, DefaultWeightedEdge>> connectedComponentList = new ArrayList<>(connectedComponents);
//            int connectedComponentIndex = 0;
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                Timer sourceTimer = (Timer) e.getSource();
//                if(connectedComponentIndex == connectedComponents.size()){
//                    sourceTimer.stop();
//                } else {
//                    Graph<Vertex, DefaultWeightedEdge> connectedComponent = connectedComponentList.get(connectedComponentIndex);
//                    System.out.println("\t\tDrawing connected component of subGraph");
//                    drawGraph(connectedComponent);
//                    ++connectedComponentIndex;
//                }
//            }
//        };
//        Timer drawConnectedComponentsTimer = new Timer(timerDelay, drawConnectedComponentsListener);
//        drawConnectedComponentsTimer.start();
//    }

    private void drawGraph(Graph<Vertex, DefaultWeightedEdge> graph) {
        this.frame.remove(graphPainter);
        this.frame.validate();
        this.frame.repaint();

        this.graph = copy(graph);
        this.graphPainter = new GraphPainter(this.graph, showEdgeWeight);

        this.frame.add(graphPainter, BorderLayout.CENTER);
        this.frame.validate();
        this.frame.repaint();
    }

    private void resetToOriginal(Graph<Vertex, DefaultWeightedEdge> graph) {
        this.graph = copy(graph);
    }
}
