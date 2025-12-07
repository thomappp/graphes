import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Theme2Solver {
    private final Graph graph;
    private final long depotId;
    private final List<CollectionPoint> points;
    private static final int DIST_MATRIX_THRESHOLD = 100;
    private static final int MAX_TRUCK_CAPACITY = 20; // m3
    private final Map<Long, Integer> volumeByNode;

    public Theme2Solver(Graph graph, long depotId, List<CollectionPoint> points) {
        this.graph = graph;
        this.depotId = depotId;
        this.points = points;
        this.volumeByNode = buildVolumeMap(points);
    }

    public static class TourResult {
        private final List<Long> order;
        private final List<List<Long>> legs;
        private final double distance;
        private final boolean feasible;
        private final String method;

        public TourResult(List<Long> order, List<List<Long>> legs, double distance, boolean feasible, String method) {
            this.order = order;
            this.legs = legs;
            this.distance = distance;
            this.feasible = feasible;
            this.method = method;
        }

        public List<Long> getOrder() {
            return order;
        }

        public List<List<Long>> getLegs() {
            return legs;
        }

        public double getDistance() {
            return distance;
        }

        public boolean isFeasible() {
            return feasible;
        }

        public String getMethod() {
            return method;
        }
    }

    // Heuristique plus proche voisin sur les sommets de collecte
    public TourResult solveNearestNeighbor() {

        if (points.isEmpty()) { // Gérer le cas sans collecte
            return new TourResult(Collections.singletonList(depotId), new ArrayList<>(), 0.0, true, "Voisin le plus proche");
        }
        // Créer l’ensemble des sommets de collecte
        Set<Long> remaining = new HashSet<>();
        for (CollectionPoint cp : points) {
            remaining.add(cp.getNearestNodeId()); // Ajouter chaque sommet associé au point
        }
        List<Long> order = new ArrayList<>(); // Stocker l’ordre des visites
        List<List<Long>> legs = new ArrayList<>(); // Stocker les chemins Dijkstra
        long current = depotId; // Commencer depuis le dépôt
        double total = 0.0;

        while (!remaining.isEmpty()) { // Boucler tant qu’il reste des points
            // Selection du candidat le plus proche depuis la position courante
            double best = Double.POSITIVE_INFINITY; // Distance minimale actuelle
            long bestNode = -1; // Meilleur candidat
            Dijkstra.PathResult bestPath = null; // Chemin associé

            // Parcourir tous les sommets restants
            for (long candidate : remaining) {
                // Calculer le plus court chemin candidat <- current
                Dijkstra.PathResult path = Dijkstra.shortestPath(graph, current, candidate);
                if (!path.isReachable()) {
                    continue; // Ignorer les sommets inatteignables
                }
                if (path.getDistance() < best) { // Enregistrer le meilleur
                    best = path.getDistance();
                    bestNode = candidate;
                    bestPath = path;
                }
            }
            if (bestNode == -1) { // Si aucun candidat valide, arrêter
                return new TourResult(new ArrayList<>(), new ArrayList<>(), Double.POSITIVE_INFINITY, false, "Voisin le plus proche");
            }
            // Ajouter le point choisi
            order.add(bestNode);
            legs.add(bestPath.getPath());
            total += bestPath.getDistance();
            current = bestNode; // Avancer
            remaining.remove(bestNode); // Retirer ce point de la liste
        }

        // Retourner au dépôt
        Dijkstra.PathResult back = Dijkstra.shortestPath(graph, current, depotId);
        if (!back.isReachable()) {
            return new TourResult(new ArrayList<>(), new ArrayList<>(), Double.POSITIVE_INFINITY, false, "Voisin le plus proche");
        }
        total += back.getDistance();
        legs.add(back.getPath());

        // Construire l’ordre final (départ et retour)
        List<Long> fullOrder = new ArrayList<>();
        fullOrder.add(depotId);
        fullOrder.addAll(order);
        fullOrder.add(depotId);

        return new TourResult(fullOrder, legs, total, true, "Voisin le plus proche");
    }

    public TourResult solveMSTApprox() {
        if (points.isEmpty()) { // Gérer le cas vide
            return new TourResult(Collections.singletonList(depotId), new ArrayList<>(), 0.0, true, "MST DFS");
        }
        List<Long> ids = new ArrayList<>(); // Créer la liste des sommets (dépot + points)
        ids.add(depotId);
        for (CollectionPoint cp : points) {
            ids.add(cp.getNearestNodeId()); // Ajouter les sommets de collecte
        }
        int n = ids.size();
        if (n > DIST_MATRIX_THRESHOLD) {
            System.out.println("Avertissement : " + n + " points a parcourir, la matrice NxN sera couteuse.");
        }

        double[][] dist = new double[n][n]; // Créer la matrice des distances
        Map<String, Dijkstra.PathResult> paths = new HashMap<>(); // Sauvegarder les chemins
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Calculer chemin i vers j
                Dijkstra.PathResult pr = Dijkstra.shortestPath(graph, ids.get(i), ids.get(j));
                if (!pr.isReachable()) {
                    return new TourResult(new ArrayList<>(), new ArrayList<>(), Double.POSITIVE_INFINITY, false, "MST DFS");
                }
                // Stocker la distance symétrique
                dist[i][j] = pr.getDistance();
                dist[j][i] = pr.getDistance();
                paths.put(key(ids.get(i), ids.get(j)), pr); // Stocker le chemin dans la map
            }
        }

        List<Integer> mstAdj[] = buildMST(dist); // Construire l’arbre couvrant (MST)
        if (mstAdj == null) {
            return new TourResult(new ArrayList<>(), new ArrayList<>(), Double.POSITIVE_INFINITY, false, "MST DFS");
        }

        // Faire un DFS sur le MST pour créer une tournée
        List<Long> visitOrder = new ArrayList<>();
        boolean[] visited = new boolean[n];
        dfs(0, mstAdj, ids, visited, visitOrder); // Parcourir tous les sommets
        visitOrder.add(depotId); // Fermer la boucle (retour au dépôt)

        double total = 0.0; // Calculer la distance totale
        List<List<Long>> legs = new ArrayList<>();
        for (int i = 0; i < visitOrder.size() - 1; i++) {
            long a = visitOrder.get(i);
            long b = visitOrder.get(i + 1);
            Dijkstra.PathResult pr = paths.get(key(a, b));
            total += pr.getDistance();
            legs.add(pr.getPath());
        }

        return new TourResult(visitOrder, legs, total, true, "MST DFS");
    }

    private Map<Long, Integer> buildVolumeMap(List<CollectionPoint> pts) {
        Map<Long, Integer> map = new HashMap<>(); // Créer la map volumes
        for (CollectionPoint cp : pts) { // Parcourir les points
            map.put(cp.getNearestNodeId(), cp.getVolume()); // Associer sommet -> volume
        }
        return map; // Retourner la map
    }

    public List<SplitTour> splitTourIntoSubTours(List<Long> order) {
        List<SplitTour> tours = new ArrayList<>(); // Stocker les sous-tournées
        if (order.size() < 2) { // Vérifier liste valide
            return tours; // Retourner vide si trop court
        }
        List<Long> current = new ArrayList<>(); // Créer la tournée courante
        current.add(depotId); // Ajouter le dépôt au début
        int load = 0; // Suivre la charge du camion
        double currentDist = 0.0; // Suivre la distance du segment

        // Parcourir les sommets intermédiaires
        for (int i = 1; i < order.size() - 1; i++) { // Ignorer le dépôt final
            long point = order.get(i); // Récupérer le sommet cible
            int vol = volumeByNode.getOrDefault(point, 1); // Récupérer le volume
            long prev = current.get(current.size() - 1); // Récupérer le sommet précédent
            Dijkstra.PathResult leg = Dijkstra.shortestPath(graph, prev, point); // Calculer la distance
            if (!leg.isReachable()) { // Vérifier accessibilité
                return Collections.emptyList(); // Abandonner si non atteignable
            }
            if (load + vol <= MAX_TRUCK_CAPACITY) { // Vérifier la capacité avant d'ajouter le sommet
                current.add(point); // Ajouter le sommet à la tournée
                load += vol; // Ajouter le volume
                currentDist += leg.getDistance(); // Ajouter la distance de l’arête
            } else {
                // Arrêter la tournée courante et retour au dépot
                Dijkstra.PathResult back = Dijkstra.shortestPath(graph, prev, depotId);
                if (!back.isReachable()) { // Vérifier accessibilité
                    return Collections.emptyList();
                }
                current.add(depotId); // Ajouter le dépôt
                currentDist += back.getDistance(); // Ajouter la distance retour
                tours.add(new SplitTour(current, load, currentDist)); // Enregistrer la tournée

                // Démarrer une nouvelle tournée
                current = new ArrayList<>(); // Réinitialiser la tournée
                current.add(depotId); // Recommencer depuis le dépôt
                current.add(point); // ajouter le premier sommet
                load = vol; // Réinitialiser le volume
                Dijkstra.PathResult startLeg = Dijkstra.shortestPath(graph, depotId, point); // Calculer distance
                if (!startLeg.isReachable()) { // Vérifier accessibilité
                    return Collections.emptyList(); // Réinitialiser la distance
                }
                currentDist = startLeg.getDistance();
            }
        }
        long last = current.get(current.size() - 1); // Récupérer le dernier sommet
        Dijkstra.PathResult backHome = Dijkstra.shortestPath(graph, last, depotId); // Calculer retour
        if (!backHome.isReachable()) { // Vérifier accessibilité
            return Collections.emptyList();
        }
        current.add(depotId); // Ajouter retour au dépôt
        currentDist += backHome.getDistance(); // Ajouter la distance
        tours.add(new SplitTour(current, load, currentDist)); // Enregistrer la tournée finale
        return tours;
    }

    public void runGreedyOnly() {
        TourResult nn = solveNearestNeighbor(); // Exécuter l'heuristique voisin le plus proche
        if (!nn.isFeasible()) { // Vérifier faisabilité
            System.out.println("Approche Plus Proche Voisin impossible (graphe non connexe ?)");
            return;
        }
        printTourWithSplits(nn);
    }

    public void runMSTOnly() {
        TourResult mst = solveMSTApprox(); // Exécuter l'heuristique MST
        if (!mst.isFeasible()) { // Vérifier faisabilité
            System.out.println("Approche MST impossible (graphe non connexe ?)");
            return;
        }
        printTourWithSplits(mst);
    }

    public void runComparison() {
        TourResult nn = solveNearestNeighbor();  // Lancer l'algo Plus Proche Voisin
        TourResult mst = solveMSTApprox(); // Lancer l'algo MST

        if (!nn.isFeasible()) { // Vérifier si Greedy possible
            System.out.println("Approche Plus Proche Voisin impossible (graphe non connexe ?)");
        } else {
            System.out.printf("Distance totale (Greedy) : %.2f m%n", nn.getDistance());
        }
        if (!mst.isFeasible()) { // vérifier si MST possible
            System.out.println("Approche MST impossible (graphe non connexe ?)");
        } else {
            System.out.printf("Distance totale (MST) : %.2f m%n", mst.getDistance());
        }

        TourResult best = null; // Stocker la meilleure tournée
        if (nn.isFeasible() && mst.isFeasible()) { // Si les deux tournées existent
            best = nn.getDistance() <= mst.getDistance() ? nn : mst; // Comparer distances
            // Calculer écart
            double base = nn.getDistance();
            double compare = mst.getDistance();
            double diffPercent = ((compare - base) / base) * 100.0;
            System.out.printf("Gain/Perte (MST vs Greedy) : %.2f %% %n", diffPercent);
        } else if (nn.isFeasible()) { // Si seulement Greedy existe
            best = nn;
        } else if (mst.isFeasible()) { // Si seulement MST existe
            best = mst;
        }

        // Afficher la meilleure tournée
        if (best != null && best.isFeasible()) {
            System.out.println("Meilleure tournée : " + best.getMethod());
            printTourWithSplits(best);
        } else {
            System.out.println("Aucune tournée trouvable.");
        }
    }

    public void printTourWithSplits(TourResult tr) {
        System.out.println("Tournée des points de collectes (" + tr.getMethod() + ")");
        List<SplitTour> splits = splitTourIntoSubTours(tr.getOrder()); // Découper la tournée
        if (splits.isEmpty()) { // Vérifier si découpage possible
            System.out.println("  Impossible de decouper la tournée (graphe non connexe ?)");
            return;
        }
        double total = 0.0;
        for (int i = 0; i < splits.size(); i++) { // Parcourir les sous-tournées
            SplitTour st = splits.get(i);
            total += st.distance; // Cumul des distances
            System.out.print("  Tournée " + (i + 1) + " (" + st.usedVolume + "/" + MAX_TRUCK_CAPACITY + " m3) : ");
            System.out.println(st.path);
            System.out.printf("    Distance : %.2f m%n", st.distance);
        }
        System.out.printf("Distance totale cumulée : %.2f m%n", total);
    }

    public List<Node> buildStreetPath(Graph graph, List<Long> pathIds) {
        List<Node> result = new ArrayList<>(); // Liste résultat
        if (pathIds == null || pathIds.size() < 2) {
            return result; // Vérifier liste trop petite
        }

        // Parcourir les paires de sommets
        for (int i = 0; i < pathIds.size() - 1; i++) {
            long a = pathIds.get(i);
            long b = pathIds.get(i + 1);
            Dijkstra.PathResult pr = Dijkstra.shortestPath(graph, a, b); // Calculer plus court chemin
            List<Long> ids = pr.getPath();
            if (ids.isEmpty()) { // Ignorer si vide
                continue;
            }

            for (int j = 0; j < ids.size(); j++) { // Ajouter les points du chemin
                // Eviter doublons
                if (i > 0 && j == 0) {
                    continue;
                }
                Node n = graph.getNode(ids.get(j)); // récupérer le sommet
                if (n != null) { // Vérifier null
                    result.add(n);
                }
            }
        }
        return result;
    }

    public static class SplitTour {
        public final List<Long> path;
        public final int usedVolume;
        public final double distance;

        public SplitTour(List<Long> path, int usedVolume, double distance) {
            this.path = path;
            this.usedVolume = usedVolume;
            this.distance = distance;
        }
    }

    private List<Integer>[] buildMST(double[][] dist) {
        int n = dist.length;
        double[] key = new double[n];
        int[] parent = new int[n];
        boolean[] inMST = new boolean[n];
        for (int i = 0; i < n; i++) {
            key[i] = Double.POSITIVE_INFINITY;
            parent[i] = -1;
        }
        key[0] = 0.0;

        for (int count = 0; count < n - 1; count++) {
            int u = minKey(key, inMST);
            if (u == -1) {
                return null;
            }
            inMST[u] = true;
            for (int v = 0; v < n; v++) {
                if (!inMST[v] && dist[u][v] < key[v]) {
                    parent[v] = u;
                    key[v] = dist[u][v];
                }
            }
        }

        List<Integer>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
        }
        for (int v = 1; v < n; v++) {
            int p = parent[v];
            if (p >= 0) {
                adj[p].add(v);
                adj[v].add(p);
            }
        }
        return adj;
    }

    private int minKey(double[] key, boolean[] inMST) {
        double min = Double.POSITIVE_INFINITY;
        int idx = -1;
        for (int v = 0; v < key.length; v++) {
            if (!inMST[v] && key[v] < min) {
                min = key[v];
                idx = v;
            }
        }
        return idx;
    }

    private void dfs(int u, List<Integer>[] adj, List<Long> ids, boolean[] visited, List<Long> order) {
        visited[u] = true;
        order.add(ids.get(u));
        for (int v : adj[u]) {
            if (!visited[v]) {
                dfs(v, adj, ids, visited, order);
            }
        }
    }

    private String key(long a, long b) {
        if (a < b) {
            return a + "-" + b;
        } else {
            return b + "-" + a;
        }
    }
}