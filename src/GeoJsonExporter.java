import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class GeoJsonExporter {

    // Exporter un seul chemin en GeoJSON (LineString)
    public static void exportToGeoJson(List<Node> path, String filename) throws IOException {
        ensureParentDir(filename); // Vérifier / créer le dossier parent si nécessaire
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            // Ouvrir FeatureCollection
            bw.write("{\"type\":\"FeatureCollection\",\"features\":[");
            // Créer une feature représentant la ligne du parcours
            bw.write("{\"type\":\"Feature\",\"properties\":{\"stroke\":\"#ff0000\",\"stroke-width\":3},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[");
            // Écrire toutes les coordonnées (lon, lat)
            for (int i = 0; i < path.size(); i++) {
                Node n = path.get(i);
                bw.write("[" + n.getLongitude() + "," + n.getLatitude() + "]");
                if (i < path.size() - 1) {
                    bw.write(","); // Ajouter une virgule si ce n'est pas le dernier point
                }
            }
            bw.write("]}}]}"); // Fermer geometry, feature et featureCollection
        }
        System.out.println("Fichier exporté : " + filename); // Indiquer la fin de l’export
    }

    // Exporter plusieurs tournées d’un coup (multi-LineString, une couleur par tour)
    public static void exportToursToGeoJson(List<List<Node>> tours, String filename) throws IOException {
        String[] colors = {"#ff0000", "#0000ff", "#00aa00", "#ff9900", "#9900ff", "#00cccc", "#cc00cc"};
        ensureParentDir(filename);  // Vérifier / créer le dossier parent
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            bw.write("{\"type\":\"FeatureCollection\",\"features\":["); // Ouvrir FeatureCollection
            for (int t = 0; t < tours.size(); t++) {
                List<Node> tour = tours.get(t);
                String color = colors[t % colors.length]; // Choisir une couleur cyclique
                // Créer une feature par tournée
                bw.write("{\"type\":\"Feature\",\"properties\":{\"stroke\":\"" + color + "\",\"stroke-width\":3},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[");
                // Écrire toutes les coordonnées de la tournée
                for (int i = 0; i < tour.size(); i++) {
                    Node n = tour.get(i);
                    bw.write("[" + n.getLongitude() + "," + n.getLatitude() + "]");
                    if (i < tour.size() - 1) {
                        bw.write(",");
                    }
                }
                bw.write("]}}"); // Fermer la feature
                if (t < tours.size() - 1) {
                    bw.write(","); // Ajouter la virgule entre deux features
                }
            }
            bw.write("]}"); // Fermer FeatureCollection
        }
        System.out.println("Fichier exporté : " + filename);
    }

    // Vérifier que le dossier parent existe, sinon le créer
    private static void ensureParentDir(String filename) {
        File f = new File(filename);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // Créer les dossiers si besoin
        }
    }
}