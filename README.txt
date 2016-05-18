CNT5106 - Computer Networks Project

Group Members:

Sharath Chandra Darsha 4519-4064
Sarojini Garapati 8182-8646

Instructions:

1) Start Server
	cd Server
	javac Server.java
	java Server
2) Start Bootstrap Server
	cd Bootstrap
	javac Bootstrap.java
	java Bootstrap
3) Start the clients
	cd Client1
	javac Client.java
	java Client

Directory Structure:

Server/Server.java
Server/Project3.pdf
Client1/Client.java
Client2/Client.java
Client3/Client.java
Client4/Client.java
Client5/Client.java
Bootstrap/Bootstrap.java
Config

Results:

sharath@sharath-Lenovo-Z40-70:~/bit_torrent/Server$ javac Server.java 
sharath@sharath-Lenovo-Z40-70:~/bit_torrent/Server$ java Server 

The server is running!!!

File Name: Project3.pdf
Divided the file 'Project3.pdf' into 5 chunks!!

Client 5 is connected!
fileSize = 429213
Sending Chunk5.pdf(29213 bytes) to Client 5

Client 4 is connected!
fileSize = 429213
Sending Chunk4.pdf(100000 bytes) to Client 4

Client 3 is connected!
fileSize = 429213
Sending Chunk3.pdf(100000 bytes) to Client 3

Client 2 is connected!
fileSize = 429213
Sending Chunk2.pdf(100000 bytes) to Client 2

Client 1 is connected!
fileSize = 429213
Sending Chunk1.pdf(100000 bytes) to Client 1

sharath@sharath-Lenovo-Z40-70:~/bit_torrent/bootstrap$ java Bootstrapper 
The bootstrap server is running.

Client 5 is connected!
Client 5 listening port is 9005
Sending download port to Client 5

Client 4 is connected!
Client 4 listening port is 9004
Sending download port to Client 4

Client 3 is connected!
Client 3 listening port is 9003
Sending download port to Client 3

Client 2 is connected!
Client 2 listening port is 9002
Sending download port to Client 2

Client 1 is connected!
Client 1 listening port is 9001
Sending download port to Client 1

sharath@sharath-Lenovo-Z40-70:~/bit_torrent/Client$ java Client 

Connected to Server in port 9000

Downloading chunks from the server !!!
DownloadedChunk1.pdf(100000 bytes) from Server

Connected to Bootstrap Server in port 8050
Download port is 9005

Connected to download neighbour in port 9005

Starting upload sevrer!!
Downloaded Chunk5.pdf(29213 bytes) from the download neighbour
Downloaded Chunk4.pdf(100000 bytes) from the download neighbour
Downloaded Chunk3.pdf(100000 bytes) from the download neighbour
Downloaded Chunk2.pdf(100000 bytes) from the download neighbour

All the chunks downloaded!
Combining all the chunks into single file!
peer 2 is connected!

Chunklist from neighboour: [2]
Sending Chunk1.pdf(100000 bytes) to the upload neighbour
Sending Chunk5.pdf(29213 bytes) to the upload neighbour
Sending Chunk4.pdf(100000 bytes) to the upload neighbour
Sending Chunk3.pdf(100000 bytes) to the upload neighbour

Upload neighbour has all the chunks!
Shutting down the Upload Server!

Chunklist from neighboour: [2, 1, 5, 4, 3]

Upload neighbour has all the chunks!
Shutting down the Upload Server!