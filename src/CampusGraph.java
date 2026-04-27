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

// Represents a path between two locations
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



