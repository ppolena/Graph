package elte.peterpolena.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public class MinCostMaxFlowService {
    private boolean found[];
    private int N, cap[][], flow[][], cost[][], dad[], dist[], pi[];

    private final int INF = Integer.MAX_VALUE / 2 - 1;

    private boolean search(int source, int sink) {
        Arrays.fill(found, false);
        Arrays.fill(dist, INF);
        dist[source] = 0;

        while (source != N) {
            int best = N;
            found[source] = true;
            for (int k = 0; k < N; k++) {
                if (found[k]) continue;
                if (flow[k][source] != 0) {
                    int val = dist[source] + pi[source] - pi[k] - cost[k][source];
                    if (dist[k] > val) {
                        dist[k] = val;
                        dad[k] = source;
                    }
                }
                if (flow[source][k] < cap[source][k]) {
                    int val = dist[source] + pi[source] - pi[k] + cost[source][k];
                    if (dist[k] > val) {
                        dist[k] = val;
                        dad[k] = source;
                    }
                }

                if (dist[k] < dist[best]) best = k;
            }
            source = best;
        }
        for (int k = 0; k < N; k++)
            pi[k] = Math.min(pi[k] + dist[k], INF);
        return found[sink];
    }


    private int[] getMaxFlow(int caps[][], int costs[][], int source, int sink) {
        cap = caps;
        cost = costs;

        N = cap.length;
        found = new boolean[N];
        flow = new int[N][N];
        dist = new int[N+1];
        dad = new int[N];
        pi = new int[N];

        int totflow = 0, totcost = 0;
        while (search(source, sink)) {
            int amt = INF;
            for (int x = sink; x != source; x = dad[x])
                amt = Math.min(amt, flow[x][dad[x]] != 0 ? flow[x][dad[x]] :
                        cap[dad[x]][x] - flow[dad[x]][x]);
            for (int x = sink; x != source; x = dad[x]) {
                if (flow[x][dad[x]] != 0) {
                    flow[x][dad[x]] -= amt;
                    totcost -= amt * cost[x][dad[x]];
                } else {
                    flow[dad[x]][x] += amt;
                    totcost += amt * cost[dad[x]][x];
                }
            }
            totflow += amt;
        }

        return new int[]{ totflow, totcost };
    }

    public Map<Vertex, Set<Vertex>> getFlow(Graph<Vertex, DefaultWeightedEdge> graph, Set<Vertex> monarchs, int maxClientsPerCenter) {
        Map<Vertex, Set<Vertex>> ret = new HashMap<>();
        N = graph.vertexSet().size() + monarchs.size() + 2;
        Map<Vertex, Integer> monarchIndexes = new HashMap<>();
        Integer index = 1;
        for(Vertex m : monarchs)
            monarchIndexes.put(m, index++);
        Map<Vertex, Integer> vertexIndexes = new HashMap<>();
        for(Vertex v : graph.vertexSet())
            vertexIndexes.put(v, index++);
        int cap[][] = new int[N][N];
        int cost[][] = new int[N][N];
        for(int i = 0; i < N; i++)
            for(int j = 0; j < N; j++)
            {
                cap[i][j] = 0;
                cost[i][j] = 0;
            }

        monarchIndexes.forEach((v, i) -> {
            Utils.getAdjacentVerticesUpToDistance(graph, v.getMajor(), 2).forEach(x -> {
                cap[i][vertexIndexes.get(x)] = 1;
                cost[i][vertexIndexes.get(x)] = v == x ? 0 : 1;
            });
        });

        //adding (s, m)
        monarchIndexes.values().forEach(i -> {
            cap[0][i] = maxClientsPerCenter;
            cost[0][i] = 1;
        });
        //adding (v, t)
        vertexIndexes.values().forEach(i -> {
            cap[i][N - 1] = 1;
            cost[i][N - 1] = 1;
        });
        getMaxFlow(cap, cost, 0, N - 1);

        monarchIndexes.forEach((m, i) ->
                {
                    Set<Vertex> flowsTo = new HashSet<>();
                    vertexIndexes.forEach((v, j) -> {
                        if(flow[i][j] != 0)
                            flowsTo.add(v);
                    });
                    ret.put(m, flowsTo);
                });
        return ret;
    }
}
