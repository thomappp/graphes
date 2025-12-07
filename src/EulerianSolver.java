import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EulerianSolver {
    private final Graph graph; // Stocker le graphe à traiter

    public EulerianSolver(Graph graph) {
        this.graph = graph;
    }

    public static class PostmanResult {
        private final boolean feasible; // Indiquer si un circuit eulérien existe
        private final List<Long> trail; // Stocker la séquence des sommets parcourus
        private final double distance; // Stocker la distance totale du circuit

        public PostmanResult(boolean feasible, List<Long> trail, double distance) {
            this.feasible = feasible;
            this.trail = trail;
            this.distance = distance;
        }

        public boolean isFeasible() {
            return feasible;
        }

        public List<Long> getTrail() {
            return trail;
        }

        public double getDistance() {
            return distance;
        }
    }

    // Résoudre le problème du facteur chinois depuis un sommet donné
    public PostmanResult solveChinesePostman(long startId) {
        if (!graph.getNodeIds().contains(startId)) {
            return new PostmanResult(false, new ArrayList<>(), Double.POSITIVE_INFINITY); // Vérifier la validité du départ
        }

        if (!graph.isConnectedFrom(startId)) {
            return new PostmanResult(false, new ArrayList<>(), Double.POSITIVE_INFINITY); // Vérifier la connexité du graphe
        }

        Graph working = graph.createCopy(); // Créer une copie pour ajouter les chemins virtuels
        // Équilibrer les degrés via ajout de chemins virtuels
        List<Long> oddNodes = working.getOddDegreeNodes(); // Identifier les sommets de degré impair
        if (oddNodes.size() == 2) {
            addShortestPathBetween(working, oddNodes.get(0), oddNodes.get(1)); // Ajouter un chemin entre les deux nœuds impairs
        } else if (oddNodes.size() > 2) {
            boolean matched = addGreedyMatchingPaths(working, oddNodes); // Faire un appariement glouton
            if (!matched) {
                return new PostmanResult(false, new ArrayList<>(), Double.POSITIVE_INFINITY); // Échec si impossible
            }
        }

        List<Long> trail = new ArrayList<>(); // Créer la liste du circuit
        double distance = runHierholzer(working, startId, trail); // Exécuter l’algorithme de Hierholzer
        boolean feasible = !trail.isEmpty() && !Double.isInfinite(distance); // Vérifier la faisabilité
        return new PostmanResult(feasible, trail, distance); // Retourner le résultat
    }

    // Ajouter le chemin le plus court entre deux sommets impairs
    private void addShortestPathBetween(Graph working, long a, long b) {
        Dijkstra.PathResult path = Dijkstra.shortestPath(working, a, b); // Calculer le chemin le plus court
        if (path.isReachable()) {
            working.addVirtualPath(path.getPath(), "virtual"); // Ajouter un chemin virtuel dans le graphe
        }
    }

    // Ajouter des chemins pour équilibrer tous les sommets impairs
    private boolean addGreedyMatchingPaths(Graph working, List<Long> oddNodes) {
        Set<Long> unmatched = new HashSet<>(oddNodes); // Créer un ensemble de sommets non appariés
        while (!unmatched.isEmpty()) {
            long current = unmatched.iterator().next(); // Sélectionner un sommet courant
            unmatched.remove(current); // Retirer le sommet courant
            long bestPartner = -1; // Initialiser le meilleur partenaire
            double bestDistance = Double.POSITIVE_INFINITY; // Initialiser la meilleure distance
            for (long candidate : unmatched) {
                Dijkstra.PathResult path = Dijkstra.shortestPath(working, current, candidate); // Calculer le chemin vers un candidat
                if (path.isReachable() && path.getDistance() < bestDistance) {
                    bestDistance = path.getDistance(); // Mettre à jour la meilleure distance
                    bestPartner = candidate; // Mettre à jour le meilleur partenaire
                }
            }
            if (bestPartner == -1) {
                return false; // Retourner false si aucun appariement possible
            }
            unmatched.remove(bestPartner); // Retirer le partenaire choisi
            Dijkstra.PathResult path = Dijkstra.shortestPath(working, current, bestPartner); // Calculer à nouveau le chemin
            working.addVirtualPath(path.getPath(), "virtual"); // Ajouter le chemin virtuel
        }
        return true; // Retourner true si tous les appariements sont réalisés
    }

    // Exécuter l’algorithme de Hierholzer pour construire le circuit eulérien
    private double runHierholzer(Graph working, long startId, List<Long> outputTrail) {
        if (working.getEdges(startId).isEmpty()) {
            return Double.POSITIVE_INFINITY; // Vérifier qu’il existe au moins une arête
        }
        Deque<Long> stack = new ArrayDeque<>(); // Créer une pile pour Hierholzer
        stack.push(startId); // Empiler le sommet de départ
        Set<Integer> usedEdges = new HashSet<>(); // Stocker les arêtes déjà utilisées
        double distance = 0.0; // Initialiser la distance totale

        while (!stack.isEmpty()) {
            // Étape Hierholzer: empiler tant qu'il reste des arêtes, dépiler sinon
            long v = stack.peek();
            Iterator<Edge> iterator = working.getEdges(v).iterator(); // Obtenir un itérateur sur les arêtes
            Edge nextEdge = null; // Initialiser l’arête suivante
            while (iterator.hasNext()) {
                Edge e = iterator.next();
                if (!usedEdges.contains(e.getId())) {
                    nextEdge = e; // Sélectionner la première arête non utilisée
                    break;
                }
            }
            if (nextEdge != null) {
                usedEdges.add(nextEdge.getId()); // Marquer l’arête comme utilisée
                distance += nextEdge.getWeight(); // Ajouter son poids à la distance
                long u = nextEdge.getOther(v); // Obtenir l’autre extrémité
                stack.push(u); // Empiler le voisin
            } else {
                outputTrail.add(stack.pop()); // Dépiler si aucune arête disponible
            }
        }
        if (outputTrail.isEmpty()) {
            return Double.POSITIVE_INFINITY; // Retourner infini si aucun circuit trouvé
        }
        java.util.Collections.reverse(outputTrail); // Inverser la liste pour obtenir l’ordre correct
        return distance; // Retourner la distance totale du circuit
    }
}
