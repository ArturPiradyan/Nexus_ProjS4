class Node {
    String id;
    String name;
    boolean isAccessible;

    public Node(String id, String name, boolean isAccessible) {
        this.id = id;
        this.name = name;
        this.isAccessible = isAccessible;
    }
}

class Edge {
    String targetNodeId;
    double distance;
    double congestionFactor;
    boolean isStairs;

    public Edge(String targetNodeId, double distance, boolean isStairs) {
        this.targetNodeId = targetNodeId;
        this.distance = distance;
        this.isStairs = isStairs;
        this.congestionFactor = 1.0;
    }

    public double getEffectiveWeight() {
        return distance * congestionFactor;
    }
}

class NodeDistance {
    String nodeId;
    double distance;

    public NodeDistance(String nodeId, double distance) {
        this.nodeId = nodeId;
        this.distance = distance;
    }
}

class AreaStatus {
    private final String nodeId;
    private final double occupancyPercent;
    private final double averageCongestionFactor;
    private final int connectionCount;
    private final String availabilityLabel;
    private final String congestionLabel;

    public AreaStatus(
            String nodeId,
            double occupancyPercent,
            double averageCongestionFactor,
            int connectionCount,
            String availabilityLabel,
            String congestionLabel
    ) {
        this.nodeId = nodeId;
        this.occupancyPercent = occupancyPercent;
        this.averageCongestionFactor = averageCongestionFactor;
        this.connectionCount = connectionCount;
        this.availabilityLabel = availabilityLabel;
        this.congestionLabel = congestionLabel;
    }

    public String getNodeId() {
        return nodeId;
    }

    public double getOccupancyPercent() {
        return occupancyPercent;
    }

    public double getAverageCongestionFactor() {
        return averageCongestionFactor;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public String getAvailabilityLabel() {
        return availabilityLabel;
    }

    public String getCongestionLabel() {
        return congestionLabel;
    }
}
