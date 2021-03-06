package dht.rush.commands;

import dht.rush.CentralServer;
import dht.rush.clusters.Cluster;
import dht.rush.clusters.ClusterStructureMap;
import dht.rush.utils.RushUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddNodeCommand extends ServerCommand {
    private Cluster root;
    private String ip;
    private String port;
    private Double weight;
    private String subClusterId;
    private ClusterStructureMap clusterStructureMap;
    private CentralServer cs;

    @Override
    public void run() throws IOException {
        CommandResponse commandResponse = clusterStructureMap.addPhysicalNode(subClusterId, ip, port, weight);
        int status = commandResponse.getStatus();
        // "1": success
        // "2": No such a subcluster
        // "3": physical node already exits
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter writer = Json.createWriter(baos);
        JsonObject params = null;
        if (status == 1) {
            JsonObject ret = clusterStructureMap.getClusterMap();
            params = Json.createObjectBuilder()
                    .add("message", "Add Node Success, new epoch is :" + clusterStructureMap.getEpoch())
                    .add("status", "OK")
                    .add("transferList", commandResponse.toString())
                    .add("epoch", ret.get("epoch"))
                    .add("nodes", ret.getJsonObject("nodes"))
                    .build();
        } else if (status == 2) {
            params = Json.createObjectBuilder()
                    .add("message", "No such a subcluster")
                    .add("status", "ERROR")
                    .build();
        } else if (status == 3) {
            params = Json.createObjectBuilder()
                    .add("message", "The sub cluster already has the node.")
                    .add("status", "ERROR")
                    .build();
        }
        writer.writeObject(params);
        writer.close();
        baos.writeTo(outputStream);
        outputStream.write("\n".getBytes());
        outputStream.flush();

        System.out.println();
        if (params != null) {
            System.out.println("Response Sent -- " + params.toString());
            System.out.println("REPONSE STATUS: " + params.getString("status") + ", " + "message: " + params.getString("message"));
        } else {
            System.out.println("Response Sent");
        }
        
        if (status == 1) {
    		System.out.println("Beginning to push DHT to all physical nodes --- " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));

        	synchronized(this.cs) {
        		this.cs.initializeDataNode(this.cs.getRoot());
        	}
        }
    }

    public Cluster getRoot() {
        return root;
    }

    public void setRoot(Cluster root) {
        this.root = root;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getSubClusterId() {
        return subClusterId;
    }

    public void setSubClusterId(String subClusterId) {
        this.subClusterId = subClusterId;
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
