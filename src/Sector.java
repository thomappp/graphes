import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Sector {
    private final String id; // Identifiant unique du secteur
    private final int gridX; // Coordonnée X dans la grille
    private final int gridY; // Coordonnée Y dans la grille
    private final List<Long> nodeIds = new ArrayList<>(); // Liste des IDs des sommets contenus dans ce secteur
    private final Set<String> neighbors = new HashSet<>(); // Voisinage direct dans la grille pour la coloration

    public Sector(String id, int gridX, int gridY) {
        this.id = id;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public String getId() {
        return id;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void addNode(long nodeId) {
        nodeIds.add(nodeId);
    }

    public Set<String> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(String neighborId) {
        neighbors.add(neighborId);
    }
}