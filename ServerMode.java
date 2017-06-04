import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.Key;
import java.security.PrivateKey;
import javax.crypto.*;
import java.util.zip.CRC32;

// Luis Cortes
// CS 380
// Project 7 

public class ServerMode {
	private String file;
	private int port;

	public ServerMode(String file, int port) {
		this.file = file;
		this.port = port;
	}

	public void startServer() {
		try (ServerSocket socket = new ServerSocket(port)) {
			new Thread(
				new ServerListener(this.file, this.port, socket.accept() ) ).start();

		} catch (Exception e) {e.printStackTrace();}
	}

	private class ServerListener implements Runnable {	
		private final String FILE;
		private final int  PORT;
		public boolean connected;
		private Socket socket;
		private ObjectInputStream inputStream;
		private ObjectOutputStream outputStream;
		private Key sessionKey;
		private PrivateKey privateKey;
		private Message message;
		private int expectedSeq = -1;
		private int chunks;
		private FileOutputStream fileOutStream; 
		private String path;

		public ServerListener() {
			throw new NullPointerException("Need file and port!");
		}

		/**
		 *	Take in the needed file and port
		 */
		public ServerListener(String file, int port, Socket socket) {
			this.FILE = file;
			this.PORT = port;
			this.socket = socket;
			this.connected = true;
		}

		/**
		 * Listen for a client, and read in messages
		 */
		@Override
		public void run() {
			while(true) {
				try {
					/* THREAD? */
					String address = socket.getInetAddress().getHostAddress();
					// System.out.printf("Client connected %s%n", address);

					// inputStream = new ObjectInputStream(socket.getInputStream());
					// outputStream = new ObjectOutputStream(
					// 	new PrintStream(socket.getOutputStream(), true));

					// Read in messag
					message = readMsg();

					MessageType msgType = message.getType();

					// Perform according to message type
					if (msgType == MessageType.DISCONNECT) {
						// Close the connection
						socket.close();
						System.out.println("DISCONNECT");
						this.connected = false;
						break;

					} else if (msgType == MessageType.START) {
						start((StartMessage) message);

					} else if (msgType == MessageType.STOP) {
						// Stop the file transfer
						sessionKey = null;
						privateKey = null;
						outputStream.writeObject(new AckMessage(-1));

					} else if (msgType == MessageType.CHUNK) {
						// System.out.println("CHUNK read");
						processChunk((Chunk) message);
					}
				} catch (Exception e) {e.printStackTrace();}
			}
		}

		/**
		 *	Return status of connection
		 */
		public boolean isConnected() {return this.connected;};

		/** 
		 *	Verify if chunk received is valid and verify the checksum
		 */
		private void processChunk(Chunk chunk) throws Exception{
			// System.out.printf("Server chunk[%d] == client chunk[%d]%n", expectedSeq, chunk.getSeq());
			if (chunk.getSeq() == expectedSeq) { // Check if valid
				// Get decrypted data
				byte[] data = decryptChunk(chunk.getData());

				// Calculate CRC from data
				CRC32 check = new CRC32();
				check.update(data);

				// Compare CRC
				if ((int)check.getValue() == chunk.getCrc()) {
					expectedSeq++;
				
					fileOutStream.write(data);
					System.out.printf("Chunk received [%2d/%2d]%n", expectedSeq, chunks);
					sendMsg(new AckMessage(expectedSeq));
				}
			}

			// Reached then end of chunks
			if (expectedSeq == chunks) {
				fileOutStream.close();
				System.out.println("Transfer complete.");
				System.out.println("Output path: "+path);
			}
		}

		/**
		 *	Decrypt data from chunk using session key
		 */
		private byte[] decryptChunk(byte[] data) throws Exception {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE, sessionKey);
			return c.doFinal(data);
		}


		private void sendMsg(Message msg) throws Exception {
			if (outputStream == null) {
				outputStream = new ObjectOutputStream(
					new PrintStream(socket.getOutputStream(), true));
			} 
			outputStream.writeObject(msg);
		}

		private Message readMsg() throws Exception {
			if (inputStream == null) {
				inputStream = new ObjectInputStream(socket.getInputStream());
			}
			return (Message) inputStream.readObject();
		}

		/**
		 *	Prepare for transfer
		 */
		private void start(StartMessage msg) throws Exception {
			// Prepare for file transfer
			if (prepareFileTransfer()) {// successful
				path = "_"+msg.getFile();
				fileOutStream = new FileOutputStream(path);

				int chunkLen = ((StartMessage)message).getChunkSize();
				long fileSize    =  ((StartMessage)message).getSize();

				chunks = (int) (fileSize / chunkLen); 

				if (fileSize % chunks > 0)
					chunks++;

				sendMsg(new AckMessage(0));
				expectedSeq = 0;

			} else { // not successful
				sendMsg(new AckMessage(-1));
			}
		}

	 	/** 
	 	 *	Decrypt the session key, and returns if successful
	 	 */
	 	private boolean prepareFileTransfer() {
	 		
	 		try {
				privateKey = (PrivateKey) new ObjectInputStream( new FileInputStream(FILE) ).readObject(); // Get private key

				Cipher c = Cipher.getInstance("RSA");
				c.init(Cipher.UNWRAP_MODE, privateKey);

				byte[] encryptedKey = ((StartMessage)message).getEncryptedKey();
				sessionKey = c.unwrap(encryptedKey, "AES", Cipher.SECRET_KEY);
				return true;

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

	 	}
	 }
}