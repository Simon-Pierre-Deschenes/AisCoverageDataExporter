import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;

/*
 * Generate KML from a Source and write it into a file.
 */
public class KMLGenerator {
    public static void generateKML(Collection<Source> grids, double latSize, double lonSize, int multiplicity, ExportDataType exportDataType, String fileName) {
        try {
            FileOutputStream out = new FileOutputStream(new File(fileName));

            writeLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", out);
            writeLine("<kml>", out);
            writeLine("<Document>", out);
            writeLine("<name>AIS Coverage</name>", out);
            writeLine("<open>1</open>", out);
            writeLine("<Style id=\"redStyle\">", out);
            writeLine("    <IconStyle>", out);
            writeLine("        <scale>1.3</scale>", out);
            writeLine("        <Icon>", out);
            writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
            writeLine("        </Icon>", out);
            writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
            writeLine("    </IconStyle>", out);
            writeLine("    <LineStyle>", out);
            writeLine("        <color>ff0000ff</color>", out);
            writeLine("    </LineStyle>", out);
            writeLine("    <PolyStyle>", out);
            writeLine("        <color>550000ff</color>", out);
            writeLine("        <fill>1</fill>", out);
            writeLine("    </PolyStyle>", out);
            writeLine("</Style>", out);
            writeLine("<Style id=\"orangeStyle\">", out);
            writeLine("    <IconStyle>", out);
            writeLine("        <scale>1.3</scale>", out);
            writeLine("        <Icon>", out);
            writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
            writeLine("        </Icon>", out);
            writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
            writeLine("    </IconStyle>", out);
            writeLine("    <LineStyle>", out);
            writeLine("        <color>ff00aaff</color>", out);
            writeLine("    </LineStyle>", out);
            writeLine("    <PolyStyle>", out);
            writeLine("        <color>5500aaff</color>", out);
            writeLine("        <fill>1</fill>", out);
            writeLine("    </PolyStyle>", out);
            writeLine("</Style>", out);
            writeLine("<Style id=\"greenStyle\">", out);
            writeLine("    <IconStyle>", out);
            writeLine("        <scale>1.3</scale>", out);
            writeLine("        <Icon>", out);
            writeLine("            <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>", out);
            writeLine("        </Icon>", out);
            writeLine("        <hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\"/>", out);
            writeLine("    </IconStyle>", out);
            writeLine("    <LineStyle>", out);
            writeLine("        <color>ff00ff00</color>", out);
            writeLine("    </LineStyle>", out);
            writeLine("    <PolyStyle>", out);
            writeLine("        <color>5500ff00</color>", out);
            writeLine("        <fill>1</fill>", out);
            writeLine("    </PolyStyle>", out);
            writeLine("</Style>", out);

            for (Source grid : grids) {
                generateGrid(grid.getIdentifier(), grid.getGrid().values(), out, latSize * multiplicity, lonSize * multiplicity, exportDataType);
            }

            writeLine("</Document>", out);
            writeLine("</kml>", out);

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeLine(String line, FileOutputStream out) {
        try {
            out.write((line + "\n").getBytes());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateGrid(String bsMmsi, Collection<Cell> cells, FileOutputStream out, double latSize, double lonSize, ExportDataType exportDataType) {
        writeLine("<Folder>", out);
        writeLine("    <name>" + bsMmsi + "</name>", out);
        writeLine("    <open>0</open>", out);
        for (Cell cell : cells) {
            double dataToExport;
            if (exportDataType == ExportDataType.RECEIVED_MESSAGES) {
                dataToExport = cell.getCoverage();
            } else {
                dataToExport = cell.getAverageSignalStrength();
            }

            if (dataToExport > exportDataType.greenThreshold()) { // green
                generatePlacemark("#greenStyle", cell, 0, out, latSize, lonSize);
            } else if (dataToExport > exportDataType.redThreshold()) { // orange
                generatePlacemark("#orangeStyle", cell, 0, out, latSize, lonSize);
            } else { // red
                generatePlacemark("#redStyle", cell, 0, out, latSize, lonSize);
            }
        }

        writeLine("</Folder>", out);
    }

    private static void generatePlacemark(String style, Cell cell, int z, FileOutputStream out, double latSize, double lonSize) {

        writeLine("    <Placemark>", out);
        writeLine("        <name>" + cell.getId() + "</name>", out);
        writeLine("        <styleUrl>" + style + "</styleUrl>", out);
        writeLine("        <Polygon>", out);
        writeLine("            <altitudeMode>clampedToGround</altitudeMode>", out);
        writeLine("            <tessellate>1</tessellate>", out);
        writeLine("            <outerBoundaryIs>", out);
        writeLine("                <LinearRing>", out);
        writeLine("                    <coordinates>", out);

        writeLine(
                cell.getLongitude() + "," + cell.getLatitude() + "," + z + " "
                        + (cell.getLongitude() + lonSize) + "," + cell.getLatitude() + "," + z + " "
                        + (cell.getLongitude() + lonSize) + "," + (cell.getLatitude() + latSize) + "," + z + " "
                        + cell.getLongitude() + "," + (cell.getLatitude() + latSize) + "," + z + " "
                        + cell.getLongitude() + "," + cell.getLatitude() + "," + z, out);

        writeLine("                    </coordinates>", out);
        writeLine("                </LinearRing>", out);
        writeLine("            </outerBoundaryIs>", out);
        writeLine("        </Polygon>", out);
        writeLine("    </Placemark>", out);
    }

}
