package application;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * 
 * DONE Implement TCP server capabilities.
 * 
 * @author Benjamin MacDougall, Sean Perez, Zach "", Alex ""
 *
 * @version 1.0 2025-09-24 Initial implementation
 *
 * @since 1.0
 * 
 */
public class TCPServer
{

	private ServerSocket serverSocket; //socket to listen for the incoming connections.
	//map that tracks clients by their IDs and stores their socket. (importantly thread safe.)
	private ConcurrentHashMap<String, Socket> clientMap;
	private File messageHistoryFile; //History file that contains all chat history, along with timestamps and client IDs.
	private BufferedWriter historyWriter; //The file writer that adds to history file.

	/**
	 * @param port this is the port the server is hosted on.
	 *
	 * @since 1.0
	 */
	public TCPServer(int port) {

		try {
			//this is all initializing the instance variables.
			this.serverSocket = new ServerSocket(port);

			this.clientMap = new ConcurrentHashMap<>();

			this.messageHistoryFile = new File("message_history_test.txt");

			this.historyWriter = new BufferedWriter(new FileWriter(this.messageHistoryFile, true));

			System.out.println("The best groups TCP chat server successfully started on port: " + port);

			//here is the main loop where the server accepts all new clients and makes a new thread for each one.

			while(true) {
				Socket clientSocket = this.serverSocket.accept();
				new ClientHandler(clientSocket).start();
			}


		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	//this method is synchronized so it can only be accessed by one thread at a time. since buffered writer/reader isn't thread safe
	//resulting in race exceptions and corrupted data in the history file. (not good).
	//this method takes care of logging history and sending the message to the user.
	private synchronized void sendMessage(String senderID, String recipientID, String message) {

		String timestamp = DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())); //idk if this works i looked it up ngl

		//these are strings formatted for each purpose. Sender, recipient, and server.
		String serverFormat = "[" + timestamp + "] " + senderID + " -> " + recipientID + ": " + message;
		String senderFormat = "To " + recipientID + ": " + message;
		String recipientFormat = "From " + senderID + ": " + message;


		//this try catch is to write to the history text file.
		try 
		{
			this.historyWriter.write(serverFormat + '\n');
			this.historyWriter.flush();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		//check the clientMap to see if the user is online. and sends if so
		Socket recipientSocket = this.clientMap.get(recipientID);
		if(recipientSocket != null) {
			try
			{
				//the actual sending of the message through the buffered writer!!!!
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(recipientSocket.getOutputStream()));
				out.write(recipientFormat + '\n');
				out.flush();

			}
			catch(IOException e)
			{
				e.printStackTrace();
			}

			//confirmation to the sender so they can show the sent message on their screen.
			Socket senderSocket = this.clientMap.get(senderID);
			//just in case, in the rare case the sender closes their connection after the server gets the message and before the receiver gets the message.
			if (senderSocket != null) {
				try {
					//sending back to the sender for sent/visual notification
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(senderSocket.getOutputStream()));
					out.write(senderFormat + '\n');
					out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}


			}

		}
	}

	private synchronized List<String> getMessageHistoryBetween(String user1, String user2){
		List<String> history = new ArrayList<>();
		try(BufferedReader reader = new BufferedReader(new FileReader(this.messageHistoryFile)))
		{
			String line;
			while((line = reader.readLine()) != null) {
				if((line.contains(user1 + " -> " + user2)) || (line.contains(user2 + " -> " + user1))) 
				{
					history.add(line);
				}
			}
		}
		catch(IOException e) 
		{
			e.printStackTrace();
		}
		return history;
	}
	private class ClientHandler extends Thread {
		private BufferedReader in; //using buffered reader/writer because they work well with regular text (which is what we are using)
		private BufferedWriter out;//we aren't using them for anything other than char streams. 
		//they also read line by line so we can use the new line char/string manipulation to separate client id recipient id and the message.
		//meaning we can easily separate them back into separate strings/arrays for ease of use.

		//the client will send something like this:

		//Ben123 REQ (sender requesting history)
		//Sean222 (receiver)

		//we can search the line for REQ (we can change this to something else)** which means to send back the message history of the two so the client can see message history
		//or it can look like this:

		//Ben123 (user1 sender)
		//Sean222 (user2 receiver)
		//Hello!! (message)

		//**the only thing is we need to make sure the users name does not ever have REQ or whatever we decide to use as the send history flag.
		//so that way a user is never stun-locked and never able to send a message because all of their sent messages are flagged as history requests.
		//Zach brought up the idea to use invisible or non typeable chars as flags so we dont have to restrict usernames.

		private String clientID;
		private String recipientID;

		public ClientHandler(Socket socket) {
			try {
				this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}


		@Override
        public void run() {

			try {
				while (true) {

					String line = this.in.readLine(); //Could be either a message request or a history request "REQ"
					if(line == null) {
						break;
					}

					if(line.endsWith("REQ")) {
						this.clientID = line.substring(0,line.indexOf(" "));
						this.recipientID = this.in.readLine();

						//get history and send it over using this.out the buffer writer to the client. NOT THE TARGET. FOR HISTORY REQUESTS WE SEND BACK TO THE CLIENT.
						List<String> history = getMessageHistoryBetween(this.clientID, this.recipientID);
						for(String msg: history) {
							this.out.write(msg + '\n');
						}
						this.out.flush();
					}
					else
					{
					
						//regular messages
						this.clientID = line;
						this.recipientID = this.in.readLine();
						String message = this.in.readLine();

						//sending the message using the send message method
						sendMessage(this.clientID,this.recipientID,message);
					}


				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {

		new TCPServer(9000);

	}
}//end class TCPServer