import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	private static final BigInteger G = BigInteger.valueOf(586);
	private static final BigInteger P = BigInteger.valueOf(3049);

	private BigInteger a;
	private BigInteger Ka;

	private Socket clientSocket;
	private ServerSocket serverSocket;
	private BufferedReader bufferedReader;
	private BufferedWriter bufferedWriter;
	private String username;

	public Client(Socket socket, ServerSocket serverSocket, String username) {
		try {
			this.a = BigInteger.valueOf((long)(1 + Math.random() * 1000)); // random secret key
			this.clientSocket = socket;
			this.serverSocket = serverSocket;
			this.username = username;
			this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// Gracefully close everything.
			closeEverything(socket, bufferedReader, bufferedWriter);
		}
	}

	// Sending a message isn't blocking and can be done without spawning a thread,
	// unlike waiting for a message.
	public void sendMessage() {
		try {			
			Scanner scanner = new Scanner(System.in);
			// TODO encrypt before sending
			while (clientSocket.isConnected()) {
				String messageToSend = scanner.nextLine();
				bufferedWriter.write(username + ": " + messageToSend);
				bufferedWriter.newLine();
				bufferedWriter.flush();
			}
			scanner.close();
		} catch (IOException e) {
			// Gracefully close everything.
			closeEverything(clientSocket, bufferedReader, bufferedWriter);
		}
	}

	// Listening for a message is blocking so need a separate thread for that.
	public void listenForMessage() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String msgFromGroupChat;
				// While there is still a connection with the server, continue to listen for
				// messages on a separate thread.
				while (clientSocket.isConnected()) {
					try {
						// Get the messages sent from other users and print it to the console.
						msgFromGroupChat = bufferedReader.readLine();
						System.out.println(msgFromGroupChat);
					} catch (IOException e) {
						// Close everything gracefully.
						closeEverything(clientSocket, bufferedReader, bufferedWriter);
					}
				}
			}
		}).start();
	}

	// Helper method to close everything so you don't have to repeat yourself.
	public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
		// Note you only need to close the outer wrapper as the underlying streams are
		// closed when you close the wrapper.
		// Note you want to close the outermost wrapper so that everything gets flushed.
		// Note that closing a socket will also close the socket's InputStream and
		// OutputStream.
		// Closing the input stream closes the socket. You need to use shutdownInput()
		// on socket to just close the input stream.
		// Closing the socket will also close the socket's input stream and output
		// stream.
		// Close the socket after closing the streams.
		try {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Run the program.
	public static void main(String[] args) throws IOException {
		System.out.println("Que bonito día no joda");
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String peerHost = args[2];
		int peerPort = Integer.parseInt(args[3]);

		ServerSocket serverSocket = new ServerSocket(port, 0, InetAddress.getByName(host));

		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter your username for the chat: ");
		String username = scanner.nextLine();

		Socket socket = new Socket(peerHost, peerPort);

		Client client = new Client(socket, serverSocket, username);
		client.waitForConnection(); // waits until both clients are up

		// now configures the secret key, a.k.a. Diffie-Hellman

		BigInteger Xa = calculatePower(G, client.a, P);
		client.bufferedWriter.write(Xa + "");
		client.bufferedWriter.newLine();
		client.bufferedWriter.flush();

		BigInteger Xb = new BigInteger(client.bufferedReader.readLine());
		client.Ka = calculatePower(Xb, client.a, P);

		// Diffie-Hellman done, now both clients have agreed on a shared secret key Ka

		System.out.println("Random private number a = " + client.a);
		System.out.println("Shared secret key Ka = " + client.Ka);

		// Infinite loop to read and send messages.
		client.listenForMessage();
		client.sendMessage();
		scanner.close();
	}

	private static BigInteger calculatePower(BigInteger g2, BigInteger y, BigInteger p2) {
		BigInteger result = BigInteger.ZERO;
		if (y.equals(BigInteger.ONE)) {
			return g2;
		} else {
			result = g2.modPow(y, p2);
			return result;
		}
	}

	public void waitForConnection() {
		try {
			Socket socket = serverSocket.accept();
			this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			System.out.println("Connection created!");
		} catch (IOException e) {
			closeServerSocket();
		}
	}

	public void closeServerSocket() {
		try {
			if (serverSocket != null) {
				serverSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
