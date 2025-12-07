import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    private static class NodeDistance {
        final long nodeId; // Stocker l'identifiant du sommet
        final double distance; // Stocker la distance associée à ce sommet

        NodeDistance(long nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    public static class PathResult {
        private final double distance; // Stocker la distance totale du chemin
        private final List<Long> path; // Stocker la liste des sommets du chemin

        public PathResult(double distance, List<Long> path) {
            this.distance = distance;
            this.path = path;
        }

        public double getDistance() {
            return distance;
        }

        public List<Long> getPath() {
            return path;
        }

        public boolean isReachable() {
            return !path.isEmpty();
        }
    }

    // Exécuter l’algorithme de Dijkstra avec reconstruction du chemin
    public static PathResult shortestPath(Graph graph, long source, long target) {
        Map<Long, Double> distances = new HashMap<>(); // Stocker les distances minimales
        Map<Long, Long> previous = new HashMap<>(); // Associer chaque sommet à son prédécesseur
        Set<Long> visited = new HashSet<>(); // Suivre les sommets déjà découverts
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                (a, b) -> Double.compare(a.distance, b.distance)); // Définir la file triée par distance

        for (long id : graph.getNodeIds()) {
            distances.put(id, Double.POSITIVE_INFINITY); // Initialiser toutes les distances à +infini
        }
        if (!distances.containsKey(source) || !distances.containsKey(target)) {
            return new PathResult(Double.POSITIVE_INFINITY, Collections.emptyList()); // Gérer les sommets invalides
        }
        distances.put(source, 0.0); // Définir la distance du point de départ
        queue.add(new NodeDistance(source, 0.0)); // Ajouter la source à la file

        while (!queue.isEmpty()) { // Parcourir la file tant qu'elle n'est pas vide
            NodeDistance entry = queue.poll(); // Extraire le sommet le plus proche
            long current = entry.nodeId; // Obtenir l'identifiant du sommet
            if (visited.contains(current)) {
                continue; // Ignorer les sommets déjà visités
            }
            visited.add(current); // Marquer le sommet comme visité

            if (current == target) {
                break; // Stopper si la cible est atteinte
            }
            for (Edge edge : graph.getEdges(current)) { // Parcourir les arêtes sortantes
                long neighbor = edge.getOther(current); // Obtenir le voisin
                if (visited.contains(neighbor)) {
                    continue; // Ignorer les voisins déjà visités
                }
                double newDist = distances.get(current) + edge.getWeight();// Calculer la nouvelle distance

                if (newDist < distances.get(neighbor)) { // Vérifier si une meilleure distance existe
                    distances.put(neighbor, newDist); // Mettre à jour la distance
                    previous.put(neighbor, current); // Enregistrer le précédent
                    queue.add(new NodeDistance(neighbor, newDist)); // Réinsérer dans la file
                }
            }
        }

        if (!previous.containsKey(target) && source != target) {
            return new PathResult(Double.POSITIVE_INFINITY, Collections.emptyList()); // Aucun chemin trouvé
        }
        List<Long> path = new ArrayList<>(); // Créer la liste du chemin
        long current = target; // Partir de la cible
        path.add(current); // Ajouter la cible au chemin
        while (current != source) { // Remonter jusqu'à la source
            Long prev = previous.get(current); // Obtenir le sommet précédent
            if (prev == null) {
                return new PathResult(Double.POSITIVE_INFINITY, Collections.emptyList());// Gérer une rupture
            }
            current = prev; // Remonter d’un niveau
            path.add(current); // Ajouter au chemin
        }
        Collections.reverse(path); // Inverser pour remettre dans l’ordre
        return new PathResult(distances.get(target), path); // Retourner le chemin et la distance
    }
}