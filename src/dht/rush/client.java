package dht.rush;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dht.common.Hashing;
import dht.common.response.Response;
import dht.rush.clusters.Cluster;
import dht.rush.clusters.ClusterStructureMap;
import dht.server.Command;

public class client {
	private ClusterStructureMap clusterStructureMap;
	public static int hashRange;
	private String proxyIP;
	private int proxyPort;
	
	public void printTable() {
		if (clusterStructureMap == null) {
			System.out.println("Local DHT table is not initialized");
			return;
		}
		clusterStructureMap.print();
	}
	
	public boolean buildTable(JsonObject data) {
		clusterStructureMap = new ClusterStructureMap();
		return clusterStructureMap.buildTable(data);
	}
	
	public void printTableInfo() {
		int items = clusterStructureMap != null ? clusterStructureMap.getChildrenList().size() : 0;
		String epoch = items != 0 ? String.valueOf(clusterStructureMap.getEpoch()): "";
		System.out.println("A total of " + items + " hash buckets found in the table, epoch " + epoch);
	}
	
	public boolean isTableLatest(String epoch) {
		String localEpoch = clusterStructureMap != null ? String.valueOf(clusterStructureMap.getEpoch()): "";
		if (localEpoch.equals(epoch)) {
			return true;
		}
		else {
			return false;
		}
	}
	
    public ClusterStructureMap getLookupTable() {
        return clusterStructureMap;
    }
    
    public void setLookupTable(ClusterStructureMap map) {
    	this.clusterStructureMap = map;
    }
	
	public String getDHTEpoch() {
		String epoch = this.clusterStructureMap != null ? String.valueOf(this.clusterStructureMap.getEpoch()) : "";
		return epoch;
	}
	
	public List<String> findNodeInfo(int rawhash) {
		List<String> info = new ArrayList<String>();
		String pgid = "PG" + rawhash;
		if (clusterStructureMap != null) {
			for(Map.Entry<String, Cluster> root: clusterStructureMap.getChildrenList().entrySet()) {
				if (root.getKey().equals("R")) {
					Cluster cluster = root.getValue();
					findNodeInfo(cluster, pgid, info);
				}
			}
		}

		return info;
	}
	
	public void findNodeInfo(Cluster cluster, String pgid, List<String> info) {
		if (cluster == null) {
			return;
		}
		System.out.println("cluster " + cluster.getId());
		System.out.println(cluster.toJSON().toString());
		Map<String, Integer> hashMap = cluster.getPlacementGroupMap();
		if (hashMap.get(pgid) != null) {
			info.add(cluster.getIp() + "-" + cluster.getPort());
		}
		
		if (cluster.getSubClusters() != null) {
			for(Cluster child: cluster.getSubClusters()) {
				findNodeInfo(child, pgid, info);
			}
		}
	}
	
