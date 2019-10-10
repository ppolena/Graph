package elte.peterpolena.graph;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vertex {

    private final int x;
    private final int y;
    private Color color;
    private List<Vertex> empire;
    private Vertex parent;
    private List<Vertex> children;
    private boolean isMarked;
    private boolean isMonarch;
    private Vertex deputy;

    public Vertex(int x, int y, Color color){
        this.x = x;
        this.y = y;
        this.color = color;
        this.empire = new ArrayList<>();
        this.children = new ArrayList<>();
        this.isMarked = false;
        this.isMonarch = false;
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

    public Vertex getParent() {
        return parent;
    }

    public List<Vertex> getChildren() {
        return children;
    }

    public List<Vertex> getEmpire() {
        return empire;
    }

    public boolean isMarked() {
        return isMarked;
    }

    public boolean isMonarch() {
        return isMonarch;
    }

    public Vertex getDeputy() {
        return deputy;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setEmpire(List<Vertex> empire) {
        this.empire = empire;
    }

    public void addToEmpire(Vertex vertex) {
        this.empire.add(vertex);
    }

    public void setParent(Vertex parent) {
        this.parent = parent;
    }

    public void addChild(Vertex child) {
        this.children.add(child);
    }

    public void setMarked() {
        this.isMarked = true;
    }

    public void setMonarch() {
        this.isMonarch = true;
    }

    public void setDeputy(Vertex deputy) {
        this.deputy = deputy;
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
