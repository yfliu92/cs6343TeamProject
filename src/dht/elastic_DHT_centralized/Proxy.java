package dht.elastic_DHT_centralized;


import java.sql.Timestamp;
import java.util.*;

public class Proxy{
    private String id = "PROXY";

    private String ip;

    private int port;

    private LookupTable lookupTable;

    public Proxy(){
    }
    public Proxy (String ip, int port){
        this.ip = ip;
        this.port = port;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public LookupTable getLookupTable() {
        return lookupTable;
    }

    public void setLookupTable(LookupTable lookupTable) {
        this.lookupTable = lookupTable;
    }

    public String dataTransfer(String fromID, String toID, int hash){
//        return "\nFrom " + fromID + " to " + toID + ": transferring data for bucket " + Integer.toString(hash);
        return "  From " + fromID + " to " + toID + ": transferring data for bucket " + Integer.toString(hash);

    }

    public String listNodes() {
        System.out.println(lookupTable);
        HashMap<Integer, HashMap<String, String>> map = lookupTable.getBucketsTable();
        StringBuilder result = new StringBuilder();
        result.append("Existing buckets (" + map.size() + "): ");

        for(HashMap.Entry<Integer, HashMap<String, String>> entry: map.entrySet()) {
//            result.append("\nBucket " + entry.getKey() + ", size " + entry.getValue().size());
            result.append("  Bucket " + entry.getKey() + ", size " + entry.getValue().size());

            for(HashMap.Entry<String, String> subentry: entry.getValue().entrySet()) {
//    			result.append("\n    key " + subentry.getKey() + ", value " + subentry.getValue());
                result.append("    " + subentry.getKey());
            }
        }

        return result.toString();
    }

    // Add a physical node without specifying for what range of buckets
    public String addNode(String ip, int port){
        // ID of the new node will be automatically created by the PhysicalNode constructor
        PhysicalNode newNode = new PhysicalNode(ip, port, "active");
        String id = newNode.getId();
        lookupTable.getPhysicalNodesMap().put(id, newNode);
        int loadPerNode = (lookupTable.getBucketsTable().size() / lookupTable.getPhysicalNodesMap().size());
        // Use this new node as a replica for the first loadPerNode buckets in the bucketsTable
        String result = "";
        for (int i = 0; i < loadPerNode; i++){
            HashMap<String, String> replicas = lookupTable.getBucketsTable().get(i);
            String fromID = replicas.entrySet().iterator().next().getKey();
            replicas.put(id, id);
            result += dataTransfer(fromID, id, i);
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        lookupTable.setEpoch(timestamp.getTime());
        // Proxy updates the lookupTable of all physical nodes
        for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
            pNode.setLookupTable(lookupTable);
        }

//        return "\ntrue|node " + id + " has been added successfully: " + result;
        return "  true|node " + id + " has been added successfully: " + result;
    }

    // Add a physical node, clearly specify for what range of buckets it will serve as a replica
    public String addNode(String ip, int port, int start, int end){
        PhysicalNode newNode = new PhysicalNode(ip, port, "active");
        String id = newNode.getId();
        lookupTable.getPhysicalNodesMap().put(id, newNode);
        String result = "";
        if (start < end){
            for (int i = start; i < end; i++){
                HashMap<String, String> replicas = lookupTable.getBucketsTable().get(i);
                String fromID = replicas.entrySet().iterator().next().getKey();
                replicas.put(id, id);
                newNode.getHashBuckets().add(i);
                result += dataTransfer(fromID, id, i);
            }
        }
        else{
            for (int i = start; i < ProxyServer.INITIAL_HASH_RANGE; i++){
                HashMap<String, String> replicas = lookupTable.getBucketsTable().get(i);
                String fromID = replicas.entrySet().iterator().next().getKey();
                replicas.put(id, id);
                newNode.getHashBuckets().add(i);
                result += dataTransfer(fromID, id, i);
            }
            for (int i = 0; i < end; i++){
                HashMap<String, String> replicas = lookupTable.getBucketsTable().get(i);
                String fromID = replicas.entrySet().iterator().next().getKey();
                replicas.put(id, id);
                newNode.getHashBuckets().add(i);
                result += dataTransfer(fromID, id, i);
            }

        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        lookupTable.setEpoch(timestamp.getTime());
        // Proxy updates the lookupTable of all physical nodes
        for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
            pNode.setLookupTable(lookupTable);
        }

//        return "\ntrue|node " + id + " has been added successfully: " + result;
        return "  true|node " + id + " has been added successfully: " + result;
    }
    public String deleteNode(String ip, int port) {
        String nodeID = ip + "-" + Integer.toString(port);

        // Try to remove this physical node from the physicalNodesMap
        // The node doesn't exit if .remove() returns null
        
        try {
            PhysicalNode node = lookupTable.getPhysicalNodesMap().remove(nodeID);
            node.setStatus("inactive");
            if (node == null){
//                return "\nfalse|This node doesn't exist!";
                return "  false|This node doesn't exist!";
            }

            // Get a list of all physical node ids
            Set<String> idSet = lookupTable.getPhysicalNodesMap().keySet();
            List<String> idList = new ArrayList<>(idSet);
            String toID = "";

            // Get the bucketsTable
            HashMap<Integer, HashMap<String, String>> table = lookupTable.getBucketsTable();
            String result = "";
            HashSet<Integer> buckets = node.getHashBuckets();
            Iterator<Integer> iter = buckets.iterator();
            while (iter.hasNext()){
                int idx = iter.next();
                iter.remove();
                // Remove this node ID from the bucket's replicas
                table.get(idx).remove(nodeID);
                // Since the node is deleted/failed, must find another existing replica to transfer data to the newly added replica
                String fromID = table.get(idx).entrySet().iterator().next().getKey();
                // Randomly pick a physical node to serve as the new replica
                String returnedValue;
                do {
                    Random ran = new Random();
                    toID = idList.get(ran.nextInt(idSet.size()));
                    returnedValue = table.get(idx).put(toID, toID);
                } while (returnedValue != null);
                lookupTable.getPhysicalNodesMap().get(toID).getHashBuckets().add(idx);
                result += (dataTransfer(fromID, toID, idx));
            }

            // Update the epoch time
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            lookupTable.setEpoch(timestamp.getTime());
            // Proxy updates the lookupTable of all physical nodes
            for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
                pNode.setLookupTable(lookupTable);
            }

            return "  true|node " + nodeID + " has been successfully removed: " + result;
//            return "\ntrue|node " + nodeID + " has been successfully removed: " + result;
        }
        catch (Exception e) {
            return "  false|This node doesn't exist!";
        }
 
    }

