import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SectorManager {
    private final Graph graph; // Graphe à découper en secteurs
    private final int gridSize; // Taille de la grille (nombre de secteurs par dimension)
    private final Map<String, Sector> sectors = new HashMap<>(); // Map des secteurs par ID

    // Constructeur avec graphe et taille de la grille
    public SectorManager(Graph graph, int gridSize) {
        this.graph = graph;
        this.gridSize = gridSize;
    }

    // Construire les secteurs et assigner les sommets
    public List<Sector> buildSectors() {
        if (graph.getNodeIds().isEmpty()) {
            return new ArrayList<>(); // Retourner vide si aucun sommet
        }
        Bounds bounds = computeBounds(); // Calculer min / max des coordonnées
        double latSpan = bounds.maxLat - bounds.minLat;
        double lonSpan = bounds.maxLon - bounds.minLon;
        // Eviter division par 0
        if (latSpan == 0) {
            latSpan = 1e-6;
        }
        if (lonSpan == 0) {
            lonSpan = 1e-6;
        }
        // Parcourir tous les sommets
        for (long nodeId : graph.getNodeIds()) {
            Node n = graph.getNode(nodeId);
            // Projeter le sommet sur la grille
            int gx = (int) Math.floor((n.getLatitude() - bounds.minLat) / latSpan * gridSize);
            int gy = (int) Math.floor((n.getLongitude() - bounds.minLon) / lonSpan * gridSize);
            // Limiter les indices dans la grille
            if (gx < 0) gx = 0;
            if (gy < 0) gy = 0;
            if (gx >= gridSize) gx = gridSize - 1;
            if (gy >= gridSize) gy = gridSize - 1;
            final int finalGx = gx;
            final int finalGy = gy;
            // Créer ou récupérer le secteur
            String id = sectorId(gx, gy);
            Sector s = sectors.computeIfAbsent(id, k -> new Sector(id, finalGx, finalGy));
            s.addNode(nodeId); // Ajouter le noeud au secteur
        }

        // Construire l'adjacence entre secteurs voisins
        for (Sector s : sectors.values()) {
            int gx = s.getGridX();
            int gy = s.getGridY();
            linkIfPresent(s, gx + 1, gy); // droite
            linkIfPresent(s, gx - 1, gy); // gauche
            linkIfPresent(s, gx, gy + 1); // haut
            linkIfPresent(s, gx, gy - 1); // bas
        }
        return new ArrayList<>(sectors.values()); // Retourner la liste des secteurs
    }

    // Lier un secteur à un voisin si le voisin existe
    private void linkIfPresent(Sector s, int gx, int gy) {
        String neighborId = sectorId(gx, gy);
        Sector neighbor = sectors.get(neighborId);
        if (neighbor != null) {
            s.addNeighbor(neighborId); // Ajouter le voisin au secteur
            neighbor.addNeighbor(s.getId()); // Ajouter le secteur au voisin
        }
    }

    // Calculer les bornes min/max des coordonnées du graphe
    private Bounds computeBounds() {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (long id : graph.getNodeIds()) {
            Node n = graph.getNode(id);
            minLat = Math.min(minLat, n.getLatitude()); // min latitude
            maxLat = Math.max(maxLat, n.getLatitude()); // max latitude
            minLon = Math.min(minLon, n.getLongitude()); // min longitude
            maxLon = Math.max(maxLon, n.getLongitude()); // max longitude
        }
        return new Bounds(minLat, maxLat, minLon, maxLon);
    }

    private String sectorId(int gx, int gy) {
        return "Secteur_" + gx + "_" + gy;
    }

    private static class Bounds {
        final double minLat;
        final double maxLat;
        final double minLon;
        final double maxLon;

        Bounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }
}