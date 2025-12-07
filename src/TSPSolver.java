import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TSPSolver {
    private final Graph graph; // Graphe sur lequel résoudre le TSP
    private static final int BRUTE_FORCE_THRESHOLD = 9; // Seuil de nombre de cibles pour passer du brute force à heuristique

    public TSPSolver(Graph graph) {
        this.graph = graph;
    }

    public static class TSPSolution {
        private final List<Long> order; // Ordre des sommets parcourus (start -> ... -> start)
        private final double distance; // Distance totale du tour
        private final boolean feasible; // Indique si le tour est réalisable
        private final List<List<Long>> legs; // Chemins détaillés entre les étapes

        public TSPSolution(List<Long> order, double distance, boolean feasible, List<List<Long>> legs) {
            this.order = order;
            this.distance = distance;
            this.feasible = feasible;
            this.legs = legs;
        }

        public List<Long> getOrder() {
            return order;
        }

        public double getDistance() {
            return distance;
        }

        public boolean isFeasible() {
            return feasible;
        }

        public List<List<Long>> getLegs() {
            return legs;
        }
    }

    // Résoudre le TSP à partir d'un sommet de départ et une liste de cibles
    public TSPSolution solve(long startId, List<Long> targets) {
        // Choix brute force si peu de cibles sinon heuristique plus proche voisin
        if (targets.isEmpty()) {
            List<Long> trivial = new ArrayList<>();
            trivial.add(startId);
            return new TSPSolution(trivial, 0.0, true, new ArrayList<>());
        }

        // Supprimer doublons et le sommet de départ des cibles
        List<Long> uniqueTargets = new ArrayList<>(new HashSet<>(targets));
        uniqueTargets.remove(startId);

        // Construire la liste complète des sommets à considérer
        List<Long> allNodes = new ArrayList<>();
        allNodes.add(startId);
        allNodes.addAll(uniqueTargets);

        // Calculer tous les plus courts chemins entre chaque paire
        Map<String, Dijkstra.PathResult> pairwise = computeAllPairs(allNodes);
        if (pairwise == null) {
            return new TSPSolution(new ArrayList<>(), Double.POSITIVE_INFINITY, false, new ArrayList<>());
        }

        // Choix de la méthode : brute force si peu de cibles, sinon heuristique
        if (uniqueTargets.size() <= BRUTE_FORCE_THRESHOLD) {
            return bruteForce(startId, uniqueTargets, pairwise);
        } else {
            return nearestNeighbor(startId, uniqueTargets, pairwise);
        }
    }

    // Calculer tous les plus courts chemins entre paires de sommets
    private Map<String, Dijkstra.PathResult> computeAllPairs(List<Long> nodes) {
        // Mémorisation des plus courts chemins pour éviter de recalculer
        Map<String, Dijkstra.PathResult> map = new HashMap<>();
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long a = nodes.get(i);
                long b = nodes.get(j);
                Dijkstra.PathResult result = Dijkstra.shortestPath(graph, a, b);
                if (!result.isReachable()) {
                    return null;
                }
                map.put(key(a, b), result);
            }
        }
        return map;
    }

    // Résolution brute force du TSP
    private TSPSolution bruteForce(long startId, List<Long> targets, Map<String, Dijkstra.PathResult> pairwise) {
        boolean[] used = new boolean[targets.size()];
        bestOrderBuffer = null; // Buffer pour stocker la meilleure permutation
        bruteForceRec(startId, targets, pairwise, used, new ArrayList<>(), Double.POSITIVE_INFINITY);
        List<Long> bestOrder = bestOrderBuffer;
        if (bestOrder == null) {
            return new TSPSolution(new ArrayList<>(), Double.POSITIVE_INFINITY, false, new ArrayList<>());
        }
        return buildSolution(startId, bestOrder, pairwise);
    }

    // Buffer pour mémoriser la meilleure permutation
    private List<Long> bestOrderBuffer = null;

    // Fonction récursive pour l'exploration brute force
    private double bruteForceRec(long startId,
                                 List<Long> targets,
                                 Map<String, Dijkstra.PathResult> pairwise,
                                 boolean[] used,
                                 List<Long> current,
                                 double bestDistance) {
        // Vérifier si l'on a placé autant de sommets que nécessaire (solution complète)
        // Dans ce cas, calculer la distance totale de la tournée construite
        if (current.size() == targets.size()) {
            double distance = routeDistance(startId, current, pairwise); // Calculer la distance complète du chemin (aller + retour au départ)
            // Mettre à jour la meilleure solution trouvée si celle-ci est meilleure
            if (distance < bestDistance) {
                bestDistance = distance;
                bestOrderBuffer = new ArrayList<>(current); // Sauvegarder l'ordre optimal actuel dans le buffer global
            }
            return bestDistance;
        }

        for (int i = 0; i < targets.size(); i++) { // Boucler sur tous les sommets possibles afin de générer toutes les permutations
            if (used[i]) { // Vérifier si le sommet a déjà été utilisé dans la permutation courante
                continue; // Éviter de réutiliser un sommet
            }
            used[i] = true; // Marquer le sommet comme utilisé
            current.add(targets.get(i)); // Ajouter ce sommet dans la permutation en cours

            // Calculer la distance partielle actuelle (bound simple)
            // Permet de couper les branches inutiles
            double optimistic = routeDistance(startId, current, pairwise);

            // Vérifier si la distance partielle reste prometteuse
            // Si elle dépasse déjà la meilleure distance connue, arrêter ici
            if (optimistic < bestDistance) {
                bestDistance = bruteForceRec(startId, targets, pairwise, used, current, bestDistance); // Explorer récursivement les choix suivants
            }
            current.remove(current.size() - 1); // Retirer le dernier sommet pour revenir dans l'état précédent (backtracking)
            used[i] = false; // Marquer ce sommet comme disponible de nouveau
        }
        return bestDistance;
    }
    // Heuristique du plus proche voisin
    // Construire un chemin en choisissant à chaque étape le sommet le plus proche
    private TSPSolution nearestNeighbor(long startId, List<Long> targets, Map<String, Dijkstra.PathResult> pairwise) {
        Set<Long> remaining = new HashSet<>(targets); // Initialiser l'ensemble des sommets encore à visiter
        List<Long> order = new ArrayList<>(); // Liste représentant l'ordre final d'exploration (sans startId)
        long current = startId; // Point de départ actuel
        double total = 0.0; // Distance totale accumulée de la tournée
        List<List<Long>> legs = new ArrayList<>(); // Liste contenant les segments de parcours (pour reconstruire visuellement la route)

        while (!remaining.isEmpty()) { // Boucler tant qu'il reste des sommets à visiter
            double best = Double.POSITIVE_INFINITY;
            long bestNode = -1;
            for (long candidate : remaining) { // Rechercher parmi tous les sommets le plus proche
                double dist = pairwise.get(key(current, candidate)).getDistance();
                if (dist < best) { // Comparer les distances et garder le sommet le plus court
                    best = dist;
                    bestNode = candidate;
                }
            }
            if (bestNode == -1 || Double.isInfinite(best)) { // Vérifier si l'on a une vraie solution (éviter erreurs de graphes cassés)
                return new TSPSolution(new ArrayList<>(), Double.POSITIVE_INFINITY, false, new ArrayList<>());
            }
            order.add(bestNode); // Ajouter ce sommet dans la tournée
            legs.add(pairwise.get(key(current, bestNode)).getPath()); // Ajouter le chemin détaillé (liste de sommets) pour les legs
            total += best; // Ajouter la distance
            current = bestNode; // Déplacer le curseur courant
            remaining.remove(bestNode); // Retirer ce sommet de l’ensemble des sommets restants
        }

        // Fermer la boucle en revenant au départ
        total += pairwise.get(key(current, startId)).getDistance();
        legs.add(pairwise.get(key(current, startId)).getPath());

        // Construire l’ordre final avec startId au début et à la fin
        List<Long> finalOrder = new ArrayList<>();
        finalOrder.add(startId);
        finalOrder.addAll(order);
        finalOrder.add(startId);
        return new TSPSolution(finalOrder, total, true, legs);
    }

    // Construire un objet TSPSolution
    private TSPSolution buildSolution(long startId, List<Long> orderWithoutStart, Map<String, Dijkstra.PathResult> pairwise) {
        // Construire la tournée complète
        List<Long> fullOrder = new ArrayList<>();
        fullOrder.add(startId);
        fullOrder.addAll(orderWithoutStart);
        fullOrder.add(startId);

        double distance = routeDistance(startId, orderWithoutStart, pairwise); // Calculer la distance totale du circuit
        List<List<Long>> legs = new ArrayList<>(); // Construire les segments détaillés du parcours
        long current = startId;
        for (long target : orderWithoutStart) { // Ajouter chaque leg correspondant aux distances calculées
            legs.add(pairwise.get(key(current, target)).getPath());
            current = target;
        }
        legs.add(pairwise.get(key(current, startId)).getPath()); // Ajouter le dernier segment (retour à la base)

        return new TSPSolution(fullOrder, distance, true, legs);
    }

    // Calculer la distance totale d'un circuit TSP partiel ou complet
    private double routeDistance(long startId, List<Long> order, Map<String, Dijkstra.PathResult> pairwise) {
        double total = 0.0;
        long current = startId;
        for (long target : order) { // Parcourir chaque sommet dans l’ordre imposé
            total += pairwise.get(key(current, target)).getDistance();
            current = target;
        }
        total += pairwise.get(key(current, startId)).getDistance(); // Ajouter le retour final au point de départ
        return total;
    }

    private String key(long a, long b) {
        if (a < b) {
            return a + "-" + b;
        } else {
            return b + "-" + a;
        }
    }
}