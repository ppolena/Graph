package elte.peterpolena.graph;

import org.jgrapht.graph.DefaultWeightedEdge;

public class WeightedEdgeWithCapacity extends DefaultWeightedEdge {

    private int capacity;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
