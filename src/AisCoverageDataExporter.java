import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class AisCoverageDataExporter {
    private static ConcurrentHashMap<String, Source> sources = new ConcurrentHashMap();

    public static void main (String[] args) {
        try {

            boolean help = false;
            for (String arg : args) {
                if (arg.equals("-h") || arg.equals("--help")) {
                    help = true;
                    break;
                }
            }

            if (help) {

                String exportDataTypes = "";
                for (ExportDataType exportDataType : ExportDataType.values()) {
                    if (!exportDataTypes.equals("")) {
                        exportDataTypes += ", ";
                    }

                    exportDataTypes += exportDataType.name();
                }


                System.out.println("------------------------------------------------------------");
                System.out.println("Program usage :");
                System.out.println("AisCoverageDataExporter DATABASE_NAME [EXPORT_DATA_TYPE] [OPTIONS]");
                System.out.println("Available export data types are " + exportDataTypes + ".");
                System.out.println("If no export data type is specified, no KML will be generated.");
                System.out.println("------------------------------------------------------------");
                System.out.println("Program options :");
                System.out.println("--help | -h : Print program help.");
                System.out.println("------------------------------------------------------------");

                System.exit(0);

            } else {

                if (args.length > 1) {

                    String dbName = args[0];
                    ExportDataType exportDataType = ExportDataType.forType(args[1]);

                    loadSavedCoverageData(dbName);
                    exportDataToJSon(dbName + ".json");
                    exportDataToKML(dbName + ".kml", exportDataType);

                    System.exit(0);

                } else if (args.length > 0) {

                    String dbName = args[0];

                    loadSavedCoverageData(dbName);
                    exportDataToJSon(dbName + ".json");

                    System.exit(0);

                } else {

                    System.err.println("No arguments were passed. Please use -h or --help to print program help.");

                    System.exit(1);

                }

            }

        } catch (IllegalArgumentException e) {

            String exportDataTypes = "";
            for (ExportDataType exportDataType : ExportDataType.values()) {
                if (!exportDataTypes.equals("")) {
                    exportDataTypes += ", ";
                }

                exportDataTypes += exportDataType.name();
            }

            System.err.println("Error : specified export data type does not exist, available export data types are " + exportDataTypes + ".");

            System.exit(1);

        }
    }

    private static Document decompressCells(Document marshalledCoverageData) {
        String base64 = (String) marshalledCoverageData.get("compressedCells");
        if (base64 != null && !base64.trim().equals("")) {
            byte[] gzippedData = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.US_ASCII));

            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzippedData))) {

                InputStreamReader isr = new InputStreamReader(gzipInputStream, StandardCharsets.US_ASCII);
                BufferedReader br = new BufferedReader(isr);

                String jsonData = br.readLine();

                br.close();
                isr.close();
                gzipInputStream.close();

                return Document.parse(jsonData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new Document();
    }

    private static Map<Long, TimeSpan> unmarshallTimeSpans(Map<String, Map<String, Number>> fixedWidthTimeSpans) {
        Map<Long, TimeSpan> unmarshalledTimeSpans = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Number>> timespan : fixedWidthTimeSpans.entrySet()) {
            TimeSpan unmarshalledTimeSpan = new TimeSpan(new Date(timespan.getValue().get("firstMessage").longValue()));
            unmarshalledTimeSpan.setLastMessage(new Date(timespan.getValue().get("lastMessage").longValue()));
            unmarshalledTimeSpan.setMessageCounterTerrestrial(timespan.getValue().get("messageCounterTerrestrial").intValue());
            unmarshalledTimeSpan.setMissingSignals(timespan.getValue().get("missingSignals").intValue());

            Integer numberOfVsiMessages = (Integer) timespan.getValue().get("vsiMessageCounter");
            if (numberOfVsiMessages != null) {
                unmarshalledTimeSpan.setVsiMessageCounter(numberOfVsiMessages.intValue());
                unmarshalledTimeSpan.setAverageSignalStrength(((Integer) timespan.getValue().get("averageSignalStrength")).intValue());
            }

            unmarshalledTimeSpans.put(Long.valueOf(timespan.getKey()), unmarshalledTimeSpan);
        }
        return unmarshalledTimeSpans;
    }

    private static Cell unmarshallCell(Map<String, Object> cell) {
        double latitude = Helper.roundLat((double) cell.get("latitude"), 1);
        double longitude = Helper.roundLon((double) cell.get("longitude"), 1);
        Cell unmarshalledCell = new Cell(latitude, longitude, (String) cell.get("cellId"));

        Map<String, Map<String, Number>> fixedWidthTimeSpans = (Map<String, Map<String, Number>>) cell.get("timespans");
        Map<Long, TimeSpan> unmarshalledTimeSpans = unmarshallTimeSpans(fixedWidthTimeSpans);

        unmarshalledCell.setFixedWidthSpans(unmarshalledTimeSpans);
        return unmarshalledCell;
    }

    private static Map<String, Collection<Cell>> unmarshallCells(Document coverageData) {
        Map<String, Collection<Cell>> unmarshalledCoverageData = new LinkedHashMap();
        Document decompressedCells = decompressCells(coverageData);
        Object cells = decompressedCells.get("cells");

        if (cells != null && (cells instanceof List)) {
            List<Map<String, Object>> grid = (List<Map<String, Object>>) cells;
            for (Map<String, Object> cell : grid) {
                Cell unmarshalledCell = unmarshallCell(cell);
                String sourceId = (String) cell.get("sourceId");
                if (unmarshalledCoverageData.containsKey(sourceId)) {
                    unmarshalledCoverageData.get(sourceId).add(unmarshalledCell);
                } else {
                    unmarshalledCoverageData.put(sourceId, new ArrayList(Arrays.asList(unmarshalledCell)));
                }
            }
        }

        return unmarshalledCoverageData;
    }

    public static Map<String, Collection<Cell>> unmarshall(Document coverageData) {
        Map<String, Collection<Cell>> unmarshalledCoverageData = new LinkedHashMap();

        if (coverageData != null) {
            unmarshalledCoverageData.putAll(unmarshallCells(coverageData));
        }

        return unmarshalledCoverageData;
    }

    private static void updateCellTimeSpans(Cell oldCell, Cell newCell) {
        for (Map.Entry<Long, TimeSpan> newTimeSpan : newCell.getFixedWidthSpans().entrySet()) {
            oldCell.getFixedWidthSpans().put(newTimeSpan.getKey(), newTimeSpan.getValue());
        }
    }

    public static void updateCell(String sourceId, Cell newCell) {
        Source source = sources.get(sourceId);
        if (source == null) {
            source = new Source(sourceId);
            sources.put(sourceId, source);
        }

        Cell oldCell = source.getCell(newCell.getLatitude(), newCell.getLongitude());
        if (oldCell == null) {
            source.addCell(newCell);
        } else {
            updateCellTimeSpans(oldCell, newCell);
        }
    }

    public static void loadSavedCoverageData(String dbName) {
        try {
            ServerAddress serverAddress = new ServerAddress("localhost", 27017);
            MongoClient client = new MongoClient(serverAddress, MongoClientOptions.builder().build());

            MongoCursor<String> databaseNames = client.listDatabaseNames().iterator();
            boolean found = false;
            while (databaseNames.hasNext()) {
                if (databaseNames.next().equals(dbName)) {
                    found = true;
                }
            }
            if (!found) {
                System.err.println("Error : database with the specified name was not found.");
                System.exit(1);
            }

            FindIterable<Document> foundDocuments = client.getDatabase(dbName).getCollection("coverageData").find().noCursorTimeout(true);

            String day = "";

            //loop through all records and unmarshall cells
            for (Document document : foundDocuments) {
                String temp = (String)document.get("dataTimestamp");
                if (!temp.substring(0, temp.indexOf("T")).equals(day)) {
                    day = temp.substring(0, temp.indexOf("T"));
                    System.out.println(day);
                }

                Map<String, Collection<Cell>> map = unmarshall(document);
                for (Map.Entry<String, Collection<Cell>> entry : map.entrySet()) {
                    for (Cell cell : entry.getValue()) {
                        updateCell(entry.getKey(), cell);
                    }
                }
            }

            //set cell information using timespans
            for (Source source : sources.values()) {
                for (Cell cell : source.getGrid().values()) {
                    for (TimeSpan timeSpan : cell.getFixedWidthSpans().values()) {
                        cell.addReceivedSignals(timeSpan.getMessageCounterTerrestrial());
                        cell.addNOofMissingSignals(timeSpan.getMissingSignals());
                        cell.addVsiMessages(timeSpan.getVsiMessageCounter(), timeSpan.getAverageSignalStrength());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Object> marshallCell(String sourceId, Cell cell) {
        Map<String, Object> savedCell = new LinkedHashMap();

        savedCell.put("sourceId", sourceId);
        savedCell.put("cellId", cell.getId());
        savedCell.put("latitude", cell.getLatitude());
        savedCell.put("longitude", cell.getLongitude());
        savedCell.put("numberOfReceivedSignals", cell.getNOofReceivedSignals());
        savedCell.put("numberOfMissingSignals", cell.getNOofMissingSignals());
        savedCell.put("numberOfVsiMessages", cell.getNumberOfVsiMessages());
        savedCell.put("averageSignalStrength", cell.getAverageSignalStrength());

        return savedCell;
    }

    private static void exportDataToJSon (String fileName) {
        try {
            LinkedHashMap<String, ArrayList<Map<String, Object>>> map = new LinkedHashMap();

            ArrayList<Map<String, Object>> cells = new ArrayList();
            for (Source source : sources.values()) {
                for (Cell cell : source.getGrid().values()) {
                    cells.add(marshallCell(source.getIdentifier(), cell));
                }
            }
            map.put("cells", cells);

            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.US_ASCII);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            osw.write(gson.toJson(map));

            osw.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportDataToKML (String fileName, ExportDataType dataType) {
        int multiplicity = 1;//exportMultiFactor
        Date starttime = new Date(0);
        Date endtime = new Date();

        ConcurrentHashMap<String, Source> tempSources = new ConcurrentHashMap();
        Source superbs = sources.get("supersource");

        for (Source bs : sources.values()) {
            Source summedbs = new Source(bs.getIdentifier());
            tempSources.put(bs.getIdentifier(), summedbs);
            Collection<Cell> cells = bs.getGrid().values();

            for (Cell cell : cells) {
                Cell dhCell = summedbs.getTempCell(cell.getLatitude(), cell.getLongitude(), multiplicity);
                if (dhCell == null) {
                    dhCell = summedbs.createTempCell(cell.getLatitude(), cell.getLongitude(), multiplicity);
                }

                Cell activesbscell = superbs.getGrid().get(cell.getId());
                if (activesbscell != null) {
                    int receivedsignals = cell.getNOofReceivedSignals(starttime, endtime);
                    dhCell.addReceivedSignals(receivedsignals);

                    int sbstotalmessages = activesbscell.getNOofReceivedSignals(starttime, endtime)
                            + activesbscell.getNOofMissingSignals(starttime, endtime);
                    dhCell.addNOofMissingSignals(sbstotalmessages - receivedsignals);

                    dhCell.addVsiMessages(activesbscell.getNumberOfVsiMessages(starttime, endtime),
                            activesbscell.getAverageSignalStrength(starttime, endtime));
                }
            }
        }
        KMLGenerator.generateKML(tempSources.values(), Helper.getLatSize(), Helper.getLonSize(), multiplicity, dataType, fileName);
    }
}
