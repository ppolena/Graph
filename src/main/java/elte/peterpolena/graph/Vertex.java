package elte.peterpolena.graph;

import java.awt.*;
import java.util.List;
import java.util.*;

public class Vertex {

/*
vertex.getEmpire() = Emp(v)
vertex.getClients() = dom(v)
vertex.getCenter() = ctr(v)
 */

    private final int x;
    private final int y;
    private Color color;
    private List<Vertex> empire;
    private Vertex parent;
    private List<Vertex> children;
    private Vertex major;
    private List<Vertex> minors;
    private Set<Vertex> clients;
    private Vertex center;
    private boolean isMarked;
    private boolean isMonarch;
    private Vertex deputy;
    private Set<Vertex> backupCenters;

    public Vertex(int x, int y, Color color){
        this.x = x;
        this.y = y;
        this.color = color;
        this.empire = new ArrayList<>();
        this.children = new ArrayList<>();
        this.minors = new ArrayList<>();
        this.clients = new HashSet<>();
        this.isMarked = false;
        this.isMonarch = false;
        this.backupCenters = new HashSet<>();
    }

    public Vertex(Vertex vertex) {
        this.x = vertex.getX();
        this.y = vertex.getY();
        this.color = vertex.getColor();
        this.empire = new ArrayList<>();
        this.children = new ArrayList<>();
        this.minors = new ArrayList<>();
        this.clients = new HashSet<>();
        this.isMarked = false;
        this.isMonarch = false;
        this.backupCenters = new HashSet<>();
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

    public Vertex getMajor() {
        return major;
    }

    public void setMajor(Vertex major) {
        this.major = major;
    }

    public List<Vertex> getMinors() {
        return minors;
    }

    public void addMinor(Vertex minor) {
        this.minors.add(minor);
    }

    public void addMinors(Collection<Vertex> minors) {
        this.minors.addAll(minors);
    }

    public Set<Vertex> getClients() {
        return clients;
    }

    public void addClient(Vertex client) {
        this.clients.add(client);
    }

    public void addClients(Set<Vertex> clients) {
        this.clients.addAll(clients);
    }

    public void setClients(Set<Vertex> clients) {
        this.clients = clients;
    }

    public Vertex getCenter() {
        return center;
    }

    public Set<Vertex> getBackupCenters() {
        return backupCenters;
    }

    public void clearData() {
        center = null;
        clients.clear();
        minors.clear();
        major = null;
        deputy = null;
        isMonarch = false;
        isMarked = false;
        empire.clear();
        parent = null;
        children.clear();
        backupCenters.clear();
        setColor(Color.BLACK);
    }

    public void setCenter(Vertex center) {
        this.center = center;
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
                y == vertex.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
