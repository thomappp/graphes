import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Graph {
    // Stocker les sommets du graphe indexés par ID
    private final Map<Long, Node> nodes = new HashMap<>();
    // Stocker les arêtes adjacentes à chaque sommet
    private final Map<Long, List<Edge>> adjacency = new HashMap<>();
    // Gérer les identifiants uniques pour chaque arête
    private int nextEdgeId = 1;

    // Charger les sommets puis les arêtes depuis les CSV
    public static Graph loadFromCsv(String nodesFile, String edgesFile) throws IOException {
        Graph graph = new Graph();
        int nodesRead = 0;
        // Lire le fichier des sommets
        try (BufferedReader br = new BufferedReader(new FileReader(nodesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue; // Ignorer les lignes vides
                }
                String[] parts = line.split(";", -1);
                if (parts.length < 3 || parts[0].equalsIgnoreCase("id")) {
                    continue; // Ignorer l’en-tête et les lignes incomplètes
                }
                try {
                    // Séparer les données de chaque ligne
                    long id = Long.parseLong(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim().replace(",", "."));
                    double lon = Double.parseDouble(parts[2].trim().replace(",", "."));
                    graph.addNode(new Node(id, lat, lon, false)); // Ajouter le sommet
                    nodesRead++;
                } catch (NumberFormatException ignored) {
                    // Ignorer les lignes mal formées
                }
            }
        }
        System.out.println("Sommets lus dans le CSV : " + nodesRead);

        int edgesRead = 0;
        int edgesRejectedMissing = 0;
        int edgeParseErrors = 0;
        int debugShown = 0;
        final int DEBUG_LIMIT = 5;
        // Lire le fichier des arêtes
        try (BufferedReader br = new BufferedReader(new FileReader(edgesFile))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue; // Ignorer les lignes vides
                }
                if (first) {
                    first = false;
                    String[] header = line.split(";", -1);
                    if (header.length > 0 && header[0].equalsIgnoreCase("from")) {
                        continue; // Ignorer l’en-tête
                    }
                }
                String[] parts = line.split(";", -1);
                if (parts.length < 3) {
                    edgeParseErrors++;
                    if (debugShown < DEBUG_LIMIT) {
                        System.out.println("Erreur parsing (colonnes manquantes) sur la ligne : " + line);
                        debugShown++;
                    }
                    continue; // Ignorer la ligne incorrecte
                }
                try {
                    long from = Long.parseLong(parts[0].trim());
                    long to = Long.parseLong(parts[1].trim());
                    double distance = Double.parseDouble(parts[2].trim().replace(",", "."));
                    String name = parts.length > 5 ? parts[5] : "";
                    edgesRead++;
                    Node template = graph.getNode(from);
                    if (template == null && graph.nodes.values().iterator().hasNext()) {
                        template = graph.nodes.values().iterator().next(); // Prendre un sommet comme modèle
                    }
                    graph.ensureNodeExists(from, template); // Créer le sommet source si absent
                    graph.ensureNodeExists(to, graph.getNode(from)); // Créer le sommet cible si absent
                    graph.addUndirectedEdge(from, to, distance, name); // Ajouter l'arête
                } catch (Exception e) {
                    edgeParseErrors++;
                    if (debugShown < DEBUG_LIMIT) {
                        System.out.println("Erreur parsing arête : " + e.getMessage() + " | ligne brute : " + line);
                        e.printStackTrace(System.out);
                        debugShown++;
                    }
                }
            }
        }
        System.out.println("Arêtes lues dans le CSV : " + edgesRead);
        System.out.println("Arêtes rejetées (car sommets introuvables) : " + edgesRejectedMissing);
        if (edgeParseErrors > 0) {
            System.out.println("Arêtes ignorées (format invalide) : " + edgeParseErrors);
        }

        int orphanCount = 0;
        // Compter les sommets sans arête
        for (long id : graph.nodes.keySet()) {
            if (graph.getDegree(id) == 0) {
                orphanCount++;
            }
        }
        System.out.println("Sommet(s) sans arête : " + orphanCount);

        int removed = graph.keepLargestConnectedComponent();
        System.out.println("Nettoyage terminé : " + removed + " sommet(s) sans arête supprimé(s). Graphe final : " + graph.nodes.size() + " sommets");
        return graph;
    }

    public static List<CollectionPoint> loadCollectionPoints(String csvPath) throws IOException {
        List<CollectionPoint> points = new ArrayList<>(); // Créer une liste pour stocker les points de collecte
        Random rnd = new Random(); // Initialiser un générateur aléatoire pour le volume
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) { // Ouvrir le fichier CSV
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) { // Ignorer les lignes vides
                    continue;
                }
                String[] parts = line.split(";", -1);
                if (parts.length < 6 || parts[0].equalsIgnoreCase("id")) {
                    continue; // Ignorer l’en-tête ou les lignes incomplètes
                }
                try {
                    // Séparer les données du fichier
                    long id = Long.parseLong(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim().replace(",", "."));
                    double lon = Double.parseDouble(parts[2].trim().replace(",", "."));
                    String amenity = parts[3];
                    String recyclingType = parts[4];
                    String name = parts[5];
                    int volume = 1 + rnd.nextInt(5); // Générer un volume aléatoire pour simuler 1..5 m3
                    points.add(new CollectionPoint(id, lat, lon, amenity, recyclingType, name, 0.0, volume, -1L)); // Ajouter le point à la liste
                } catch (NumberFormatException ignored) {
                    // Ignorer les lignes mal formées
                }
            }
        }
        return points; // Retourner la liste des points
    }

    // Ajouter un sommet au graphe si absent
    public void addNode(Node node) {
        nodes.putIfAbsent(node.getId(), node);
    }

    // Vérifier si un sommet existe et le créer si nécessaire
    public void ensureNodeExists(long id, Node template) {
        if (nodes.containsKey(id)) {
            return; // Ne rien faire si le sommet existe
        }
        double lat = 0.0;
        double lon = 0.0;
        if (template != null) {
            lat = template.getLatitude();
            lon = template.getLongitude();
        }
        // Créer un sommet inféré et l’ajouter
        Node inferred = new Node(id, lat, lon, true);
        addNode(inferred);
    }

    // Ajouter une arête non orientée entre deux sommets
    public void addUndirectedEdge(long from, long to, double weight, String name) {
        ensureNodeExists(from, null); // S’assurer que le sommet de départ existe
        ensureNodeExists(to, nodes.get(from)); // S’assurer que le sommet d’arrivée existe
        Edge edge = new Edge(nextEdgeId++, from, to, weight, name == null ? "" : name);
        addEdgeReference(edge); // Ajouter l’arête dans l’adjacence
    }

    // Ajouter une arête dans la structure d’adjacence
    private void addEdgeReference(Edge edge) {
        adjacency.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge); // Lier l’arête au sommet de départ
        adjacency.computeIfAbsent(edge.getTo(), k -> new ArrayList<>()).add(edge); // Lier l’arête au sommet d’arrivée
    }

    // Récupérer un sommet par son ID
    public Node getNode(long id) {
        return nodes.get(id);
    }

    // Trouver le sommet le plus proche d’une position
    public Long findNearestNodeId(double latitude, double longitude) {
        long bestId = -1;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Node node : nodes.values()) {
            // Calculer distance euclidienne simplifiée
            double dLat = node.getLatitude() - latitude;
            double dLon = node.getLongitude() - longitude;
            double dist = dLat * dLat + dLon * dLon;
            // Mettre à jour le sommet le plus proche si nécessaire
            if (dist < bestDist) {
                bestDist = dist;
                bestId = node.getId();
            }
        }
        return bestId == -1 ? null : bestId; // Retourner null si aucun sommet trouvé
    }

    // Récupérer toutes les arêtes d’un sommet
    public List<Edge> getEdges(long nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList());
    }

    // Récupérer l’arête entre deux sommets
    public Edge getEdgeBetween(long a, long b) {
        Edge best = null;
        // Parcourir toutes les arêtes du sommet a
        for (Edge edge : adjacency.getOrDefault(a, Collections.emptyList())) {
            if (edge.getOther(a) == b) { // Vérifier si l’arête relie au sommet b

                // Garder l’arête la plus courte
                if (best == null || edge.getWeight() < best.getWeight()) {
                    best = edge; // Choisir l’arête la plus courte
                }
            }
        }
        return best;
    }

    // Récupérer tous les IDs des sommets
    public Set<Long> getNodeIds() {
        return nodes.keySet();
    }

    // Compter le nombre total d’arêtes
    public int getEdgeCount() {
        int total = 0;
        for (List<Edge> edges : adjacency.values()) {
            total += edges.size();
        }
        return total / 2; // Diviser par 2 car graphe non orienté
    }

    // Récupérer le degré d’un sommet (nombre d’arêtes)
    public int getDegree(long nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList()).size();
    }

    // Récupérer les sommets ayant un nombre impair d’arêtes
    public List<Long> getOddDegreeNodes() {
        List<Long> odd = new ArrayList<>();
        // Vérifier chaque sommet
        for (long id : nodes.keySet()) {
            if (getDegree(id) % 2 != 0) {
                odd.add(id); // Ajouter le sommet à la liste
            }
        }
        return odd;
    }

    // Créer une copie complète du graphe
    public Graph createCopy() {
        Graph copy = new Graph();
        copy.nodes.putAll(this.nodes); // Copier tous les sommets
        Map<Integer, Edge> uniqueEdges = new HashMap<>();
        int maxEdgeId = 0;
        // Copier toutes les arêtes uniques
        for (List<Edge> edges : adjacency.values()) {
            for (Edge e : edges) {
                uniqueEdges.putIfAbsent(e.getId(), e); // Ajouter chaque arête unique
                maxEdgeId = Math.max(maxEdgeId, e.getId());
            }
        }
        // Ajouter chaque arête à la copie
        for (Edge e : uniqueEdges.values()) {
            Edge duplicate = new Edge(e.getId(), e.getFrom(), e.getTo(), e.getWeight(), e.getName());
            copy.addEdgeReference(duplicate);
        }
        // Mettre à jour le compteur d’ID d’arête
        copy.nextEdgeId = Math.max(maxEdgeId + 1, this.nextEdgeId);
        return copy;
    }

    // Récupérer le poids de l’arête entre deux sommets
    public double getEdgeWeightBetween(long from, long to) {
        double best = Double.POSITIVE_INFINITY;
        // Chercher toutes les arêtes depuis le sommet from
        for (Edge edge : adjacency.getOrDefault(from, Collections.emptyList())) {
            if (edge.getOther(from) == to) {
                best = Math.min(best, edge.getWeight()); // Garder le poids minimal
            }
        }
        return best;
    }

    // Ajouter un chemin virtuel composé de plusieurs sommets
    public void addVirtualPath(List<Long> path, String label) {
        if (path == null || path.size() < 2) {
            return; // Ignorer les chemins invalides
        }
        // Parcourir les sommets du chemin
        for (int i = 0; i < path.size() - 1; i++) {
            long u = path.get(i);
            long v = path.get(i + 1);
            double w = getEdgeWeightBetween(u, v); // Récupérer le poids de l’arête existante
            if (Double.isInfinite(w)) {
                continue; // Ignorer si l’arête est manquante
            }
            addUndirectedEdge(u, v, w, label); // Ajouter l’arête virtuelle au graphe
        }
    }

    // Vérifier si tous les sommets sont connectés depuis un sommet donné
    public boolean isConnectedFrom(long startId) {
        if (!nodes.containsKey(startId)) {
            return false; // Retourner faux si le sommet n’existe pas
        }
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(startId);
        visited.add(startId);
        // Parcourir tous les sommets accessibles
        while (!stack.isEmpty()) {
            long current = stack.pop();
            for (Edge e : getEdges(current)) {
                long next = e.getOther(current);
                if (visited.add(next)) {
                    stack.push(next); // Marquer le sommet comme visité
                }
            }
        }
        return visited.size() == nodes.size(); // Vérifier que tous les sommets sont atteints
    }

    // Récupérer tous les sommets atteignables depuis un sommet
    public List<Long> getReachableNodes(long startId) {
        if (!nodes.containsKey(startId)) {
            return Collections.emptyList(); // Retourner liste vide si sommet inexistant
        }
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(startId);
        visited.add(startId);
        // Parcourir les sommets accessibles
        while (!stack.isEmpty()) {
            long current = stack.pop();
            for (Edge e : getEdges(current)) {
                long next = e.getOther(current);
                if (visited.add(next)) {
                    stack.push(next); // Ajouter les sommets non visités
                }
            }
        }
        return new ArrayList<>(visited); // Retourner la liste des sommets atteignables
    }

    // Conserver uniquement la plus grande composante connectée du graphe
    public int keepLargestConnectedComponent() {
        if (nodes.isEmpty()) {
            return 0; // Retourner 0 si aucun sommet
        }
        Set<Long> visited = new HashSet<>();
        List<Set<Long>> components = new ArrayList<>();

        // Identifier toutes les composantes connexes
        for (long nodeId : nodes.keySet()) {
            if (visited.contains(nodeId)) {
                continue; // Ignorer si sommet déjà visité
            }
            Set<Long> component = new HashSet<>();
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(nodeId);
            visited.add(nodeId);

            // Parcourir la composante
            while (!stack.isEmpty()) {
                long current = stack.pop();
                component.add(current); // Ajouter le sommet à la composante
                for (Edge edge : getEdges(current)) {
                    long next = edge.getOther(current);
                    if (visited.add(next)) {
                        stack.push(next); // Ajouter sommet voisin non visité
                    }
                }
            }
            components.add(component); // Ajouter la composante à la liste
        }

        // Trier les composantes par taille décroissante
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));
        // Afficher la taille des 5 plus grandes composantes
        System.out.print("Taille des 5 plus grandes composantes connexes : ");
        for (int i = 0; i < Math.min(5, components.size()); i++) {
            System.out.print(components.get(i).size());
            if (i < Math.min(5, components.size()) - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();

        if (components.isEmpty()) {
            return 0; // Retourner 0 si aucune composante
        }
        Set<Long> largest = components.get(0);
        int before = nodes.size();

        // Supprimer les sommets hors de la plus grande composante
        nodes.keySet().removeIf(id -> !largest.contains(id));
        adjacency.keySet().removeIf(id -> !largest.contains(id));
        // Supprimer les arêtes qui ne sont plus connectées
        for (Map.Entry<Long, List<Edge>> entry : adjacency.entrySet()) {
            entry.getValue().removeIf(e -> !largest.contains(e.getOther(entry.getKey())));
        }
        int after = nodes.size();
        return before - after; // Retourner le nombre de sommets supprimés
    }
}