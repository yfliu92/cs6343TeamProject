package dht.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import dht.common.Context;
import dht.common.request.Request;
import dht.common.request.RequestReader;

public class RequestRouter extends Thread {
	    private Socket socket;
	    private RequestMap map;
	    
	    public RequestRouter(Socket socket, RequestMap map) {
	        this.socket = socket;
	        this.map = map;
	        System.out.println("Opening request router...");
	    }
	    
	    public void run() {
	        try {
	        	Context context = Context.getInstance();
	            BufferedReader in = new BufferedReader(
	                    new InputStreamReader(socket.getInputStream()));

	            // Send a welcome message to the client.
	            System.out.println("Request received...");
	            
	            while (context.isRunning()) {
	                String input = in.readLine();
	                if (input == null || input.equals(".")) {
	                    break;
	                }
	                System.out.println(input);
	                Request req = RequestReader.readRequest(input);
	                ByteArrayOutputStream response = null;
	                try {
	                	response = map.Execute(req).toByteStream();
	                } catch(NoSuchMethodException e) {
	                	System.err.println(e.getLocalizedMessage());
	                }
	                
	                if(req == null || response == null)
	                {
	                	response = new ByteArrayOutputStream();
            			response.write("{\"status\":\"error\",\"message\":\"Could not parse request\"}".getBytes());
            			String error_type = (req == null) ? "1" : "0";
            			System.err.println("RequestRouter.run: No response provided by req " + error_type);
	                }
	                
	                response.write("\n".getBytes());
	                response.writeTo(socket.getOutputStream());
	                response.close();
	            }
	        } catch (IOException e) {
	        	System.out.println("++ Could not read socket ++");
	        } finally {
	            try {
	                socket.close();
	            } catch (IOException e) {
	                log("?? Couldn't close a socket, what's going on ??");
	            }
	            System.out.println("-- Socket closed --");
	        }
	    }

	    /**
	     * Logs a simple message.  In this case we just write the
	     * message to the server applications standard output.
	     */
	    private void log(String message) {
	        System.out.println(message);
	    }
}
