public class Node {
    private final long id; // Stocker l'identifiant unique du sommet
    private final double latitude; // Latitude du sommet
    private final double longitude; // Longitude du sommet
    private final boolean inferred; // Créer ?

    public Node(long id, double latitude, double longitude) {
        this(id, latitude, longitude, false);
    }

    public Node(long id, double latitude, double longitude, boolean inferred) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.inferred = inferred;
    }

    public long getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isInferred() {
        return inferred;
    }
}
