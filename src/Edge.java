public class Edge {
    private final int id; // Stocker l'identifiant unique de l'arête
    private final long from; // Stocker le sommet de départ
    private final long to; // Stocker le sommet d'arrivée
    private final double weight; // Stocker le poids de l'arête
    private final String name; // Stocker le nom de la rue

    public Edge(int id, long from, long to, double weight, String name) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public double getWeight() {
        return weight;
    }

    public String getName() {
        return name;
    }

    public long getOther(long nodeId) {
        // retourne l'autre extremité de l'arête
        if (nodeId == from) {
            return to; // Renvoyer le sommet d'arrivée si nodeId est le départ
        } else if (nodeId == to) {
            return from; // Renvoyer le sommet de départ si nodeId est l'arrivée
        } else {
            throw new IllegalArgumentException("Cette arête ne contient pas le point : " + nodeId);
        }
    }
}