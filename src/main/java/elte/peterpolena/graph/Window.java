package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

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

@Service
public class Window {

    private JFrame frame;
    private Graph<Vertex, DefaultWeightedEdge> graph;
    private GraphPainter graphPainter;
    private boolean randomizedPlacement = false;
    private boolean showEdgeWeight = true;
    private boolean isConservative = false;
    private int maxCentersValue;
    private int maxClientsPerCentersValue;
    private int maxFailedCentersValue;
    private JLabel descriptionLabel;

    public Window(){

        this.frame = new JFrame("Graph");
        this.frame.setSize(frameWidth, frameHeight);
        this.frame.setLocationRelativeTo(null);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        this.graphPainter = new GraphPainter(graph, showEdgeWeight);

        this.descriptionLabel = new JLabel("Vertices: " + this.graph.vertexSet().size() + ", Edges: " + this.graph.edgeSet().size());

        this.maxCentersValue = 1;
        this.maxClientsPerCentersValue = 1;
        this.maxFailedCentersValue = 2;

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
        nodesSlider.setToolTipText("Set the number of vertices");
        nodesSlider.setMinorTickSpacing(1);
        nodesSlider.setMajorTickSpacing(1);
        nodesSlider.setPaintTicks(true);
        nodesSlider.setPaintLabels(true);
        nodesSlider.setSnapToTicks(true);
        nodesSlider.setName("NodesSlider");
        JLabel NodesLabel = new JLabel("V");

        JCheckBox randomizedPlacementCheckBox = new JCheckBox("Randomized placement", randomizedPlacement);
        randomizedPlacementCheckBox.addItemListener(e -> {
            randomizedPlacement = e.getStateChange() == SELECTED;
            Graph<Vertex, DefaultWeightedEdge> generatedGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
            drawGraph(generatedGraph);
        });

        JCheckBox showEdgeWeightCheckbox = new JCheckBox("W", showEdgeWeight);
        showEdgeWeightCheckbox.setToolTipText("Enable to show edge weight");
        showEdgeWeightCheckbox.addItemListener(e -> {
            showEdgeWeight = e.getStateChange() == SELECTED;
            drawGraph(this.graph);
        });

        JCheckBox isConservativeCheckbox = new JCheckBox("C", isConservative);
        isConservativeCheckbox.setToolTipText("Enable to use conservative algorithm");
        isConservativeCheckbox.addItemListener(e -> isConservative = e.getStateChange() == SELECTED);

        JButton reloadButton = new JButton("Reload");
        reloadButton.setToolTipText("Reload current graph with new edge weights");
        reloadButton.addActionListener(e -> {
            Graph<Vertex, DefaultWeightedEdge> generateGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
            drawGraph(generateGraph);
        });

        JSpinner maxCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        maxCentersSpinner.setToolTipText("Set the maximum number of assignable centers");
        ((JSpinner.DefaultEditor) maxCentersSpinner.getEditor()).getTextField().setEditable(false);
        maxCentersSpinner.addChangeListener(e -> this.maxCentersValue = (int) maxCentersSpinner.getValue());
        JLabel maxCentersLabel = new JLabel("K");

        JSpinner maxClientsPerCenterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxClientsPerCenter, 1));
        maxClientsPerCenterSpinner.setToolTipText("Set the maximum number of clients assignable to a center");
        ((JSpinner.DefaultEditor) maxClientsPerCenterSpinner.getEditor()).getTextField().setEditable(false);
        maxClientsPerCenterSpinner.addChangeListener(e -> this.maxClientsPerCentersValue = (int) maxClientsPerCenterSpinner.getValue());
        JLabel maxClientsPerCenterLabel = new JLabel("L");

        JSpinner maxFailedCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        maxFailedCentersSpinner.setToolTipText("Set the maximum number of centers that could fail");
        ((JSpinner.DefaultEditor) maxFailedCentersSpinner.getEditor()).getTextField().setEditable(false);
        maxFailedCentersSpinner.addChangeListener(e -> this.maxFailedCentersValue = (int) maxFailedCentersSpinner.getValue());
        JLabel maxFailedCentersLabel = new JLabel("α");

        JButton executeMainAlgorithmButton = new JButton("Start");
        executeMainAlgorithmButton.setToolTipText("Start the algorithm");
        executeMainAlgorithmButton.addActionListener(e -> executeMainAlgorithm());

        ChangeListener optionsChangeListener = e -> {
            JSlider slider = (JSlider) e.getSource();
            if (!slider.getValueIsAdjusting()) {
                Graph<Vertex, DefaultWeightedEdge> generatedGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
                drawGraph(generatedGraph);
                descriptionLabel.setText("Vertices: " + generatedGraph.vertexSet().size() + ", Edges: " + generatedGraph.edgeSet().size());
            }
        };
