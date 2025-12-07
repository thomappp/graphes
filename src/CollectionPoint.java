public class CollectionPoint {
    private final long id; // Stocker l'identifiant unique du point de collecte
    private final double latitude; // Stocker la latitude du point
    private final double longitude; // Stocker la longitude du point
    private final String amenity; // Stocker le type d'équipement (ex : recycleur, déchetterie)
    private final String recyclingType; // Stocker le type de recyclage associé
    private final String name; // Stocker le nom du point de collecte
    private final double capacity; // Stocker la capacité maximale du point
    private final int volume; // Stocker le volume actuel de déchets
    private long nearestNodeId; // Stocker l'identifiant du sommet du graphe le plus proche

    public CollectionPoint(long id, double latitude, double longitude, String amenity, String recyclingType, String name) {
        this(id, latitude, longitude, amenity, recyclingType, name, 0.0, 0, -1L);
    }

    public CollectionPoint(long id, double latitude, double longitude, String amenity, String recyclingType, String name, double capacity, int volume, long nearestNodeId) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.amenity = amenity;
        this.recyclingType = recyclingType;
        this.name = name;
        this.capacity = capacity;
        this.volume = volume;
        this.nearestNodeId = nearestNodeId;
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

    public String getAmenity() {
        return amenity;
    }

    public String getRecyclingType() {
        return recyclingType;
    }

    public String getName() {
        return name;
    }

    public double getCapacity() {
        return capacity;
    }

    public int getVolume() {
        return volume;
    }

    public long getNearestNodeId() {
        return nearestNodeId;
    }

    public void setNearestNodeId(long nearestNodeId) {
        this.nearestNodeId = nearestNodeId;
    }
}