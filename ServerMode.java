import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

// Luis Cortes
// CS 380
// Project 7 

public class ServerMode {
	private final File FILE;
	private final int  PORT;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private boolean connected;

	public ServerMode() {
		throw new NullPointerException("Need file and port!");
	}

	/**
	 *	Take in the needed file and port
	 */
	public ServerMode(File file, int port) {
		this.FILE = file;
		this.PORT = port;
		this.connected = true;
	}

	/**
	 * Listen for a client, and read in messages
	 */
	public void runServer() throws Exception{

		// Create socket on given port to listen for connections
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			while(true) {
				Socket socket = serverSocket.accept();
				/* THREAD? */
				String address = socket.getInetAddress().getHostAddress();
				System.out.printf("Client connected %s%n", address);

				// Read in first message
				inputStream = new ObjectInputStream(socket.getInputStream());
				Message message = (Message) inputStream.readObject();

				// Check message type
				switch(message.getType()) {
					case MessageType.DISCONNECT:
						// Close the connection
						serverSocket.close();
						this.conected = false;
						break;
					case MessageType.START;
						// Prepare for file transfer
				}

			}// end while
		} // end try
	}

	/**
	 *	Return the state of connection; true -> connected, false -> disconnected
	 */
	public boolean isConnected() {
		return this.connected;
	}

	/**
	 *
	 */
	private boolean start(Message msg) {
		Key key = 
	}
}