
package application ;

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
import java.nio.charset.StandardCharsets ;
import java.text.DateFormat ;
import java.util.ArrayList ;
import java.util.Date ;
import java.util.List ;
import java.util.concurrent.ConcurrentHashMap ;

/**
 * DONE Implement TCP server capabilities.
 *
 * @author Benjamin MacDougall, Sean Perez, Zach "", Alex ""
 *
 * @version 1.1 2025-11-05 Added detailed server logging
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
     *     : this is the port that the server runs out of.
     *
     * @since 1.0
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


    private synchronized void sendMessage( final String senderID,
                                           final String recipientID,
                                           final String message )
        {

        final String timestamp = DateFormat.getDateTimeInstance()
                                           .format( new Date( System.currentTimeMillis() ) ) ;

        final String serverFormat = "[" + timestamp + "] " + senderID + " -> " +
                                    recipientID + ": " + message ;
        final String senderFormat = "To " + recipientID + ": " + message ;
        final String recipientFormat = "From " + senderID + ": " + message ;

        try
            {
            this.historyWriter.write( serverFormat + '\n' ) ;
            this.historyWriter.flush() ;
            System.out.println( "Message saved to history: " + serverFormat ) ;
            }
        catch ( final IOException e )
            {
            System.err.println( "Failed to write message to history for " +
                                senderID + " -> " + recipientID + ": " +
                                e.getMessage() ) ;
            e.printStackTrace() ;
            }

        final Socket recipientSocket = this.clientMap.get( recipientID ) ;

        if ( recipientSocket != null )
            {

            try
                {
                final BufferedWriter out = new BufferedWriter( new OutputStreamWriter( recipientSocket.getOutputStream() ) ) ;
                out.write( recipientFormat + '\n' ) ;
                out.flush() ;
                System.out.println( "Sent message from " + senderID + " to " +
                                    recipientID ) ;
                }
            catch ( final IOException e )
                {
                System.err.println( "Failed to send message to " + recipientID +
                                    ": " + e.getMessage() ) ;
                }

            final Socket senderSocket = this.clientMap.get( senderID ) ;

            if ( senderSocket != null )
                {

                try
                    {
                    final BufferedWriter out = new BufferedWriter( new OutputStreamWriter( senderSocket.getOutputStream() ) ) ;
                    out.write( senderFormat + '\n' ) ;
                    out.flush() ;
                    System.out.println( "Confirmation sent back to sender: " +
                                        senderID ) ;
                    }
                catch ( final IOException e )
                    {
                    System.err.println( "Failed to send confirmation to sender " +
                                        senderID + ": " + e.getMessage() ) ;
                    }

                }

            }
        else
            {
            System.out.println( "Recipient " + recipientID +
                                " is offline. Message stored in history only." ) ;
            }

        }


    private synchronized List<String> getMessageHistoryBetween(
                                                                final String user1,
                                                                final String user2 )
        {

        final List<String> history = new ArrayList<>() ;

        System.out.println( "Retrieving message history between " + user1 +
                            " and " + user2 + "..." ) ;

        try ( BufferedReader reader = new BufferedReader( new FileReader( this.messageHistoryFile ) ) )
            {
            String line ;

            while ( ( line = reader.readLine() ) != null )
                {

                if ( ( line.contains( user1 + " -> " + user2 ) ) ||
                     ( line.contains( user2 + " -> " + user1 ) ) )
                    {
                    history.add( line ) ;
                    }

                }

            System.out.println( "Found " + history.size() +
                                " messages between " + user1 + " and " +
                                user2 ) ;
            }
        catch ( final IOException e )
            {
            System.err.println( "Error reading message history: " +
                                e.getMessage() ) ;
            }

        return history ;

        }


    private class ClientHandler extends Thread
        {

        private final Socket socket ;
        private BufferedReader in ;
        private BufferedWriter out ;
        private String clientID ;
        private String recipientID ;


        public ClientHandler( final Socket socketInput )
            {

            this.socket = socketInput ;

            }


        @Override
        public void run()
            {

            try
                {
                this.in = new BufferedReader( new InputStreamReader( this.socket.getInputStream(),
                                                                     StandardCharsets.UTF_8 ) ) ;
                this.out = new BufferedWriter( new OutputStreamWriter( this.socket.getOutputStream(),
                                                                       StandardCharsets.UTF_8 ) ) ;

                System.out.println( "Handler started for client socket: " +
                                    this.socket.getRemoteSocketAddress() ) ;

                while ( true )
                    {
                    final String line = this.in.readLine() ;

                    if ( line == null )
                        {
                        System.out.println( "Client " + ( this.clientID != null
                            ? this.clientID
                            : "unknown" ) + " disconnected." ) ;
                        break ;
                        }

                    if ( line.endsWith( "REQ" ) )
                        {
                        final int spaceIndex = line.indexOf( ' ' ) ;

                        if ( spaceIndex == -1 )
                            {
                            System.out.println( "Received malformed REQ: " +
                                                line ) ;
                            continue ;
                            }

                        this.clientID = line.substring( 0, spaceIndex ) ;
                        this.recipientID = this.in.readLine() ;

                        if ( this.recipientID == null )
                            {
                            System.out.println( "Client " + this.clientID +
                                                " disconnected mid-history request." ) ;
                            break ;
                            }

                        TCPServer.this.clientMap.putIfAbsent( this.clientID,
                                                              this.socket ) ;

                        System.out.println( this.clientID +
                                            " requested history with " +
                                            this.recipientID ) ;

                        final List<String> history = getMessageHistoryBetween( this.clientID,
                                                                               this.recipientID ) ;

                        for ( final String msg : history )
                            {
                            this.out.write( msg + '\n' ) ;
                            }

                        this.out.flush() ;
                        System.out.println( "Sent history (" + history.size() +
                                            " lines) to " + this.clientID ) ;
                        }
                    else
                        {
                        this.clientID = line ;
                        this.recipientID = this.in.readLine() ;
                        final String message = this.in.readLine() ;

                        if ( ( this.recipientID == null ) ||
                             ( message == null ) )
                            {
                            System.out.println( "Client " + this.clientID +
                                                " disconnected mid-message send." ) ;
                            break ;
                            }

                        TCPServer.this.clientMap.putIfAbsent( this.clientID,
                                                              this.socket ) ;

                        System.out.println( "Message received from " +
                                            this.clientID + " to " +
                                            this.recipientID + ": " +
                                            message ) ;

                        sendMessage( this.clientID,
                                     this.recipientID,
                                     message ) ;
                        }

                    }

                }
            catch ( final IOException e )
                {
                System.err.println( "Connection error with client " +
                                    this.clientID + ": " + e.getMessage() ) ;
                }
            catch ( final Exception e )
                {
                System.err.println( "Unexpected error with client " +
                                    this.clientID + ": " + e.getMessage() ) ;
                e.printStackTrace() ;
                }
            finally
                {

                try
                    {

                    if ( this.in != null )
                        {
                        this.in.close() ;
                        }

                    }
                catch ( final IOException ignored )
                    {}

                try
                    {

                    if ( this.out != null )
                        {
                        this.out.close() ;
                        }

                    }
                catch ( final IOException ignored )
                    {}

                try
                    {

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

                System.out.println( "Client handler cleaned up for " +
                                    ( this.clientID != null
                                        ? this.clientID
                                        : "unknown" ) ) ;
                }

            }

        }


    /**
     * @param args
     *     unused.
     *
     * @since 1.0
     */
    public static void main( final String[] args )
        {

        new TCPServer( 9000 ) ;

        }

    } // end class TCPServer
