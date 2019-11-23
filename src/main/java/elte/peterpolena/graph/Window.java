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
import static elte.peterpolena.graph.Utils.copy;
import static elte.peterpolena.graph.Utils.getCentersCount;
import static java.awt.event.ItemEvent.SELECTED;

//import static elte.peterpolena.graph.Config.timerDelay;

@Service
public class Window {

    private JFrame frame;
    private Graph<Vertex, DefaultWeightedEdge> graph;
    private GraphPainter graphPainter;
    private boolean randomizedPlacement = false;
    private boolean showEdgeWeight = true;
    private boolean isConservative = false;
	private boolean auto = false;
    private int maxCentersValue;
    private int maxClientsPerCentersValue;
    private int maxFailedCentersValue;
	private int timerDelay;
	private JLabel descriptionLabel;
	private JSlider nodesSlider;
	private JCheckBox randomizedPlacementCheckBox;
	private JCheckBox showEdgeWeightCheckbox;
	private JCheckBox isConservativeCheckbox;
	private JCheckBox autoCheckbox;
	private JButton reloadButton;
	private JSpinner maxCentersSpinner;
	private JSpinner maxClientsPerCenterSpinner;
	private JSpinner maxFailedCentersSpinner;
	private JSpinner timerDelaySpinner;
	private JButton executeMainAlgorithmButton;
	private JButton showPreviousPartialResult;
	private JButton showNextPartialResult;
	private JButton end;
	private Timer drawSubGraphsTimer;
	private Result result;

