package elte.peterpolena.graph;

import java.awt.*;

public class Config {

    public static final int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    public static final int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
    public static final int frameWidth = (int) Math.round(screenWidth * 0.66);
    public static final int frameHeight = (int) Math.round(screenHeight * 0.75);
    public static final int graphPanelWidth = frameWidth;
    public static final int graphPanelHeight = (int) Math.round(frameHeight * 0.75);
    public static final int sliderPanelWidth = frameWidth;
    public static final int sliderPanelHeight = (int) Math.round(screenHeight * 0.25);
    public static final int minXCoordinate = (int) Math.round(graphPanelWidth * 0.05);
    public static final int minYCoordinate = (int) Math.round(graphPanelHeight * 0.05);
    public static final int maxXCoordinate = (int) Math.round(graphPanelWidth - (graphPanelWidth * 0.1));
    public static final int maxYCoordinate = (int) Math.round(graphPanelHeight - (graphPanelHeight * 0.05));
    public static final int centerX = (int) Math.round(graphPanelWidth * 0.5);
    public static final int centerY = (int) Math.round(graphPanelHeight * 0.5);
    public static final double graphRadius = (int) Math.round(graphPanelHeight * 0.33);
    public static final int vertexRadius = (int) Math.round(graphRadius * 0.1);
    public static final int sliderMinValue = 0;
    public static final int sliderMaxValue = 10;
    public static final int centersSliderStartValue = 0;
    public static final int clientsSliderStartValue = 0;
    public static final int minWeight = 1;
    public static final int maxWeight = 10;
    public static final int maxCenters = 10;
    public static final int maxClientsPerCenter = 10;
    public static final int timerDelay = 2500;
}
