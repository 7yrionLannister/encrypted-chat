import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Client {

	public static final BigInteger G = new BigInteger("59");
	public static final BigInteger P = new BigInteger("3049761456087");
	public static final String algorithm = "AES/CBC/PKCS5Padding";

	private BigInteger a; // The random number chosen by the client for the Diffie-Hellman algorithm
	private BigInteger Ka; // Shared secret key from Diffie-Hellman
	private IvParameterSpec iv; // Initialization vector
	private SecretKey secretKey; // Secret key

	private Socket peerSocket;
	private ServerSocket serverSocket;
	private BufferedReader bufferedReader;
	private BufferedWriter bufferedWriter;
	private String username;

	public Client(Socket peerSocket, ServerSocket serverSocket, String username) throws NoSuchAlgorithmException {
		try {
			this.a = BigInteger.valueOf((long) (1 + Math.random() * 1000)); // random secret key
			this.peerSocket = peerSocket;
			this.serverSocket = serverSocket;
			this.username = username;
			this.bufferedReader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
		} catch (IOException e) {
			// Gracefully close everything.
			closeEverything(peerSocket, bufferedReader, bufferedWriter);
		}
	}

	/**
	 * Sending a message isn't blocking and can be done without spawning a thread,
	 * unlike waiting for a message.
	 */
	public void sendMessage() {
		try {
			Scanner scanner = new Scanner(System.in);
			
			while (peerSocket.isConnected()) {
				String messageToSend = scanner.nextLine();
				String plainText = username + ": " + messageToSend;
				String cipherText = encryptMessage(plainText, secretKey, iv);
				bufferedWriter.write(cipherText);
				bufferedWriter.newLine();
				bufferedWriter.flush();
			}
			scanner.close();
		} catch (Exception e) {
			// Gracefully close everything.
			e.printStackTrace();
			closeEverything(peerSocket, bufferedReader, bufferedWriter);
		}
	}
	
	/**
	 * Encrypts the message that is being sent
	 * @param message - the string to be encrypted
	 * @param key - the secret key
	 * @param iv - the initialization vector
	 * @return String representation of the cipher text
	 * @throws Exception
	 */
	public String encryptMessage(String message, SecretKey key, IvParameterSpec iv) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] cipherText = cipher.doFinal(message.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(cipherText);
	}
	
	/**
	 * Decrypts the message that is received
	 * @param message - the string to be encrypted
	 * @param key - the secret key
	 * @param iv - the initialization vector
	 * @return String representation of the plain text
	 * @throws Exception
	 */
	public String decryptMessage(String message, SecretKey key, IvParameterSpec iv) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		byte[] plainText  = cipher.doFinal(Base64.getDecoder().decode(message));
        return new String(plainText);
	}
	
	/**
	 * Generates the key and the initialization vector. It also executes the Diffie-Hellman algorithm.
	 */
	public void init() {
		try {
			DiffieHellman();
			secretKey = generateKey(Ka);
			iv = generateIv(Ka.toByteArray());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generates the secret key
	 * @param n - the secret key shared between the two clients after Diffie-Hellman execution
	 * @return the secret key
	 * @throws NoSuchAlgorithmException
	 */
	public SecretKey generateKey(BigInteger n) throws NoSuchAlgorithmException {
		byte[] key = n.toByteArray();
		key = Arrays.copyOf(key, 16);
		SecretKey secretKey = new SecretKeySpec(key, "AES");
		return secretKey;
    }
	
	/**
	 * Generates the Initialization Vector (IV) based on the shared secret vector for the CBC algorithm. 
	 * Its size should be 128 bits.
	 * @param ivParam - the bytes array representation of shared secret key
	 * @return the initialization vector for the CBC algorithm
	 * @throws IOException
	 */
    public IvParameterSpec generateIv(byte[] ivParam) throws IOException {
        byte[] iv = Arrays.copyOf(ivParam, 16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        return ivParameterSpec;
    }
    
    /**
     * Performs the Diffie-Hellman algorithm.
     * @throws IOException
     */
    public void DiffieHellman() throws IOException {
    	BigInteger Xa = calculatePower(G, a, P);
		bufferedWriter.write(Xa + "");
		bufferedWriter.newLine();
		bufferedWriter.flush();

		BigInteger Xb = new BigInteger(bufferedReader.readLine());
		Ka = calculatePower(Xb, a, P);
		if (Ka.intValue() > 0) {
			System.out.println("Diffie-Hellman done!");			
		}
    }

    /**
     * Listening for a message is blocking so need a separate thread for that.
     */
	public void listenForMessage() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String msgFromGroupChat;
				// While there is still a connection with the server, continue to listen for
				// messages on a separate thread.
				while (peerSocket.isConnected()) {
					try {
						// Get the messages sent from other users and print it to the console.
						msgFromGroupChat = bufferedReader.readLine();
						String decryptedText = decryptMessage(msgFromGroupChat, secretKey, iv);
						System.out.println("Cipher text - " + msgFromGroupChat);
						System.out.println("Plain text - " + decryptedText);
					} catch (IOException e) {
						// Close everything gracefully.
						closeEverything(peerSocket, bufferedReader, bufferedWriter);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	/**
	 * Helper method to close everything so you don't have to repeat yourself.
	 * @param socket - the peer socket
	 * @param bufferedReader - the input stream
	 * @param bufferedWriter - the output stream
	 */
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

	/**
	 * Calculates the g^y mod p formula for the Diffie-Hellman algorithm.
	 * @param g2
	 * @param y
	 * @param p2
	 * @return
	 */
	public BigInteger calculatePower(BigInteger g2, BigInteger y, BigInteger p2) {
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

	public BigInteger getA() {
		return a;
	}

	public void setA(BigInteger a) {
		this.a = a;
	}

	public BigInteger getKa() {
		return Ka;
	}

	public void setKa(BigInteger ka) {
		Ka = ka;
	}

	public IvParameterSpec getIv() {
		return iv;
	}

	public void setIv(IvParameterSpec iv) {
		this.iv = iv;
	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public Socket getClientSocket() {
		return peerSocket;
	}

	public void setClientSocket(Socket clientSocket) {
		this.peerSocket = clientSocket;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public BufferedReader getBufferedReader() {
		return bufferedReader;
	}

	public void setBufferedReader(BufferedReader bufferedReader) {
		this.bufferedReader = bufferedReader;
	}

	public BufferedWriter getBufferedWriter() {
		return bufferedWriter;
	}

	public void setBufferedWriter(BufferedWriter bufferedWriter) {
		this.bufferedWriter = bufferedWriter;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
