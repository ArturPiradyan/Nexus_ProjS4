import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SmartCampusEngine engine = new SmartCampusEngine();

        try {
            System.out.println("Connecting to SQL Server and loading spatial data...");
            engine.loadMapFromDB();

            // Example Search: Entrance to Lab 302, Accessible Route
            String start = "ENTRANCE_A";
            String destination = "LAB_302";
            boolean isWheelchairUser = true;

            System.out.println("Calculating shortest path for " + destination + "...");
            List<String> path = engine.findPath(start, destination, isWheelchairUser);

            System.out.println("Path Found: " + String.join(" -> ", path));

        } catch (InvalidNodeException | PathNotFoundException e) {
            System.err.println("LOGIC ERROR: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("DATABASE ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("FATAL ERROR: " + e.getMessage());
        }
    }
}
