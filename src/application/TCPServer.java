package application;

import java.io.BufferedReader ;
import java.io.BufferedWriter ;
import java.io.File ;
import java.io.FileReader ;
import java.io.FileWriter ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.io.OutputStreamWriter ;
import java.net.ServerSocket ;
import java.net.Socket ;
import java.text.DateFormat ;
import java.util.ArrayList ;
import java.util.Date ;
import java.util.List ;
import java.util.concurrent.ConcurrentHashMap ;

/**
 * 
 *
 * @author Benjamin
 *
 * @version 1.2 2025-11-12 Updated the REQ and message protocol slightly
 *
 * @since 1.0
 */
public class TCPServer
    {

    private ServerSocket serverSocket ;
    private ConcurrentHashMap<String, Socket> clientMap ;
    private File messageHistoryFile ;
    private BufferedWriter historyWriter ;


    /**
     * @param port
     *     The port the server listens on.
     */
    public TCPServer( final int port )
        {

        try
            {
            this.serverSocket = new ServerSocket( port ) ;
            this.clientMap = new ConcurrentHashMap<>() ;
            this.messageHistoryFile = new File( "message_history_test.txt" ) ;
            this.historyWriter = new BufferedWriter( new FileWriter( this.messageHistoryFile,
                                                                     true ) ) ;

            System.out.println( "TCP chat server started successfully on port: " +
                                port ) ;
            System.out.println( "Waiting for incoming client connections..." ) ;

            while ( true )
                {
                final Socket clientSocket = this.serverSocket.accept() ;
                System.out.println( "New client connection from: " +
                                    clientSocket.getRemoteSocketAddress() ) ;
                new ClientHandler( clientSocket ).start() ;
                }

            }
        catch ( final IOException e )
            {
            System.err.println( "Server failed to start: " + e.getMessage() ) ;
            e.printStackTrace() ;
            }

        }


    /**
     * Send a message to the intended recipient and confirmation to the sender.
     */
    private synchronized void sendMessage( final String senderID,
                                           final String recipientID,
                                           final String message )
        {

        final String timestamp = DateFormat.getDateTimeInstance()
                                           .format( new Date( System.currentTimeMillis() ) ) ;

        final String logFormat = "[" + timestamp + "] " + senderID + " -> " +
                                 recipientID + ": " + message ;
        System.out.println( "Processing message: " + logFormat ) ;

        try
            {
            this.historyWriter.write( logFormat + '\n' ) ;
            this.historyWriter.flush() ;
            }
        catch ( final IOException e )
            {
            System.err.println( "Failed to write message to history: " +
                                e.getMessage() ) ;
            }

        final Socket recipientSocket = this.clientMap.get( recipientID ) ;

        if ( recipientSocket != null )
            {

            try
                {
                final BufferedWriter out = new BufferedWriter( new OutputStreamWriter( recipientSocket.getOutputStream() ) ) ;

                out.write( senderID + "\n" ) ;
                out.write( recipientID + "\n" ) ;
                out.write( message + "\n" ) ;
                out.flush() ;

                System.out.println( "Delivered message to recipient " +
                                    recipientID ) ;
                }
            catch ( final IOException e )
                {
                System.err.println( "Failed to send message to " + recipientID +
                                    ": " + e.getMessage() ) ;
                }

            }
        else
            {
            System.out.println( "Recipient " + recipientID +
                                " is offline. Message stored in history only." ) ;
            }

        final Socket senderSocket = this.clientMap.get( senderID ) ;

        if ( senderSocket != null )
            {

            try
                {
                final BufferedWriter out = new BufferedWriter( new OutputStreamWriter( senderSocket.getOutputStream() ) ) ;

                out.write( senderID + "\n" ) ;
                out.write( recipientID + "\n" ) ;
                out.write( message + "\n" ) ;
                out.flush() ;

                System.out.println( "Sent confirmation back to sender " +
                                    senderID ) ;
                }
            catch ( final IOException e )
                {
                System.err.println( "Failed to send confirmation to sender " +
                                    senderID + ": " + e.getMessage() ) ;
                }

            }

        }


    /**
     * Retrieve chat history between two users.
     */
    private synchronized List<String> getMessageHistoryBetween(
                                                                final String user1,
                                                                final String user2 )
        {

        final List<String> history = new ArrayList<>() ;

        try ( BufferedReader reader = new BufferedReader( new FileReader( this.messageHistoryFile ) ) )
            {
            String line ;

            while ( ( line = reader.readLine() ) != null )
                {

                if ( line.contains( user1 + " -> " + user2 ) ||
                     line.contains( user2 + " -> " + user1 ) )
                    {
                    history.add( line ) ;
                    }

                }

            }
        catch ( final IOException e )
            {
            System.err.println( "Error reading message history: " +
                                e.getMessage() ) ;
            }

        return history ;

        }


    /**
     * Handles communication for a single client.
     */
    private class ClientHandler extends Thread
        {

        private final Socket socket ;
        private BufferedReader in ;
        private BufferedWriter out ;
        private String clientID ;


        public ClientHandler( final Socket socketInput )
            {

            this.socket = socketInput ;

            }


        @Override
        public void run()
            {

            try
                {
                this.in = new BufferedReader( new InputStreamReader( this.socket.getInputStream() ) ) ;
                this.out = new BufferedWriter( new OutputStreamWriter( this.socket.getOutputStream() ) ) ;

                while ( true )
                    {
                    final String senderID = this.in.readLine() ;

                    if ( senderID == null )
                        {
                        System.out.println( "Client disconnected." ) ;
                        break ;
                        }

                    // REQ handler
                    if ( senderID.endsWith( "REQ" ) )
                        {
                        final String recipientID = this.in.readLine() ;

                        if ( recipientID == null )
                            {
                            break ;
                            }

                        TCPServer.this.clientMap.putIfAbsent( senderID.replace( " REQ",
                                                                                "" ),
                                                              this.socket ) ;
                        System.out.println( senderID +
                                            " requested history with " +
                                            recipientID ) ;

                        final List<String> history = getMessageHistoryBetween( senderID.replace( " REQ",
                                                                                                 "" ),
                                                                               recipientID ) ;

                        // Send REQ header
                        this.out.write( senderID + "\n" ) ;
                        this.out.write( recipientID + "\n" ) ;

                        for ( final String msg : history )
                            {
                            this.out.write( msg + "\n" ) ;
                            }

                        this.out.write( "REQ\n" ) ;
                        this.out.flush() ;

                        continue ;
                        }

                    // Normal message
                    final String recipientID = this.in.readLine() ;
                    final String message = this.in.readLine() ;

                    if ( ( recipientID == null ) || ( message == null ) )
                        {
                        System.out.println( "Client disconnected mid-message." ) ;
                        break ;
                        }

                    this.clientID = senderID ;
                    TCPServer.this.clientMap.putIfAbsent( senderID,
                                                          this.socket ) ;

                    sendMessage( senderID, recipientID, message ) ;
                    }

                }
            catch ( final IOException e )
                {
                System.err.println( "Connection error with client " +
                                    this.clientID + ": " + e.getMessage() ) ;
                }
            finally
                {
                cleanup() ;
                }

            }


        private void cleanup()
            {

            try
                {

                if ( this.in != null )
                    {
                    this.in.close() ;
                    }

                if ( this.out != null )
                    {
                    this.out.close() ;
                    }

                if ( ( this.socket != null ) && !this.socket.isClosed() )
                    {
                    this.socket.close() ;
                    }

                }
            catch ( final IOException ignored )
                {}

            if ( this.clientID != null )
                {
                TCPServer.this.clientMap.remove( this.clientID ) ;
                System.out.println( "Removed " + this.clientID +
                                    " from client map." ) ;
                }

            }

        }


    /**
     * Server entry point.
     */
    public static void main( final String[] args )
        {

        new TCPServer( 9000 ) ;

        }

    }
