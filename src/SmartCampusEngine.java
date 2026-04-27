import java.sql.*;
import java.util.*;

public class SmartCampusEngine {
    private final Map<String, List<Edge>> graph = new HashMap<>();
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
                String src = rs.getString("SourceNode");
                Edge edge = new Edge(
                        rs.getString("TargetNode"),
                        rs.getDouble("BaseDistance"),
                        rs.getBoolean("IsStairs")
                );
                edge.congestionFactor = rs.getDouble("CongestionFactor");
                graph.computeIfAbsent(src, k -> new ArrayList<>()).add(edge);
            }
        }

        if (!foundRows) {
            throw new SQLException("MapEdges is empty. Load campus edge data before running pathfinding.");
        }
    }

    // Step 2: Implement Weighted Dijkstra
    public List<String> findPath(String start, String end, boolean needsAccessible) throws CampusException {
        if (!graph.containsKey(start)) throw new InvalidNodeException(start);

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

            for (Edge edge : graph.getOrDefault(u, new ArrayList<>())) {
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
}
