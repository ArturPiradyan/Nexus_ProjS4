import java.sql.*;
import java.util.*;

public class SmartCampusEngine {
    private final Map<String, List<Edge>> graph = new HashMap<>();
    private final Map<String, AreaStatus> areaStatuses = new HashMap<>();
    private final String dbUrl;
    private final Properties dbProperties = new Properties();
    private final boolean hasUsername;
    private final boolean hasPassword;

    public SmartCampusEngine() {
        dbUrl = readConfig(
                "SMART_CAMPUS_DB_URL",
                "smartcampus.db.url",
                "jdbc:sqlserver://localhost:1433;databaseName=SmartCampusNexus;encrypt=true;trustServerCertificate=true;loginTimeout=5;"
        );

        String username = readConfig("SMART_CAMPUS_DB_USER", "smartcampus.db.user", "");
        String password = readConfig("SMART_CAMPUS_DB_PASSWORD", "smartcampus.db.password", "");
        hasUsername = !username.isBlank();
        hasPassword = !password.isBlank();

        if (hasUsername) {
            dbProperties.setProperty("user", username);
        }
        if (hasPassword) {
            dbProperties.setProperty("password", password);
        }
    }

    private String readConfig(String envKey, String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }

    // Step 1: Load graph from SQL Server
    public void loadMapFromDB() throws SQLException {
        graph.clear();
        areaStatuses.clear();

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "SQL Server JDBC driver not found. Add mssql-jdbc to the runtime classpath.",
                    e
            );
        }

        if (!hasUsername && !dbUrl.toLowerCase(Locale.ROOT).contains("integratedsecurity=true")) {
            throw new SQLException(
                    "No database login configured. Set SMART_CAMPUS_DB_USER and SMART_CAMPUS_DB_PASSWORD, " +
                    "or use a JDBC URL with integratedSecurity=true."
            );
        }

        if (hasUsername && !hasPassword) {
            throw new SQLException("Database username is set but SMART_CAMPUS_DB_PASSWORD is missing.");
        }

        boolean foundRows = false;

        try (
                Connection conn = dbProperties.isEmpty()
                        ? DriverManager.getConnection(dbUrl)
                        : DriverManager.getConnection(dbUrl, dbProperties);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT SourceNode, TargetNode, BaseDistance, CongestionFactor, IsStairs FROM MapEdges"
                )
        ) {

            while (rs.next()) {
                foundRows = true;
                String src = normalizeNodeId(rs.getString("SourceNode"));
                String target = normalizeNodeId(rs.getString("TargetNode"));
                double baseDistance = rs.getDouble("BaseDistance");
                double congestionFactor = rs.getDouble("CongestionFactor");
                boolean isStairs = rs.getBoolean("IsStairs");

                addEdge(src, target, baseDistance, congestionFactor, isStairs);
                addEdge(target, src, baseDistance, congestionFactor, isStairs);
            }
        }

        if (!foundRows) {
            throw new SQLException("MapEdges is empty. Load campus edge data before running pathfinding.");
        }

        rebuildAreaStatuses();
    }

    // Step 2: Implement Weighted Dijkstra
    public List<String> findPath(String start, String end, boolean needsAccessible) throws CampusException {
        start = normalizeNodeId(start);
        end = normalizeNodeId(end);

        if (!graph.containsKey(start)) throw new InvalidNodeException(start);
        if (!graph.containsKey(end)) throw new InvalidNodeException(end);

        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        pq.add(new NodeDistance(start, 0));
        distances.put(start, 0.0);

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.nodeId;

            if (u.equals(end)) break;
            if (visited.contains(u)) continue;
            visited.add(u);

            for (Edge edge : graph.getOrDefault(u, Collections.emptyList())) {
                // ADA Compliance Check
                if (needsAccessible && edge.isStairs) continue;

                double newDist = distances.get(u) + edge.getEffectiveWeight();
                if (newDist < distances.getOrDefault(edge.targetNodeId, Double.MAX_VALUE)) {
                    distances.put(edge.targetNodeId, newDist);
                    previous.put(edge.targetNodeId, u);
                    pq.add(new NodeDistance(edge.targetNodeId, newDist));
                }
            }
        }

        return reconstructPath(previous, start, end);
    }

    private List<String> reconstructPath(Map<String, String> previous, String start, String end) throws PathNotFoundException {
        LinkedList<String> path = new LinkedList<>();
        for (String at = end; at != null; at = previous.get(at)) {
            path.addFirst(at);
        }
        if (path.isEmpty() || !path.getFirst().equals(start)) throw new PathNotFoundException(start, end);
        return path;
    }

    public List<AreaStatus> getAreaStatuses() {
        List<AreaStatus> statuses = new ArrayList<>(areaStatuses.values());
        statuses.sort(
                Comparator.comparingDouble(AreaStatus::getOccupancyPercent).reversed()
                        .thenComparing(AreaStatus::getNodeId)
        );
        return statuses;
    }

    public List<AreaStatus> getAreaStatusesForPath(List<String> path) {
        List<AreaStatus> routeStatuses = new ArrayList<>();
        for (String nodeId : path) {
            AreaStatus status = areaStatuses.get(normalizeNodeId(nodeId));
            if (status != null) {
                routeStatuses.add(status);
            }
        }
        return routeStatuses;
    }

    public double calculateAverageOccupancy(List<AreaStatus> statuses) {
        if (statuses.isEmpty()) {
            return 0.0;
        }
        return statuses.stream().mapToDouble(AreaStatus::getOccupancyPercent).average().orElse(0.0);
    }

    public double calculateAverageCongestion(List<AreaStatus> statuses) {
        if (statuses.isEmpty()) {
            return 0.0;
        }
        return statuses.stream().mapToDouble(AreaStatus::getAverageCongestionFactor).average().orElse(0.0);
    }

    private void addEdge(String source, String target, double distance, double congestionFactor, boolean isStairs) {
        graph.computeIfAbsent(source, k -> new ArrayList<>());
        graph.computeIfAbsent(target, k -> new ArrayList<>());

        List<Edge> edges = graph.get(source);
        boolean alreadyPresent = edges.stream().anyMatch(edge ->
                edge.targetNodeId.equals(target)
                        && Double.compare(edge.distance, distance) == 0
                        && Double.compare(edge.congestionFactor, congestionFactor) == 0
                        && edge.isStairs == isStairs
        );

        if (!alreadyPresent) {
            Edge edge = new Edge(target, distance, isStairs);
            edge.congestionFactor = congestionFactor;
            edges.add(edge);
        }
    }

    private String normalizeNodeId(String nodeId) {
        return nodeId == null ? "" : nodeId.trim().toUpperCase(Locale.ROOT);
    }

    private void rebuildAreaStatuses() {
        for (Map.Entry<String, List<Edge>> entry : graph.entrySet()) {
            String nodeId = entry.getKey();
            List<Edge> edges = entry.getValue();
            int connectionCount = edges.size();

            double avgCongestion = edges.stream()
                    .mapToDouble(edge -> edge.congestionFactor)
                    .average()
                    .orElse(1.0);

            double occupancyPercent = estimateOccupancyPercent(connectionCount, avgCongestion);
            String availabilityLabel = resolveAvailabilityLabel(occupancyPercent);
            String congestionLabel = resolveCongestionLabel(avgCongestion);

            areaStatuses.put(
                    nodeId,
                    new AreaStatus(
                            nodeId,
                            occupancyPercent,
                            avgCongestion,
                            connectionCount,
                            availabilityLabel,
                            congestionLabel
                    )
            );
        }
    }

    private double estimateOccupancyPercent(int connectionCount, double avgCongestion) {
        double connectionLoad = Math.min(connectionCount * 8.0, 28.0);
        double congestionLoad = Math.max(0.0, avgCongestion - 1.0) * 55.0;
        double occupancy = 18.0 + connectionLoad + congestionLoad;
        return Math.max(5.0, Math.min(100.0, occupancy));
    }

    private String resolveAvailabilityLabel(double occupancyPercent) {
        if (occupancyPercent < 35.0) {
            return "Open";
        }
        if (occupancyPercent < 60.0) {
            return "Available";
        }
        if (occupancyPercent < 80.0) {
            return "Limited";
        }
        return "Crowded";
    }

    private String resolveCongestionLabel(double avgCongestion) {
        if (avgCongestion < 1.1) {
            return "Low";
        }
        if (avgCongestion < 1.3) {
            return "Moderate";
        }
        if (avgCongestion < 1.6) {
            return "High";
        }
        return "Severe";
    }
}
