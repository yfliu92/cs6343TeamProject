package dht.rush.commands;

import dht.rush.CentralServer;
import dht.rush.clusters.Cluster;
import dht.rush.clusters.ClusterStructureMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class WriteCommand extends ServerCommand {
    private String fileName;
    private ClusterStructureMap clusterStructureMap;
    private CentralServer cs;

    @Override
    public void run() throws IOException {

        Map<Integer, Cluster> ret = clusterStructureMap.write(fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter writer = Json.createWriter(baos);
        JsonObject params;
        JsonObjectBuilder jcb = Json.createObjectBuilder();

        for (Map.Entry<Integer, Cluster> entry : ret.entrySet()) {
            jcb.add(String.valueOf(entry.getKey()), entry.getValue().toString());
        }
        params = jcb.build();

        writer.writeObject(params);
        writer.close();
        baos.writeTo(outputStream);
        outputStream.write("\n".getBytes());
        outputStream.flush();

        System.out.println();
        if (params != null) {
            System.out.println("Response Sent -- " + params.toString());
        } else {
            System.out.println("Response Sent");
        }
        
		System.out.println("Beginning to push DHT to all physical nodes --- " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));

    	synchronized(this.cs) {
    		this.cs.initializeDataNode(this.cs.getRoot());
    	}
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ClusterStructureMap getClusterStructureMap() {
        return clusterStructureMap;
    }

    public void setClusterStructureMap(ClusterStructureMap clusterStructureMap) {
        this.clusterStructureMap = clusterStructureMap;
    }
    
    public void setCentralServer(CentralServer cs) {
        this.cs = cs;
    }
}
