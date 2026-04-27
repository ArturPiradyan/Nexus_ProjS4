class CampusException extends Exception {
    public CampusException(String message) { super(message); }
}

class PathNotFoundException extends CampusException {
    public PathNotFoundException(String start, String end) {
        super("No route found between " + start + " and " + end + ".");
    }
}

class InvalidNodeException extends CampusException {
    public InvalidNodeException(String node) {
        super("Node '" + node + "' is not defined in the campus database.");
    }
}