	public Window() {

		frame = new JFrame("Graph");
		frame.setSize(frameWidth, frameHeight);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		graphPainter = new GraphPainter(graph, showEdgeWeight);

		descriptionLabel = new JLabel("Vertices: " + graph.vertexSet().size() + ", Edges: " + graph.edgeSet().size());

		maxCentersValue = 1;
		maxClientsPerCentersValue = 1;
		maxFailedCentersValue = 2;

        GraphGenerator graphGenerator = new GraphGenerator();

//        JSlider centerSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, centersSliderStartValue);
//        centerSlider.setMinorTickSpacing(1);
//        centerSlider.setMajorTickSpacing(1);
//        centerSlider.setPaintTicks(true);
//        centerSlider.setPaintLabels(true);
//        centerSlider.setSnapToTicks(true);
//        centerSlider.setName("CentersSlider");
//        JLabel centersLabel = new JLabel("Centers");

		nodesSlider = new JSlider(SwingConstants.HORIZONTAL, sliderMinValue, sliderMaxValue, clientsSliderStartValue);
        nodesSlider.setToolTipText("Set the number of vertices");
        nodesSlider.setMinorTickSpacing(1);
        nodesSlider.setMajorTickSpacing(1);
        nodesSlider.setPaintTicks(true);
        nodesSlider.setPaintLabels(true);
        nodesSlider.setSnapToTicks(true);
        nodesSlider.setName("NodesSlider");
        JLabel NodesLabel = new JLabel("V");

		randomizedPlacementCheckBox = new JCheckBox("Randomized placement", randomizedPlacement);
        randomizedPlacementCheckBox.addItemListener(e -> {
            randomizedPlacement = e.getStateChange() == SELECTED;
            Graph<Vertex, DefaultWeightedEdge> generatedGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
            drawGraph(generatedGraph);
        });

		showEdgeWeightCheckbox = new JCheckBox("W", showEdgeWeight);
        showEdgeWeightCheckbox.setToolTipText("Enable to show edge weight");
        showEdgeWeightCheckbox.addItemListener(e -> {
            showEdgeWeight = e.getStateChange() == SELECTED;
			drawGraph(graph);
        });

		isConservativeCheckbox = new JCheckBox("C", isConservative);
        isConservativeCheckbox.setToolTipText("Enable to use conservative algorithm");
        isConservativeCheckbox.addItemListener(e -> isConservative = e.getStateChange() == SELECTED);

		autoCheckbox = new JCheckBox("Auto", auto);
		autoCheckbox.setToolTipText("Enable to automate algorithm result drawing");
		autoCheckbox.addItemListener(e -> auto = e.getStateChange() == SELECTED);

		reloadButton = new JButton("Reload");
        reloadButton.setToolTipText("Reload current graph with new edge weights");
        reloadButton.addActionListener(e -> {
			Graph<Vertex, DefaultWeightedEdge> generatedGraph = graphGenerator.generate(0, nodesSlider.getValue(), randomizedPlacement);
			drawGraph(generatedGraph);
			descriptionLabel.setText("Vertices: " + generatedGraph.vertexSet().size() + ", Edges: " + generatedGraph.edgeSet().size());
        });

		maxCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        maxCentersSpinner.setToolTipText("Set the maximum number of assignable centers");
        ((JSpinner.DefaultEditor) maxCentersSpinner.getEditor()).getTextField().setEditable(false);
		maxCentersSpinner.addChangeListener(e -> maxCentersValue = (int) maxCentersSpinner.getValue());
        JLabel maxCentersLabel = new JLabel("K");

		maxClientsPerCenterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxClientsPerCenter, 1));
        maxClientsPerCenterSpinner.setToolTipText("Set the maximum number of clients assignable to a center");
        ((JSpinner.DefaultEditor) maxClientsPerCenterSpinner.getEditor()).getTextField().setEditable(false);
		maxClientsPerCenterSpinner.addChangeListener(e -> maxClientsPerCentersValue = (int) maxClientsPerCenterSpinner.getValue());
        JLabel maxClientsPerCenterLabel = new JLabel("L");

		maxFailedCentersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxCenters, 1));
        maxFailedCentersSpinner.setToolTipText("Set the maximum number of centers that could fail");
        ((JSpinner.DefaultEditor) maxFailedCentersSpinner.getEditor()).getTextField().setEditable(false);
		maxFailedCentersSpinner.addChangeListener(e -> maxFailedCentersValue = (int) maxFailedCentersSpinner.getValue());
        JLabel maxFailedCentersLabel = new JLabel("α");

		timerDelaySpinner = new JSpinner(new SpinnerNumberModel(500, 500, 10000, 500));
		timerDelaySpinner.setToolTipText("Set the delay in ms between displaying intermediate results");
		((JSpinner.DefaultEditor) timerDelaySpinner.getEditor()).getTextField().setEditable(false);
		timerDelaySpinner.addChangeListener(e -> timerDelay = (int) timerDelaySpinner.getValue());
		JLabel timerDelayLabel = new JLabel("Delay");

		executeMainAlgorithmButton = new JButton("Start");
        executeMainAlgorithmButton.setToolTipText("Start the algorithm");
        executeMainAlgorithmButton.addActionListener(e -> executeMainAlgorithm());

		showPreviousPartialResult = new JButton("<");
		showPreviousPartialResult.setToolTipText("Show previous partial result");
		showPreviousPartialResult.setEnabled(false);

		showNextPartialResult = new JButton(">");
		showNextPartialResult.setToolTipText("Show next partial result");
		showNextPartialResult.setEnabled(false);

		end = new JButton("END");
		end.setToolTipText("Finish showing partial results and jump to end result");
		end.addActionListener(e -> {
			if (auto) {
				endAutoDisplay();
			}
		});
		end.setEnabled(false);

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
		optionsPanel.add(autoCheckbox);
		optionsPanel.add(timerDelayLabel);
		optionsPanel.add(timerDelaySpinner);
        optionsPanel.add(executeMainAlgorithmButton);
		optionsPanel.add(showPreviousPartialResult);
		optionsPanel.add(showNextPartialResult);
		optionsPanel.add(end);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.add(descriptionLabel);

		frame.add(optionsPanel, BorderLayout.SOUTH);
		frame.add(descriptionPanel, BorderLayout.NORTH);

		frame.validate();
		frame.repaint();
		frame.setVisible(true);
    }

	private void executeMainAlgorithm() {

		drawGraph(graph);

		result = new AlgorithmService().mainAlgorithm(
				graph,
				maxCentersValue,
				maxClientsPerCentersValue,
				maxFailedCentersValue,
				isConservative);

		if (result != null && auto) {
			setEnableOptions(false);
			end.setEnabled(true);
			drawSubGraphs(result);
		} else if (result != null) {

		} else {
			System.out.println("NOT SOLVABLE");
		}
    }

	private void setEnableOptions(boolean enable) {
		nodesSlider.setEnabled(enable);
		randomizedPlacementCheckBox.setEnabled(enable);
		showEdgeWeightCheckbox.setEnabled(enable);
		isConservativeCheckbox.setEnabled(enable);
		autoCheckbox.setEnabled(enable);
		reloadButton.setEnabled(enable);
		maxCentersSpinner.setEnabled(enable);
		maxClientsPerCenterSpinner.setEnabled(enable);
		maxFailedCentersSpinner.setEnabled(enable);
		timerDelaySpinner.setEnabled(enable);
		executeMainAlgorithmButton.setEnabled(enable);
	}

    private void drawSubGraphs(Result result) {

		System.out.println("\nSTART DRAWING RESULT\n");
    	
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
					setEnableOptions(true);
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
		drawSubGraphsTimer = new Timer(timerDelay, drawSubGraphsListener);
        drawSubGraphsTimer.setInitialDelay(0);
        drawSubGraphsTimer.start();
    }

	private void endAutoDisplay() {
		drawSubGraphsTimer.stop();
		setEnableOptions(true);
		Graph<Vertex, DefaultWeightedEdge> endResult = result.getGraphsToDraw().get(result.getGraphsToDraw().size() - 1);
		drawGraph(endResult);
		graph = copy(result.getOriginalGraph());
		System.out.println("\tJumping to end result...");
		System.out.println("\tCenters drawn: " + getCentersCount(endResult));
		System.out.println("\nEND DRAWING RESULT\n");
		end.setEnabled(false);
	}

    private void drawGraph(Graph<Vertex, DefaultWeightedEdge> graph) {
		frame.remove(graphPainter);
		frame.validate();
		frame.repaint();

        this.graph = copy(graph);
		graphPainter = new GraphPainter(this.graph, showEdgeWeight);

		frame.add(graphPainter, BorderLayout.CENTER);
		frame.validate();
		frame.repaint();
    }

    private void resetToOriginal(Graph<Vertex, DefaultWeightedEdge> graph) {
        this.graph = copy(graph);
    }
}
