import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphColoringSolver {
    // Enumération des jours, utilisée comme couleurs
    public enum Day {
        LUNDI, MARDI, MERCREDI, JEUDI, VENDREDI, SAMEDI, DIMANCHE
    }

    public static class ColoringResult {
        private final Map<String, String> colorBySector; // Map secteur -> couleur assignée
        private final int chromaticNumber; // Nombre chromatique final

        public ColoringResult(Map<String, String> colorBySector, int chromaticNumber) {
            this.colorBySector = colorBySector;
            this.chromaticNumber = chromaticNumber;
        }

        public Map<String, String> getColorBySector() {
            return colorBySector;
        }

        public int getChromaticNumber() {
            return chromaticNumber;
        }
    }

    // Algorithme Welsh-Powell
    public ColoringResult color(List<Sector> sectors) {
        // Créer une map d'accès rapide par ID
        Map<String, Sector> byId = sectors.stream().collect(Collectors.toMap(Sector::getId, s -> s));
        // Copier la liste et trier par degré décroissant (Welsh-Powell)
        List<Sector> sorted = new ArrayList<>(sectors);
        sorted.sort(Comparator.comparingInt((Sector s) -> s.getNeighbors().size()).reversed());

        Map<String, String> assignment = new HashMap<>(); // Map pour stocker la couleur assignée à chaque secteur
        List<String> colors = colorPalette(); // Liste des couleurs disponibles (initialisées avec les jours)

        // Parcourir les secteurs dans l'ordre décroissant de degré
        for (Sector sector : sorted) {
            // Récupérer les couleurs déjà utilisées par les voisins
            Set<String> used = sector.getNeighbors().stream()
                    .map(assignment::get) // Récupérer couleur assignée au voisin
                    .filter(c -> c != null) // Ignorer voisins non colorés
                    .collect(Collectors.toSet());
            String chosen = null; // Stocker la couleur choisie pour ce secteur
            // Chercher la première couleur libre dans la palette
            for (String c : colors) {
                if (!used.contains(c)) {
                    chosen = c;
                    break; // Stop dès qu’une couleur disponible est trouvée
                }
            }
            // Si aucune couleur n'est libre, créer une nouvelle couleur
            if (chosen == null) {
                chosen = "Couleur_" + (colors.size() + 1);
                colors.add(chosen); // Ajouter la nouvelle couleur à la palette
            }
            assignment.put(sector.getId(), chosen); // Assigner la couleur choisie au secteur
        }

        // Calculer le nombre chromatique (nombre de couleurs utilisées)
        int chromatic = assignment.values().stream().collect(Collectors.toSet()).size();
        return new ColoringResult(assignment, chromatic); // Retourner le résultat de la coloration
    }

    // Créer la palette initiale de couleurs basée sur les jours
    private List<String> colorPalette() {
        List<String> colors = new ArrayList<>();
        for (Day d : Day.values()) {
            colors.add(d.name()); // Ajouter le nom du jour comme couleur
        }
        return colors; // Retourner la palette complète
    }
}
