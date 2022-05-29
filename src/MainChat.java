import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MainChat {

	public static void main(String[] args) {
		try {			
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			String peerHost = args[2];
			int peerPort = Integer.parseInt(args[3]);
	
			ServerSocket clientSocket = new ServerSocket(port, 0, InetAddress.getByName(host));
	
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter your username for the chat: ");
			String username = scanner.nextLine();
	
			Socket peerSocket = new Socket(peerHost, peerPort);
	
			Client client = new Client(peerSocket, clientSocket, username);
			client.waitForConnection(); // waits until both clients are up
			client.init();

			System.out.println("Random private number a = " + client.getA());
			System.out.println("Shared secret key Ka = " + client.getKa());
			System.out.println("Secret key AES: " + client.getSecretKey().hashCode());
			System.out.println("IV AES: " + client.getIv().hashCode());
	
			// Infinite loop to read and send messages.
			client.listenForMessage();
			client.sendMessage();
			scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
