import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.CRC32;
import javax.crypto.*;

// Luis Cortes
// CS 380
// Poject 7 

public class ClientMode {
	private final String FILE;
	private final int PORT;
	private final String HOST;

	private String filename;
	private String host;
	private int port;
	private byte[] encryptedSessionKey;
	private Scanner kb; 
	private ObjectInputStream inStream;
	private ObjectOutputStream outStream;
	private Socket socket;
	private Key sessionKey;

	public ClientMode(String file, String host, int port ) throws Exception {
		this.FILE = file;
		this.HOST = host;
		this.PORT = port;

		kb = new Scanner(System.in);
		socket = new Socket(HOST, PORT);
	}

	/**
	 *	Run the client
	 */
	public void start() throws Exception {
		encryptedSessionKey = generateSessionKey();

		// Ask user for path and chunk size
		System.out.print("Enter path: ");
		String path = kb.nextLine();
		File file = new File(path);

		if (!file.exists())
			throw new FileNotFoundException("FILE NOT FOUND!!");

		System.out.print("Enter chunk size: ");
		int chunkSize = kb.nextInt();

		// Send Start message to server
		sendMsg(new StartMessage(path, encryptedSessionKey, chunkSize));
		System.out.printf("Sending: %s  File size: %d%n", path, file.length());

		// // Receive ACK message and proceed if valid
		// AckMessage ackMsg = (AckMessage) readMsg();

		List<Chunk> chunks = makeChunks(file, chunkSize);
		System.out.printf("Sending: %s chunks%n", chunks.size());


		while (true) {
			AckMessage msg = (AckMessage)readMsg();
			int seqNum = msg.getSeq();

			if (msg.getSeq() == -1 || msg.getSeq() == chunks.size())
				break;
			System.out.printf("Chunk completed [%2d/%2d]%n", seqNum, chunks.size());
			sendMsg(chunks.get(seqNum));
		}

		sendMsg(new DisconnectMessage());
 

	}

	/** 
	 *	Make a list of chunks out of a file
	 */
	public List<Chunk> makeChunks(File file, int chunkSize) throws Exception {
		FileInputStream fileStream = new FileInputStream(file);
		List<Chunk> list = new ArrayList<>();

		int numOfChunks = ((int) file.length() / chunkSize);
		if (file.length()%chunkSize != 0)
			numOfChunks++;

		for (int i = 0; i < numOfChunks; i++) {
			byte[] part = new byte[chunkSize];
			fileStream.read(part);

			CRC32 crc = new CRC32();
			crc.update(part);

			// Encrypt chunk
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, sessionKey);
			part = c.doFinal(part);

			list.add(new Chunk(i, part, (int)crc.getValue()));
		}
		return list;
	}

	/** 
	 *	Send a serialized message to server
	 */
	public void sendMsg(Message msg) throws Exception{
		if (outStream == null)
			outStream = new ObjectOutputStream(
				new PrintStream(socket.getOutputStream(), true));
		outStream.writeObject(msg);
	}

	/** 
	 *	Return Message from server
	 */
	public Message readMsg() throws Exception {
		if (inStream == null)
			inStream = new ObjectInputStream(socket.getInputStream());
		return (Message) inStream.readObject();
	}
 
	/**
	 *	Generate an encrypted session key and return it
	 */
	private byte[] generateSessionKey() throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		sessionKey = keyGen.generateKey();

		PublicKey publicKey = (PublicKey) new ObjectInputStream(new FileInputStream(FILE)).readObject();

		Cipher c = Cipher.getInstance("RSA");
		c.init(Cipher.WRAP_MODE, publicKey);

		return c.wrap(sessionKey);
	}

}