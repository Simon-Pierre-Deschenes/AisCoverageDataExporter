import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Minimalist implementation of the Source class.
 */
public class Source implements Serializable {

    private static final long serialVersionUID = 1L;
    private Map<String, Cell> grid = new ConcurrentHashMap<String, Cell>();
    private String identifier;
    private int multiplicationFactor = 1;

    public Source(String identifier) {
        this.identifier = identifier;
    }

    public Cell getCell(double latitude, double longitude) {
        return grid.get(Helper.getCellId(latitude, longitude, multiplicationFactor));
    }

    public Cell getTempCell(double latitude, double longitude, int multiplicationFactorTemp) {
        return grid.get(Helper.getCellId(latitude, longitude, multiplicationFactorTemp));
    }

    public Cell createTempCell(double latitude, double longitude, int multiplicationFactorTemp) {
        String id = Helper.getCellId(latitude, longitude, multiplicationFactorTemp);
        double lat = Helper.roundLat(latitude, multiplicationFactorTemp);
        double lon = Helper.roundLon(longitude, multiplicationFactorTemp);
        Cell cell = new Cell(lat, lon, id);
        grid.put(cell.getId(), cell);

        return cell;
    }

    public void addCell(Cell cell) {
        grid.put(cell.getId(), cell);
    }

    public Map<String, Cell> getGrid() {
        return grid;
    }

    public String getIdentifier() {
        return identifier;
    }
}
