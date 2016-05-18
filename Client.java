import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Client {
	private static final int PEER_NUMBER = 1;
	private static final String CONFIG_FILE = "../config";
	private static final int CHUNK_SIZE = 100000;
	private static final int NUM_OF_PEERS = 5;
	private Lock lock = new ReentrantLock();
	Socket requestSocket;        // socket connect to the server
	ObjectOutputStream out;      // stream write to the socket
 	BufferedInputStream in;      // stream read from the socket
 	int uploadPort;			 	// port number of peer to which you upload chunks
 	int downloadPort;			 // port number of peer from which you download chunks
 	int serverPort;			 	// server port number to download it chunks
 	int totalChunks;			// number of chunks the original file is made of
 	int fileSize;				// size of the file to be distributed
 	int bootstrapPort = 8050;
 	ArrayList<Integer> chunksList = new ArrayList<>();

	public void Client() {}

	void run()
	{
		try{
			// Get server, upload and download port numbers from config file
			BufferedReader br = new BufferedReader(new FileReader(CONFIG_FILE));
			String text;
			int sPort = 0;
			int line = 0;
			while ((text=br.readLine())!=null){
			    if(!text.equals("")){//Ommit Empty lines
			    	line++;
			    	if(line == 1){
			    		String[] strArray = text.split(" ");
			        	serverPort = Integer.parseInt(strArray[1]);
			    	}
		    		String[] strArray = text.split(" ");
		    		if(Integer.parseInt(strArray[0]) == PEER_NUMBER){
		    			uploadPort = Integer.parseInt(strArray[1]);
		        		//downloadPort = Integer.parseInt(strArray[2]);
		    		}
			    }
			}

			//create a socket to connect to the server
			requestSocket = new Socket("localhost", serverPort);
			System.out.println("\nConnected to Server in port "+serverPort);

			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new BufferedInputStream(requestSocket.getInputStream());

			// Tell server about your peer number
			out.writeObject(Integer.toString(PEER_NUMBER));
			out.flush();

			int i = PEER_NUMBER;
	  		int bytesRead = 0;

	  		// Get total number of chunks
			byte [] barray1  = new byte [4];
			in.read(barray1,0,barray1.length);
			totalChunks = ByteBuffer.wrap(barray1).getInt();

			// Get total file size
			byte [] barray2  = new byte [4];
			in.read(barray2,0,barray2.length);
			fileSize = ByteBuffer.wrap(barray2).getInt();

			int chunk = 0;
			int tempFileSize = fileSize;
			System.out.println("\nDownloading chunks from the server !!!");
	      	do {
	      		if((tempFileSize-CHUNK_SIZE) < 0){
	      			chunk = tempFileSize;
	      			tempFileSize -= chunk;
	      		} else {
	      			chunk = CHUNK_SIZE;
	      			tempFileSize -= chunk;
	      		}
		      	byte [] chunkbytearray  = new byte [chunk];
		        	bytesRead = in.read(chunkbytearray,0,chunkbytearray.length);

		        	if (bytesRead > 0){
		        		FileOutputStream fos = new FileOutputStream("download/chunks/Chunk"+i);
		  				BufferedOutputStream bos = new BufferedOutputStream(fos);
		        		bos.write(chunkbytearray,0,bytesRead);
			      		bos.flush();
			      			chunksList.add(i);
			      		System.out.println("Downloaded" + "Chunk"+i
			          	+ "(" + bytesRead + " bytes) from Server");
			      		i = i + NUM_OF_PEERS;
		        	}
	      	} while(bytesRead > 0);
	      	// Connect to bootstrap server
	  		requestSocket = new Socket("localhost", bootstrapPort);
			System.out.println("\nConnected to Bootstrap Server in port "+bootstrapPort);

			ObjectOutputStream out1 = new ObjectOutputStream(requestSocket.getOutputStream());
			out1.flush();
			ObjectInputStream in1 = new ObjectInputStream(requestSocket.getInputStream());

			out1.writeObject(Integer.toString(PEER_NUMBER));
			out1.flush();
			out1.writeObject(Integer.toString(uploadPort));
			out1.flush();

			downloadPort = Integer.parseInt((String)in1.readObject());
			System.out.println("Download port is "+downloadPort);

	      	// Spawn a thread to handle download neighbour
	      	downloadHandler dh = new downloadHandler(downloadPort, PEER_NUMBER, totalChunks, fileSize);
	      	dh.setName("download_thread");
	      	dh.start();

	      	// Spawn a thread to handle upload neighbour
	      	uploadHandler uh = new uploadHandler(uploadPort, PEER_NUMBER, totalChunks);
	     	uh.setName("upload_thread");
	     	uh.start();
		}catch(ClassNotFoundException classnot){
			System.err.println("Data received in unknown format");
		}
		catch (ConnectException e) {
    		System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

	private class uploadHandler extends Thread {
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private BufferedOutputStream out;    //stream write to the socket
		private int num;		//The index number of the client
		private int uploadPort;
		private int totalChunks;
		private int upload_num;

		public uploadHandler(int uploadPort, int num, int totalChunks) {
			this.num = num;
			this.uploadPort = uploadPort;
			this.totalChunks = totalChunks;
			if(this.num == 5){
				upload_num = 1;
			} else {
				upload_num = this.num + 1;
			}
		}

		@SuppressWarnings("unchecked")
		public void run() {
	 		try{
	 			System.out.println("\nStarting upload sevrer!!");
				ServerSocket upload = new ServerSocket(uploadPort);
		        connection = upload.accept();
		        System.out.println("peer "  + upload_num + " is connected!");

		        //initialize Input and Output streams
				out = new BufferedOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				boolean firstTime = true;
				// Get the chunklist from the neighbour
				ArrayList<Integer> nchunksList = (ArrayList<Integer>)in.readObject();
				int nchunksListSize = nchunksList.size();
				System.out.println("\nChunklist from neighbour "+upload_num+": "+nchunksList);
				
				// If the chunklist contains all the chunks, all DONE!!!
				while(nchunksListSize != totalChunks){
					if(firstTime == true){
						firstTime = false;
					} else {
						// Get the chunkList from the neighbour
						nchunksList = (ArrayList<Integer>)in.readObject();
						nchunksListSize = nchunksList.size();
						System.out.println("\nChunklist from neighbour "+upload_num+": "+nchunksList);
					}
					
					int i = 0;
					int chunksListSize = 0;
					lock.lock();
	      			try {
	      				chunksListSize = chunksList.size();
	      			} finally {
	      				lock.unlock();
	      			}

	      			while(i < chunksListSize){
	      				lock.lock();
	      				try {
	      					chunksListSize = chunksList.size();
	      					// Find the chunks not present in the list and send them
			 				if(nchunksList.contains(chunksList.get(i))){
			 					// Dont send the chunk
			 				} else {
			 					// Send the chunk
			 					File myFile = new File ("download/chunks/Chunk"+chunksList.get(i));
				  				byte [] mybytearray  = new byte [(int)myFile.length()];
				  				FileInputStream fis = new FileInputStream(myFile);
				          		BufferedInputStream bis = new BufferedInputStream(fis);
				          		bis.read(mybytearray,0,mybytearray.length);
				          		
				          		// Send the chunk number
				          		// System.out.println("chunk number = "+chunksList.get(i));
				          		out.write(ByteBuffer.allocate(4).putInt(chunksList.get(i)).array(),0,4);
				          		out.flush();
				          		// Send the chunk file size
				          		// System.out.println("chunk_size = "+(int)myFile.length());
				          		out.write(ByteBuffer.allocate(4).putInt((int)myFile.length()).array(),0,4);
				          		out.flush();
				          		// Send the actual chunk bytes
				          		System.out.println("Sending " + "Chunk"+chunksList.get(i)+
				          			"("+mybytearray.length+" bytes) to the upload neighbour "+upload_num);
				          		out.write(mybytearray,0,mybytearray.length);
				          		out.flush();
			 				}
							i++;
						} finally {
			      			lock.unlock();
			      		}
			 		}
		 			out.write(ByteBuffer.allocate(4).putInt(0).array(),0,4);
			      	out.flush();
				}
				System.out.println("\nUpload neighbour has all the chunks!");; 
				System.out.println("Shutting down the Upload Server!"); 
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + upload_num);
				// ioException.printStackTrace();
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + upload_num);
					// ioException.printStackTrace();
				}
			}
		}

    }

    private class downloadHandler extends Thread {
		private Socket downloadSocket;
		private BufferedInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int num;			//The index number of the client
		private int downloadPort;	// download port to be connected
		private int totalChunks;	// total chunks of the file
		private int fileSize;		// file size
		private int download_num;

		public downloadHandler(int downloadPort, int num, int totalChunks, int fileSize) {
			this.num = num;
			this.downloadPort = downloadPort;
			this.totalChunks = totalChunks;
			this.fileSize = fileSize;
			if(this.num == 1){
				download_num = 5;
			} else {
				download_num = this.num - 1;
			}
		}

		public void run() {
	 		try{
				//create a socket to connect to the download peer
				downloadSocket = null;
		        while(true) {
		            try {
		                downloadSocket = new Socket("localhost", downloadPort);
		            } catch (ConnectException ignore) {
		            	System.out.print("Download neighbour is still not up!!");
		            	System.out.println(" Waiting for 5 seconds!!");
		            }
		            if(downloadSocket == null){
		            	try {
						    Thread.sleep(5000);      //1000 milliseconds is one second.
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
		            } else {
		            	break;
		            }
		        }
				System.out.println("\nConnected to download neighbour in port "+downloadPort);
				//initialize Input and Output streams
				out = new ObjectOutputStream(downloadSocket.getOutputStream());
				out.flush();
				in = new BufferedInputStream(downloadSocket.getInputStream());

				int chunksListSize = 0;
				lock.lock();
      			try {
      				chunksListSize = chunksList.size();
      			} finally {
      				lock.unlock();
      			}

				while(chunksListSize != totalChunks){
					//Send the chunksList 
					lock.lock();
	      			try {
	      				chunksListSize = chunksList.size();
	      				out.writeObject(chunksList);
						out.flush();
						out.reset();
	      			} finally {
	      				lock.unlock();
	      			}
					//Download the chunks
					int i = 0;
			  		int bytesRead = 0;
			  		int chunk_size = 0;
			  		int totalBytes = 0;
			  		boolean newChunk = true;
			      	do {
			      		if(newChunk == true){
			      			byte [] barray1  = new byte [4];
			      			bytesRead = in.read(barray1,0,barray1.length);
			      			
			      			if(bytesRead > 0){
			      				i = ByteBuffer.wrap(barray1).getInt();
			      				// System.out.println("chunk number = "+i);
			      				if(i == 0) break;

				      			byte [] barray2  = new byte [4];
				      			bytesRead = in.read(barray2,0,barray2.length);
				      			chunk_size = ByteBuffer.wrap(barray2).getInt();
				        	} else {
				        		break;
				        	}
					        newChunk = false; 
			      		}
			      		// System.out.println("chunk_size = "+chunk_size);
			      		byte [] chunkbytearray  = new byte [chunk_size];

			      		while (chunk_size > 0 && (bytesRead = in.read(chunkbytearray, 0, chunk_size)) != -1) {
                            chunk_size -= bytesRead;
                        }
			        	// bytesRead = in.read(chunkbytearray,0,(chunkbytearray.length)-totalBytes);
			        	// totalBytes = totalBytes+bytesRead;

			        	// if (bytesRead > 0 && totalBytes == chunk_size){
			        		FileOutputStream fos = new FileOutputStream("download/chunks/Chunk"+i);
			  				BufferedOutputStream bos = new BufferedOutputStream(fos);
			        		bos.write(chunkbytearray,0,bytesRead);
				      		bos.flush();
				      		lock.lock();
				      		try {
				      			chunksList.add(i);
				      		} finally {
				      			lock.unlock();
				      		}
				      		System.out.println("Downloaded " + "Chunk"+i
				          	+ "(" + bytesRead + " bytes) from the download neighbour "+download_num);
				      		newChunk = true;
			        	// }
			      	} while(bytesRead > 0 || newChunk == true);
				}

				// Combine all the chunks
				System.out.println("\nAll the chunks downloaded!\nCombining all the chunks into single file!");
				int bytesRead = 0;
	  			int current = 0;
	          	byte[] buff= new byte[fileSize];
	          	for(int j = 1; j<=totalChunks; j++){
	              	File file = new File("download/chunks/Chunk"+j);
	              	FileInputStream fis = new FileInputStream(file);
	              	BufferedInputStream bis = new BufferedInputStream(fis);
	              	do {
	                    bytesRead = bis.read(buff,current,buff.length-current);
	                    if(bytesRead > 0){
	                        current += bytesRead;
	                    }
	              	} while(bytesRead > 0);
	          	}

	          	FileOutputStream ffos = new FileOutputStream("final");
	        	BufferedOutputStream fbos = new BufferedOutputStream(ffos);
	        	fbos.write(buff,0,current);
	          	fbos.flush();
	          	fbos.close();
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + download_num);
				// ioException.printStackTrace();
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					downloadSocket.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + download_num);
					// ioException.printStackTrace();
				}
			}
		}
    }
	
	//main method
	public static void main(String args[]){
		Client client = new Client();
		client.run();
	}
}