    public String loadBalance(String fromIP, int fromPort, String toIP, int toPort, int numOfBuckets){
        String fromID = fromIP + "-" + Integer.toString(fromPort);
        String toID = toIP + "-" + Integer.toString(toPort);
        // Check if the Physical node of fromID exists or not
        PhysicalNode fromNode = lookupTable.getPhysicalNodesMap().get(fromID);
        if (fromNode == null){
            return "false|This node of " + fromID + "doesn't exist!";
        }
        PhysicalNode toNode = lookupTable.getPhysicalNodesMap().get(toID);
        if (toNode == null){
            // Create a new physical node of toID
            PhysicalNode newNode = new PhysicalNode(toIP, toPort, "active");
            lookupTable.getPhysicalNodesMap().put(toID, newNode);
        }
        // Get the bucketsTable
        HashMap<Integer, HashMap<String, String>> table = lookupTable.getBucketsTable();
        int count = Math.min(fromNode.getHashBuckets().size(), numOfBuckets);
        String result = "";

        HashSet<Integer> bucketsOfFromNode = fromNode.getHashBuckets();
        HashSet<Integer> bucketsOfToNode = toNode.getHashBuckets();
        Iterator<Integer> iter = bucketsOfFromNode.iterator();
        for (int i = 0; i < count; i++){
            Integer idx = iter.next();
            iter.remove();
            bucketsOfToNode.add(idx);
            // Remove this node ID from the bucket's replicas
            table.get(idx).remove(fromID);
            table.get(idx).put(toID, toID);
            result += dataTransfer(fromID, toID, idx) + " ";
        }
        // Update the epoch number
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        lookupTable.setEpoch(timestamp.getTime());
        // Proxy updates the lookupTable of all physical nodes
        for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
            pNode.setLookupTable(lookupTable);
        }
//        return "\ntrue|loadBalance from " + fromID + " to " + toID + " for " + numOfBuckets + " buckets: " + result;

        return "  true|loadBalance from " + fromID + " to " + toID + " for " + numOfBuckets + " buckets: " + result;
    }
    public String expandTable(){
        int oldHashRange = ProxyServer.CURRENT_HASH_RANGE;
        int newHashRange = oldHashRange * 2;
        for (int i = oldHashRange; i < newHashRange; i++){
            lookupTable.getBucketsTable().put(i, lookupTable.getBucketsTable().get(i - oldHashRange));
        }
        ProxyServer.CURRENT_HASH_RANGE *= 2;
        // Update the timestamp
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        lookupTable.setEpoch(timestamp.getTime());
        // Update the lookupTable of all physical nodes
        for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
            pNode.setLookupTable(lookupTable);
        }
//        return "\ntrue|expandTable from " + oldHashRange + " buckets to " + newHashRange + " buckets is successful.";

        return "  true|expandTable from " + oldHashRange + " buckets to " + newHashRange + " buckets is successful.";
    }
    public String shrinkTable(){
        int oldHashRange = ProxyServer.CURRENT_HASH_RANGE;
        if (oldHashRange <= ProxyServer.INITIAL_HASH_RANGE){
//            return "\nfalse|Shrunk cannot be done beyond the original table.";
        	return "  false|Shrunk cannot be done beyond the original table.";
        }
        int newHashRange = oldHashRange / 2;
        for (int i = newHashRange; i < oldHashRange; i++){
            lookupTable.getBucketsTable().remove(i);
        }
        ProxyServer.CURRENT_HASH_RANGE /= 2;
        // Update the timestamp
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        lookupTable.setEpoch(timestamp.getTime());
        // Update the lookupTable of all physical nodes
        for (PhysicalNode pNode: lookupTable.getPhysicalNodesMap().values()){
            pNode.setLookupTable(lookupTable);
        }
//        return "\ntrue|shrinkTable from " + oldHashRange + " buckets to " + newHashRange + " buckets is successful.";
        return "  true|shrinkTable from " + oldHashRange + " buckets to " + newHashRange + " buckets is successful.";
    }

    // Return a set of nodeIDs for replicas for each hash value
    public Set<String> getReplicas(int hash){
        Set<String> replicas = lookupTable.getBucketsTable().get(hash).keySet();
        return replicas;
    }

}