	public void initialize() {
        String rootPath = System.getProperty("user.dir");
//      String xmlPath = rootPath + File.separator + "src" + File.separator + "dht" + File.separator + "rush" + File.separator + "ceph_config.xml";

        String xmlPath = rootPath + File.separator + "dht" + File.separator + "rush" + File.separator + "ceph_config.xml";        File inputFile = new File(xmlPath);
        SAXReader reader = new SAXReader();
        
        Document config = null;
        try {
        	config = reader.read(inputFile);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        this.hashRange = Integer.valueOf(config.getRootElement().element("placementGroupNumber").getStringValue());
        this.proxyIP = config.getRootElement().element("proxy").element("ip").getStringValue();
        this.proxyPort = Integer.parseInt(config.getRootElement().element("proxy").element("port").getStringValue());
        
	}
	
    public static void main(String[] args) throws Exception {
        System.out.println("==== Welcome to Client !!! =====");
        
        int dhtType = 2;
        String dhtName = dhtType == 1 ? "DHT Ring" : dhtType == 2 ? "DHT Ceph" : dhtType == 3 ? "Elastic DHT" : "";
        client myclient = new client();
        myclient.initialize();
        
        if (args.length == 0) {
        	RWClient rwclient = new RWClient();
        	boolean connected = rwclient.connectServer(myclient.proxyIP, myclient.proxyPort, myclient, true);
    		
    		if (connected) {
    			System.out.println("Connected to " + dhtName + " Proxy Server ");
    			try {
					rwclient.processCommand(dhtType, "dht pull");
					rwclient.disconnectServer();
					System.out.println("Disconnected to " + dhtName + " Proxy Server ");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			
    		}
    		else {
    			System.out.println("Unable to connect to " + dhtName + " Proxy Server!");
    			return;
    		}
    		
    		Console console = System.console();
            while(true)
            {
            	String cmd = console.readLine("Input your command:");
                
            	try {
					rwclient.processCommand(dhtType, cmd);
				} catch (Exception e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
				}
            }
        }
        else {
          String serverAddress = "localhost";
        	int port = 0; 

        	if (args.length >= 2) {
        		serverAddress = args[0];
        		port = Integer.valueOf(args[1]);
        	}
        	
        	RWClient rwclient = new RWClient();
        	boolean connected = rwclient.connectServer(serverAddress, port, myclient, false);
    		
    		if (connected) {
    			System.out.println("Connected to " + dhtName + " Data Node");
    			rwclient.processCommand(dhtType, "dht pull");
    		}
    		else {
    			System.out.println("Unable to connect to " + dhtName + " Data Node!");
    			return;
    		}
    		
    		Console console = System.console();
            while(true)
            {
            	String cmd = console.readLine("Input your command:");
                
            	try {
					rwclient.processCommand(dhtType, cmd);
				} catch (Exception e) {
					// TODO Auto-generated catch block
//					e.printStackTrace();
				}
            }
        }
    }

}

class RWClient {
    PrintWriter output;
    BufferedReader input;
    InputStream inputStream;
    OutputStream outputStream;
	SocketAddress socketAddress;
	Socket socket;
	boolean isProxy;
	client myclient;
	
    public boolean connectServer(String serverAddress, int port, client myclient, boolean isProxy) {
    	int timeout = 2000;
		try {
			socketAddress = new InetSocketAddress(serverAddress, port);
			socket = new Socket();
			socket.connect(socketAddress, timeout);
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
			output = new PrintWriter(outputStream, true);
			input = new BufferedReader(new InputStreamReader(inputStream));
			this.isProxy = isProxy;
			this.myclient = myclient;

	        System.out.println("Connected to server " + serverAddress + ":" + port + ", with local port " + socket.getLocalPort());
//			socket.close();
			return true;
 
		} catch (SocketTimeoutException exception) {
//			socket.close();
			System.out.println("SocketTimeoutException " + serverAddress + ":" + port + ". " + exception.getMessage());
			return false;
		} catch (IOException exception) {
//			socket.close();
			System.out.println(
					"IOException - Unable to connect to " + serverAddress + ":" + port + ". " + exception.getMessage());
			return false;
		}
    }
    
    public void disconnectServer() {
    	try {
    		
    		this.input.close();
    		this.output.close();
    		this.socket.close();
    	}
    	catch (Exception e) {
//    		e.printStackTrace();
    	}
    }
    
    public void sendCommandStr(Command command, BufferedReader input, PrintWriter output) throws Exception {
    	String[] jsonCommands = {"read", "write", "data", "dht", "find", "info", "writebatch", "updatebatch"};
    	for(String jsonCommand: jsonCommands) {
    		if (command.getAction().equals(jsonCommand)) {
    			sendCommandStr_JsonRes(command, input, output);
    			return;
    		}
    	}

    	System.out.println("Sending command" + " ---- " + new Date().toString());
            output.println(command);
            output.flush();
        String response = input.readLine();
        System.out.println("Response received: " + response + " ---- " + new Date().toString());
    }
    
    public void sendCommandJson(Command command, BufferedReader input, PrintWriter output, boolean isProxy) throws Exception {
    	sendCommandJson(command, input, output, isProxy, false);
    }
    
    public void sendCommandJson(Command command, BufferedReader input, PrintWriter output, boolean isProxy, boolean skipLocal) throws Exception {
    	if (!skipLocal && parseLocalRequest(command)) {
    		return;
    	}

    	
    	JsonObject jobj = null;
    	if (isProxy && command.getAction().equals("dht")) {
                JsonObjectBuilder paramsBuilder = Json.createObjectBuilder()
                        .add("operation", command.getCommandSeries().get(0));
    		            if (command.getCommandSeries().size() > 1) {
    		            	JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
    		            	for(String item: command.getCommandSeries()) {
    		            		jsonArrayBuilder.add(item);
    		            	}
    		            	paramsBuilder.add("series", jsonArrayBuilder.build());
    		            }
                        
                jobj = Json.createObjectBuilder()
                        .add("method", "dht")
                        .add("parameters", paramsBuilder.build())
                        .build();
    	}
    	else {
    		jobj = new Response(true, command.getRawCommand(), command.getRawCommand()).toJSON();
    	}
    	
       	String timeStamp = new Date().toString();
    	System.out.println("Sending command" + " ---- " + timeStamp);
    	System.out.println(jobj.toString());
    	
    	output.println(jobj.toString());
    	output.flush();
        
        JsonObject res = parseRequest(input);
        if (res != null) {
            System.out.println();
            System.out.println("Response received at " + timeStamp);
            System.out.println(res.toString());
            parseResponse(res, command, input, output);
        	
        	System.out.println();
         }
    }
    
    public void sendCommandStr_JsonRes(Command command, BufferedReader input, PrintWriter output) throws Exception {
    	if (parseLocalRequest(command)) {
    		return;
    	}
    	
    	String timeStamp = new Date().toString();
    	System.out.println("Sending command" + " ---- " + timeStamp);
    	
        output.println(command.getRawCommand());
        output.flush();
        
        JsonObject res = parseRequest(input);
        if (res != null) {
            System.out.println();
            System.out.println("Response received at " + timeStamp);
            parseResponse(res, command, input, output);
        	
        	System.out.println();
         }
    }
    
    public boolean parseLocalRequest(Command command) {
    	boolean processed = false;
    	if (command.getAction().equals("dht")) {
    		if (command.getCommandSeries().size() > 0 && command.getCommandSeries().get(0).equals("info")) {
    			System.out.println("Epoch Number of Local DHT is " + this.myclient.getDHTEpoch());
    			processed = true;
    		}
    		else if (command.getCommandSeries().size() > 0 && command.getCommandSeries().get(0).equals("print")) {
    			this.myclient.printTable();
    			processed = true;
    		}
    	}
    	else if ((command.getAction().equals("read") ||command.getAction().equals("write")) && command.getCommandSeries().size() > 0) {
			String dataStr = command.getCommandSeries().get(0);
			
			if (dataStr.equals("auto")) {
				int count = 0;
				while(count < 100) {
					// try {
					// 	// Thread.sleep(1000);
					// } catch (InterruptedException e) {
					// 	// TODO Auto-generated catch block
					// 	e.printStackTrace();
					// }
					Random ran = new Random();
					int ranhash = ran.nextInt(this.myclient.hashRange);
					List<String> nodeinfo = this.myclient.findNodeInfo(ranhash);
					String message = dataStr + " (hash value: " + ranhash + ") can be found in Data Node " + Arrays.toString(nodeinfo.toArray());
					System.out.println(message);
					
					for(String node: nodeinfo) {
						try {
							String ip = node.split("-")[0];
							int port = Integer.valueOf(node.split("-")[1]);
							boolean connected = connectServer(ip, port, this.myclient, false);
							if (connected) {
								sendCommandJson(command, this.input, this.output, false, true);
//								disconnectServer();
							}
							
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					count++;
				}
			}
			else {
				int rawhash = Hashing.getHashValFromKeyword(dataStr, this.myclient.hashRange);
				try {
					if (rawhash < this.myclient.hashRange) {
						rawhash = Integer.valueOf(dataStr);
					}
				}
				catch (Exception e) {
					
				}
				List<String> nodeinfo = this.myclient.findNodeInfo(rawhash);
				String message = dataStr + " (hash value: " + rawhash + ") can be found in Data Node " + Arrays.toString(nodeinfo.toArray());
				System.out.println(message);
				
				for(String node: nodeinfo) {
					try {
						String ip = node.split("-")[0];
						int port = Integer.valueOf(node.split("-")[1]);
						boolean connected = connectServer(ip, port, this.myclient, false);
						if (connected) {
							sendCommandJson(command, this.input, this.output, false, true);
//							disconnectServer();
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			

			
			processed = true;
    		
    	}
    	return processed;
    }
    
    public void parseResponse(JsonObject res, Command command, BufferedReader input, PrintWriter output) throws Exception {
    	if (command.getAction().equals("info")) {
    		String message = "Data Epoch number: ";
    		if (res.containsKey("message") ) {
    			message = res.getString("message");
    		}
    		if (res.containsKey("result")) {
    			System.out.println(message + " --- " + res.getString("result"));
    		}
    	}
    	else if (command.getAction().equals("dht")) {
    		if (command.getCommandSeries().size() > 0 && command.getCommandSeries().get(0).equals("pull")) {
    			this.myclient.buildTable(res.getJsonObject("jsonResult"));
    			System.out.println("Local DHT built, with epoch number " + this.myclient.getDHTEpoch());
    		}
    	}
    	else if (command.getAction().equals("read") || command.getAction().equals("write")) {
    		if (res.containsKey("result")) {
    			long remoteEpoch = Long.parseLong(res.getString("result"));
    			long localEpoch = Long.parseLong(this.myclient.getDHTEpoch());
    			if (remoteEpoch > localEpoch) {
    				sendCommandJson(new Command("dht pull"), input, output, false);
    			}
    			
    		}
    	}
    }
	
    public static JsonObject parseRequest(BufferedReader br) throws Exception {
        String str;
        JsonObject jsonObject = null;

        while ((str = br.readLine()) != null) {
            JsonReader jsonReader = Json.createReader(new StringReader(str));
            jsonObject = jsonReader.readObject();
            return jsonObject;
        }
        return jsonObject;
    }
    
    public void processCommandRush(String cmd) throws Exception {
    	Command command = new Command(cmd);
    	
    	String timeStamp = new Date().toString();
    	System.out.println("Sending command" + " ---- " + timeStamp);
    	System.out.println();
        
        JsonObject params = null;
        JsonObject jobj = null;
		if(command.getAction().equals("addnode")) {
			  params = Json.createObjectBuilder()
			  .add("subClusterId", command.getCommandSeries().get(0))
			  .add("ip", command.getCommandSeries().get(1))
			  .add("port", command.getCommandSeries().get(2))
			  .add("weight", command.getCommandSeries().get(3))
			  .build();
			
			  jobj = Json.createObjectBuilder()
			  .add("method", "addnode")
			  .add("parameters", params)
			  .build();
		}
		else if(command.getAction().equals("deletenode")) {
	          params = Json.createObjectBuilder()
	          .add("subClusterId", command.getCommandSeries().get(0))
	          .add("ip", command.getCommandSeries().get(1))
	          .add("port", command.getCommandSeries().get(2))
	          .build();
	
	          jobj = Json.createObjectBuilder()
	          .add("method", "deletenode")
	          .add("parameters", params)
	          .build();
		}
		else if(command.getAction().equals("getnodes")) {
            params = Json.createObjectBuilder()
                    .add("pgid", command.getCommandSeries().get(0))
                    .build();

            jobj = Json.createObjectBuilder()
                    .add("method", "getnodes")
                    .add("parameters", params)
                    .build();
		}
		else if (command.getAction().equals("help")) {
			System.out.println(getHelpText(2));
			return;
		}
		else if (command.getAction().equals("dht")) {
			
		}
		else {
			System.out.println("command not supported");
			return;
		}
    	
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter writer = Json.createWriter(baos);
        writer.writeObject(jobj);
        writer.close();
        baos.writeTo(outputStream);

        outputStream.write("\n".getBytes());
        outputStream.flush();

        JsonObject res = parseRequest(input);
        if (res != null) {
            System.out.println();
        	System.out.println("Response received at " + timeStamp + " ---- " + res.toString());
            if (res.containsKey("status") && res.containsKey("message")) {
                System.out.println("REPONSE STATUS: " + res.getString("status") + ", " + "message: " + res.getString("message"));
            }
            
            parseResponse(res, command, input, output);
            
            System.out.println();
         }
    }
    
    public void processCommandRing(String cmd, int dhtType) throws Exception {
        Command command = new Command(cmd);
        if(command.getAction().equals("help"))
        {
            System.out.println(getHelpText(dhtType));
        }
        else if(command.getAction().equals("exit"))
        {
            System.exit(0);
        }
        else
        {
//        	if (isProxy) {
////        		sendCommandStr_JsonRes(command, input, output);
//        		sendCommandJson(command, input, output, true);
//        	}
//        	else {
//        		sendCommandJson(command, input, output, false);
//        	}
        	
        	sendCommandJson(command, input, output, isProxy);
        }
    }
    
    public void processCommandRing(String cmd) throws Exception {
    	processCommandRing(cmd, 1);
    }
    
    public void processCommandElastic(String cmd) throws Exception {
    	processCommandRing(cmd, 3);
    }
    
    public void processCommand(int typeDHT, String cmd) throws Exception {
    	switch(typeDHT) {
	    	case 1:
	    		processCommandRing(cmd);
	    		break;
	    	case 2:
//	    		processCommandRush(cmd);
	    		processCommandRing(cmd, 2);
	    		break;
	    	case 3:
	    		processCommandElastic(cmd);
	    		break;
    	}
    }
    
    public static String getHelpText(int dhtType) {
    	String tip = "";
    	switch(dhtType) {
	    	case 1:
	    		tip = "\nhelp";
	    		tip += "\nfind <hash>    //find the virtual node on the server corresponding to the hash value";
	    		tip += "\ndht head|pull  //fetch server dht table info";
	    		tip += "\ndht info|print  //show local dht table info";
	    		tip += "\ninfo           //show server dht table info";
	    		tip += "\nexit\n";
	    		break;
	    	case 2:
	    		tip = "\nhelp";
	    		tip += "\naddnode <subClusterId> <IP> <Port> <weight>  //example: addnode S0 localhost 689 0.5";
	    		tip += "\ndeletenode <subClusterId> <IP> <Port>  //example: deletenode S0 localhost 689";
	    		tip += "\ngetnodes <pgid> | example: getnodes PG1";
	    		tip += "\ninfo";
	    		tip += "\nexit\n";
	    		break;
	    	case 3:
	    		tip = "\nhelp";
	    		tip += "\nread <randomStr>";
	    		tip += "\nfind <hash>    //find the virtual node on the server corresponding to the hash value";
	    		tip += "\ndht head|pull  //fetch server dht table info";
	    		tip += "\ndht info|print  //show local dht table info";
	    		tip += "\ninfo           //show server dht table info";
	    		tip += "\nexit\n";
	    		break;
    	}
    	
    	return tip;
    }
}
