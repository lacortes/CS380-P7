import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;

// Luis Cortes
// CS380
// Project 7

public class FileTransfer {
	/**
	 *	Main Method
	 * 	Check whether command line arguments were passed correctly
	 */
	public static void main(String[] args) {
		if (args.length <= 0) { // NO command line arguments inserted
			System.out.println("Usage:  FileTransfer");
			System.out.println("\tmakeKeys");
			System.out.println("\tserver <file> <port>");
			System.out.println("\tclient <file> <host> <port>");
		} else { // Determine how many args were passed
			int size = args.length;

			// Check if valid size
			if (size == 1 || size == 3 || size == 4) 
				new FileTransfer(args); // Valid
			else
				System.out.println("Incorrect argument size!");
		}
	}
	
	private final String[] args; 

	public FileTransfer(String[] args) {
		this.args = args;
		switch(args.length) {
			case 1: // makeKeys
				makeKeys();
				break;
			case 3: // server
				server();
				break;
			case 4: // client
				client();
				break;
			default:
		}
	}

	/**
	 *	Generate public/private RSA key pair
	 */
	private void makeKeys() {
		try {
			// Generate RSA pair
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048);

			// Get private and public key
			KeyPair keyPair = gen.genKeyPair();
			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			// Write public key to file public.bin
			try (ObjectOutputStream outStream = new ObjectOutputStream(
				new FileOutputStream(new File("public.bin")))) {
				outStream.writeObject(publicKey);
			}

			// Write private key to file private.bin
			try (ObjectOutputStream outStream = new ObjectOutputStream(
				new FileOutputStream(new File("private.bin")))) {
				outStream.writeObject(privateKey);
			}
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 *	Run the server while the connection is open
	 */
	private void server() {
		try  {
			ServerMode server = new ServerMode(args[1], Integer.parseInt(args[2]));
			server.startServer();

		} catch (Exception e ) {e.printStackTrace();}
	}

	/**
	 *	Run the client
	 */
	private void client() {
		try {
			ClientMode client = new ClientMode(args[1], args[2], Integer.parseInt(args[3]));
			client.start();
		} catch (Exception e) {e.printStackTrace();}
	}

}