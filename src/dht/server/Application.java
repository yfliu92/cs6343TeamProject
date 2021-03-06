package dht.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import dht.common.Configuration;
import dht.server.impl.SingleServer;

public class Application {
	
	public static void main(String[] args) {
		System.out.println("Opening application");
		
		Options options = Configuration.buildOptions();
		CommandLineParser parser = new DefaultParser();
		Configuration config = Configuration.getInstance();
		
		try {
			CommandLine cmd = parser.parse( options, args);
			config.setConfiguration(cmd);
			
			BaseServer server = null;
			
			// TODO: Add new modes as they are available
			switch(config.getMode()) {
				case "single":
				default:
					server = new SingleServer(config);
					break;	
			}
			
			server.buildRouting();
			server.run();
		} catch (ParseException e) {
			// Print help message if we can't understand command line options
			System.err.println("FATAL: Unable to parse command line: " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "DHTServer", options );
			return;
		}
	}
}
