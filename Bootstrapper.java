import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Bootstrapper {
	private static final String CONFIG_FILE = "../config";
	private static final int NUM_OF_PEERS = 5;

	public static void main(String[] args) throws Exception {
		int bootstrapPort = 8050;
		ServerSocket listener = new ServerSocket(bootstrapPort);
		System.out.println("The bootstrap server is running."); 
		int clientNum = 0;
		try {
        	while(clientNum != NUM_OF_PEERS) {
        	new Handler(listener.accept(),clientNum).start();
				// System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
        	}
    	} finally {
    		// System.out.println("\nAll chunks are successfully distributed to the peers! ");
        	listener.close();
    	} 
	}

	private static class Handler extends Thread {
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int num;		//The index number of the client

		public Handler(Socket connection, int num) {
	    	this.connection = connection;
			this.num = num;
		}

		public void run(){
				

			try{
	 			//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				// Get the peer number
				int peer_num = Integer.parseInt((String)in.readObject());
				int listeningport = Integer.parseInt((String)in.readObject());
				System.out.println("\nClient "+peer_num+" is connected!");
				System.out.println("Client "+peer_num+" listening port is "+listeningport);
				BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
				int line = 0;
				String text;
				while ((text=br.readLine())!=null){
				    if(!text.equals("")){//Ommit Empty lines
				    	line++;
			    		String[] strArray = text.split(" ");
			    		// System.out.println(Arrays.toString(strArray));
			    		if(Integer.parseInt(strArray[0]) == peer_num){
			    			int downloadPort = Integer.parseInt(strArray[2]);
			    			System.out.println("Sending download port to Client "+peer_num);
			        		out.writeObject(Integer.toString(downloadPort));
			    		}
				    }
				}
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + num);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + num);
				}
			}
		}
	}
}