//        centerSlider.addChangeListener(optionsChangeListener);
        nodesSlider.addChangeListener(optionsChangeListener);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new FlowLayout());
        optionsPanel.setSize(sliderPanelWidth, sliderPanelHeight);
//        optionsPanel.add(centersLabel);
//        optionsPanel.add(centerSlider);
        optionsPanel.add(showEdgeWeightCheckbox);
        optionsPanel.add(NodesLabel);
        optionsPanel.add(nodesSlider);
//        optionsPanel.add(randomizedPlacementCheckBox);
        optionsPanel.add(reloadButton);
        optionsPanel.add(maxCentersLabel);
        optionsPanel.add(maxCentersSpinner);
        optionsPanel.add(maxClientsPerCenterLabel);
        optionsPanel.add(maxClientsPerCenterSpinner);
        optionsPanel.add(maxFailedCentersLabel);
        optionsPanel.add(maxFailedCentersSpinner);
        optionsPanel.add(isConservativeCheckbox);
        optionsPanel.add(executeMainAlgorithmButton);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.add(descriptionLabel);

        this.frame.add(optionsPanel, BorderLayout.SOUTH);
        this.frame.add(descriptionPanel, BorderLayout.NORTH);

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
                this.maxFailedCentersValue,
                this.isConservative);

        System.out.println("\nSTART DRAWING RESULT\n");
        if (result != null) {
            drawSubGraphs(result);
        } else {
            System.out.println("NOT SOLVABLE");
        }
    }

    private void drawSubGraphs(Result result) {

        List<Graph<Vertex, DefaultWeightedEdge>> graphsToDraw = result.getGraphsToDraw();
        List<String> descriptions = result.getDescriptions();

        ActionListener drawSubGraphsListener = new ActionListener() {
			int graphIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                Timer sourceTimer = (Timer) e.getSource();
                if (graphIndex == result.getGraphsToDraw().size()) {
                    System.out.println("\tCenters drawn: " + getCentersCount(graphsToDraw.get(graphIndex - 1)));
                    System.out.println("\nEND DRAWING RESULT\n");
                    sourceTimer.stop();
					resetToOriginal(result.getOriginalGraph());
                } else {
                    Graph<Vertex, DefaultWeightedEdge> graphToDraw = graphsToDraw.get(graphIndex);
                    int vertexCount = graphToDraw.vertexSet().size();
                    int edgeCount = graphToDraw.edgeSet().size();
                    descriptionLabel.setText(descriptions.get(graphIndex));
                    System.out.println("\t(" + (graphsToDraw.size()) + "/" + (graphIndex + 1) + ") Drawing " + descriptions.get(graphIndex) + " with " + vertexCount + " vertices and " + edgeCount + " edges...");
                    drawGraph(graphToDraw);
					++graphIndex;
                }
            }
        };
        Timer drawSubGraphsTimer = new Timer(timerDelay, drawSubGraphsListener);
        drawSubGraphsTimer.setInitialDelay(0);
        drawSubGraphsTimer.start();
    }

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
