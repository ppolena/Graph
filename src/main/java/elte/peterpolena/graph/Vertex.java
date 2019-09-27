package elte.peterpolena.graph;

import java.awt.*;
import java.util.Objects;

public class Vertex {

    private final int x;
    private final int y;
    private final Color color;

    public Vertex(int x, int y, Color color){
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "x=" + x +
                ", y=" + y +
                ", color=" + color +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return x == vertex.x &&
                y == vertex.y &&
                color.equals(vertex.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, color);
    }
}
