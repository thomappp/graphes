import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static final String NODES_FILE = "data/nodes.csv";
    private static final String EDGES_FILE = "data/edges.csv";
    private static final String COLLECTION_FILE = "data/dechets_semicolon_clean.csv";
    private static final int DEFAULT_TOUR_SIZE = 10;

    public static void main(String[] args) {
        Graph graph;
        List<CollectionPoint> rawCollectionPoints = new ArrayList<>();
        try {
            //chargement unique des donnees CSV avant toute interaction
            graph = Graph.loadFromCsv(NODES_FILE, EDGES_FILE);
            rawCollectionPoints = Graph.loadCollectionPoints(COLLECTION_FILE);
        } catch (IOException e) {
            System.err.println("Erreur de lecture des fichiers CSV : " + e.getMessage());
            return;
        }

        if (graph.getNodeIds().isEmpty()) {
            System.err.println("Aucun noeud charge. Verifiez nodes.csv.");
            return;
        }
        long depotId = graph.getNodeIds().stream().min(Long::compareTo).orElse(-1L);
        System.out.println("Graphe chargé : " + graph.getNodeIds().size() + " noeuds, " + graph.getEdgeCount() + " aretes.");
        System.out.println("Dépôt (point de départ) : " + depotId);
        List<CollectionPoint> mappedPoints = mapCollectionPoints(graph, rawCollectionPoints);
        System.out.println("Points de collecte : " + mappedPoints.size());

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            //ecran d'authentification multi-profils
            System.out.println("\n=== GESTION DES DECHETS (LEVALLOIS) ===");
            System.out.println("Veuillez selectionner votre profil utilisateur :");
            System.out.println("1. Collectivité (Mairie de Levallois)");
            System.out.println("2. Prestataire de Collecte");
            System.out.println("3. Quitter");
            System.out.print("Votre choix : ");
            String profileChoice = scanner.nextLine();
            switch (profileChoice) {
                case "1" -> collectiviteMenu(scanner, graph);
                case "2" -> prestataireMenu(scanner, graph, depotId, mappedPoints);
                case "3" -> running = false;
                default -> System.out.println("Le choix est invalide.");
            }
        }
        System.out.println("Vous avez quitté le programme.");
    }

    private static void collectiviteMenu(Scanner scanner, Graph graph) {
        boolean back = false;
        while (!back) {
            //menu restreint pour la mairie (planification globale uniquement)
            System.out.println("\nMenu Collectivité - Mairie de Levallois");
            System.out.println("1. Consulter le plan de la ville (statistiques du graphe)");
            System.out.println("2. Planification des secteurs & jours de collecte (Thème 3)");
            System.out.println("3. Déconnexion");
            System.out.print("Votre choix : ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> showGraphStats(graph);
                case "2" -> runTheme3(graph);
                case "3" -> back = true;
                default -> System.out.println("Le choix est invalide.");
            }
        }
    }

    private static void prestataireMenu(Scanner scanner, Graph graph, long depotId, List<CollectionPoint> mappedPoints) {
        boolean back = false;
        while (!back) {
            //menu operationnel pour le prestataire (themes techniques)
            System.out.println("\nMenu Prestataire");
            System.out.println("1. Optimisation Itinéraires Voirie (Thème 1)");
            System.out.println("2. Optimisation Points de Collecte (Thème 2)");
            System.out.println("3. Déconnexion");
            System.out.print("Votre choix : ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> theme1Menu(scanner, graph, depotId);
                case "2" -> theme2Menu(scanner, graph, depotId, mappedPoints);
                case "3" -> back = true;
                default -> System.out.println("Le choix est invalide.");
            }
        }
    }

    private static void showGraphStats(Graph graph) {
        System.out.println("Plan de la ville - Statistiques du graphe :");
        System.out.println("- Nombre de sommets : " + graph.getNodeIds().size());
        System.out.println("- Nombre d'arêtes : " + graph.getEdgeCount());
    }

    private static void theme1Menu(Scanner scanner, Graph graph, long depotId) {
        Random random = new Random();
        boolean back = false;
        while (!back) {
            System.out.println("\nThème 1");
            System.out.println("1. Collecte Encombrants (1 point) - Dijkstra");
            System.out.println("2. Collecte Encombrants (Tournée 10 points) - TSP");
            System.out.println("3. Collecte Globale - Postier chinois");
            System.out.println("4. Retour");
            System.out.print("Votre choix : ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> runSinglePickup(scanner, graph, depotId, random);
                case "2" -> runTourPickup(scanner, graph, depotId, random);
                case "3" -> runChinesePostman(graph, depotId);
                case "4" -> back = true;
                default -> System.out.println("Le choix est invalide.");
            }
        }
    }

    private static void theme2Menu(Scanner scanner, Graph graph, long depotId, List<CollectionPoint> collectionPoints) {
        if (collectionPoints.isEmpty()) {
            System.out.println("Aucun point de collecte valide (mapping impossible).");
            return;
        }
        Theme2Solver solver = new Theme2Solver(graph, depotId, collectionPoints);
        boolean back = false;
        while (!back) {
            System.out.println("\nThème 2");
            System.out.println("1. Plus proche voisin");
            System.out.println("2. MST");
            System.out.println("3. Lancer le Comparatif");
            System.out.println("4. Retour");
            System.out.print("Votre choix : ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> {
                    Theme2Solver.TourResult res = solver.solveNearestNeighbor();
                    if (!res.isFeasible()) {
                        System.out.println("Approche Plus Proche Voisin impossible (graphe non connexe ?)");
                        break;
                    }
                    solver.printTourWithSplits(res);
                    List<Theme2Solver.SplitTour> splits = solver.splitTourIntoSubTours(res.getOrder());
                    maybeExportGeoJson(scanner, graph, solver, splits, "output/resultat_theme2_greedy.geojson");
                }
                case "2" -> {
                    Theme2Solver.TourResult res = solver.solveMSTApprox();
                    if (!res.isFeasible()) {
                        System.out.println("Approche MST impossible (graphe non connexe ?)");
                        break;
                    }
                    solver.printTourWithSplits(res);
                    List<Theme2Solver.SplitTour> splits = solver.splitTourIntoSubTours(res.getOrder());
                    maybeExportGeoJson(scanner, graph, solver, splits, "output/resultat_theme2_mst.geojson");
                }
                case "3" -> {
                    Theme2Solver.TourResult nn = solver.solveNearestNeighbor();
                    Theme2Solver.TourResult mst = solver.solveMSTApprox();
                    if (!nn.isFeasible()) {
                        System.out.println("Approche Plus Proche Voisin impossible (graphe non connexe ?)");
                    } else {
                        System.out.printf("Distance totale (Greedy) : %.2f m%n", nn.getDistance());
                    }
                    if (!mst.isFeasible()) {
                        System.out.println("Approche MST impossible (graphe non connexe ?)");
                    } else {
                        System.out.printf("Distance totale (MST) : %.2f m%n", mst.getDistance());
                    }
                    Theme2Solver.TourResult best = null;
                    if (nn.isFeasible() && mst.isFeasible()) {
                        best = nn.getDistance() <= mst.getDistance() ? nn : mst;
                        double base = nn.getDistance();
                        double compare = mst.getDistance();
                        double diffPercent = ((compare - base) / base) * 100.0;
                        System.out.printf("Gain/Perte (MST vs Greedy) : %.2f %% %n", diffPercent);
                    } else if (nn.isFeasible()) {
                        best = nn;
                    } else if (mst.isFeasible()) {
                        best = mst;
                    }
                    if (best != null && best.isFeasible()) {
                        System.out.println("Meilleure tournée : " + best.getMethod());
                        solver.printTourWithSplits(best);
                        List<Theme2Solver.SplitTour> splits = solver.splitTourIntoSubTours(best.getOrder());
                        maybeExportGeoJson(scanner, graph, solver, splits, "output/resultat_theme2_best.geojson");
                    } else {
                        System.out.println("Aucune tournée trouvable.");
                    }
                }
                case "4" -> back = true;
                default -> System.out.println("Le choix est invalide.");
            }
        }
    }

    private static void runSinglePickup(Scanner scanner, Graph graph, long depotId, Random random) {
        System.out.println("Collecte Encombrants (1 point) - Algorithme de Dijkstra");
        System.out.print("Entrez l'identifiant du sommet cible (ou aléatoire : entrée) : ");
        String line = scanner.nextLine();
        long targetId;
        if (line.isBlank()) {
            targetId = pickRandomNode(graph, depotId, random);
            System.out.println("Sommet aléatoire choisi : " + targetId);
        } else {
            try {
                targetId = Long.parseLong(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("L'identifiant est invalide.");
                return;
            }
        }
        Dijkstra.PathResult result = Dijkstra.shortestPath(graph, depotId, targetId);
        if (!result.isReachable()) {
            System.out.println("Aucun chemin trouvé entre le dépot (" + depotId + ") et l'arrivée (" + targetId + ").");
            return;
        }
        System.out.printf("Itinéraire (distance : %.2f m%n)", result.getDistance());
        printPathWithStreets(graph, result.getPath());
    }

    private static void runTourPickup(Scanner scanner, Graph graph, long depotId, Random random) {
        System.out.println("Collecte Encombrants (tournée) - TSP");
        System.out.println("Combien de sommets cibler ? (Par défaut ~10 : entrée) : ");
        String line = scanner.nextLine();
        int count = DEFAULT_TOUR_SIZE;
        if (!line.isBlank()) {
            try {
                count = Integer.parseInt(line.trim());
                if (count <= 0) {
                    System.out.println("Le nombre doit etre positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("La valeur est invalide.");
                return;
            }
        }

        System.out.println("Chargement en cours ...");

        List<Long> targets = pickRandomTargets(graph, depotId, count, random);
        if (targets.isEmpty()) {
            System.out.println("Impossible de selectionner des sommets cibles.");
            return;
        }
        System.out.println("Sommets à visiter : " + targets);
        TSPSolver solver = new TSPSolver(graph);
        TSPSolver.TSPSolution solution = solver.solve(depotId, targets);
        if (!solution.isFeasible()) {
            System.out.println("Impossible de trouver une tournée (graphe non connexe ?).");
            return;
        }
        System.out.println("Ordre de visite (retour au dépôt inclus) : " + solution.getOrder());
        System.out.println("Détails des trajets :");
        for (int i = 0; i < solution.getLegs().size(); i++) {
            System.out.println("Trajet n°" + (i + 1) + " :");
            printPathWithStreets(graph, solution.getLegs().get(i));
        }
        System.out.printf("Distance totale estimée : %.2f m%n", solution.getDistance());
    }

    private static void runChinesePostman(Graph graph, long depotId) {
        System.out.println("Chargement en cours ...");

        EulerianSolver solver = new EulerianSolver(graph);
        EulerianSolver.PostmanResult result = solver.solveChinesePostman(depotId);
        if (!result.isFeasible()) {
            System.out.println("Aucun circuit eulerien trouvé (graphe non connexe ?).");
            return;
        }
        printPathWithStreets(graph, result.getTrail());
        System.out.printf("Distance totale : %.2f m%n", result.getDistance());
    }

    private static long pickRandomNode(Graph graph, long depotId, Random random) {
        List<Long> reachable = graph.getReachableNodes(depotId);
        if (reachable.isEmpty()) {
            return depotId;
        }
        reachable.remove(depotId);
        if (reachable.isEmpty()) {
            return depotId;
        }
        return reachable.get(random.nextInt(reachable.size()));
    }

    private static List<Long> pickRandomTargets(Graph graph, long depotId, int count, Random random) {
        List<Long> reachable = graph.getReachableNodes(depotId);
        reachable.remove(depotId);
        Collections.shuffle(reachable, random);
        if (reachable.size() > count) {
            reachable = new ArrayList<>(reachable.subList(0, count));
        }
        return reachable;
    }

    private static void printPathWithStreets(Graph graph, List<Long> path) {
        if (path == null || path.size() < 2) {
            System.out.println("Aucun trajet.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  Départ");
        for (int i = 0; i < path.size() - 1; i++) {
            long a = path.get(i);
            long b = path.get(i + 1);
            Edge edge = graph.getEdgeBetween(a, b);
            String name = (edge != null && edge.getName() != null && !edge.getName().isBlank())
                    ? edge.getName()
                    : "Rue sans nom";
            double dist = edge != null ? edge.getWeight() : Double.NaN;
            sb.append(" -> ");
            sb.append(name);
            if (!Double.isNaN(dist)) {
                sb.append(String.format(" (%.2f m)", dist));
            }
        }
        sb.append(" -> Arrivée");
        System.out.println(sb.toString());
    }

    private static void runTheme3(Graph graph) {
        System.out.println("Découpage de la ville en grille 4x4...");
        int gridSize = 4;
        SectorManager manager = new SectorManager(graph, gridSize);
        List<Sector> sectors = manager.buildSectors();
        if (sectors.isEmpty()) {
            System.out.println("Aucun secteur généré (Erreur : graphe vide ?).");
            return;
        }
        System.out.println("Secteurs générés : " + sectors.size());
        for (Sector s : sectors) {
            System.out.println("  " + s.getId() + " -> " + s.getNodeIds().size() + " noeuds");
        }

        GraphColoringSolver solver = new GraphColoringSolver();
        GraphColoringSolver.ColoringResult res = solver.color(sectors);

        Map<String, List<Sector>> byDay = new HashMap<>();
        for (Sector s : sectors) {
            String day = res.getColorBySector().get(s.getId());
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(s);
        }

        System.out.println("Planning final (aucun voisin le meme jour) :");
        for (String day : byDay.keySet()) {
            System.out.print("  " + day + " : ");
            List<Sector> list = byDay.get(day);
            list.sort((a, b) -> a.getId().compareTo(b.getId()));
            for (int i = 0; i < list.size(); i++) {
                System.out.print(list.get(i).getId());
                if (i < list.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println();
        }
        System.out.println("Nombre chromatique (jours necessaires) : " + res.getChromaticNumber());
    }

    private static void maybeExportGeoJson(Scanner scanner, Graph graph, Theme2Solver solver, List<Theme2Solver.SplitTour> splits, String filename) {
        if (splits == null || splits.isEmpty()) {
            return;
        }
        System.out.print("Voulez-vous exporter ce resultat en GeoJSON ? (O/N) : ");
        String line = scanner.nextLine().trim().toLowerCase();
        if (!line.equals("o")) {
            return;
        }
        List<List<Node>> tours = convertSplitToursToNodes(graph, solver, splits);
        try {
            GeoJsonExporter.exportToursToGeoJson(tours, filename);
        } catch (IOException e) {
            System.out.println("Echec export GeoJSON : " + e.getMessage());
        }
    }

    private static List<List<Node>> convertSplitToursToNodes(Graph graph, Theme2Solver solver, List<Theme2Solver.SplitTour> splits) {
        List<List<Node>> result = new ArrayList<>();
        for (Theme2Solver.SplitTour st : splits) {
            List<Node> tourNodes = solver.buildStreetPath(graph, st.path);
            if (!tourNodes.isEmpty()) {
                result.add(tourNodes);
            }
        }
        return result;
    }

    private static List<CollectionPoint> mapCollectionPoints(Graph graph, List<CollectionPoint> rawPoints) {
        List<CollectionPoint> mapped = new ArrayList<>();
        for (CollectionPoint cp : rawPoints) {
            Long nearest = graph.findNearestNodeId(cp.getLatitude(), cp.getLongitude());
            if (nearest == null) {
                continue;
            }
            CollectionPoint mappedCp = new CollectionPoint(
                    cp.getId(),
                    cp.getLatitude(),
                    cp.getLongitude(),
                    cp.getAmenity(),
                    cp.getRecyclingType(),
                    cp.getName(),
                    cp.getCapacity(),
                    cp.getVolume(),
                    nearest
            );
            mapped.add(mappedCp);
        }
        return mapped;
    }
}
