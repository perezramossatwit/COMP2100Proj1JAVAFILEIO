
package application ;

import java.io.BufferedReader ;
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.io.OutputStreamWriter ;
import java.net.Socket ;
import java.util.concurrent.ConcurrentLinkedQueue ;

/**
 * @author Benjamin, Zach
 *
 * @version 1.3 2025-11-06 Removed some bugs relating to history requests.
 *
 * @since 1.0
 */
public class TCPClient implements Runnable
    {
    private final String clientID ;
    private final Socket clientSocket ;
    private final BufferedReader in ;
    private final BufferedWriter out ;
    private final Thread thread ;
    private volatile boolean running = true ; // to ensure out of order execution does not
                                              // contaminate this value, it is marked as volatile
    private volatile boolean req = false ;
    private final ConcurrentLinkedQueue<String> messageQueue ;
    private final ConcurrentLinkedQueue<String> messageConfirmation ;


    /**
     * @since 1.0
     */
    public TCPClient( final String id,
                      final String hostIP,
                      final int port ) throws IOException
        {

        this.clientID = id ;
        this.clientSocket = new Socket( hostIP, port ) ;
        this.in = new BufferedReader( new InputStreamReader( this.clientSocket.getInputStream() ) ) ;
        this.out = new BufferedWriter( new OutputStreamWriter( this.clientSocket.getOutputStream() ) ) ;
        this.messageQueue = new ConcurrentLinkedQueue<>() ;
        this.messageConfirmation = new ConcurrentLinkedQueue<>() ;
        this.thread = new Thread( this ) ;
        this.thread.start() ;

        }

    /**
     * @since 1.0
     */
    @Override
    public void run()
        {

        try
            {

            while ( this.running )
                {
                final String sender = this.in.readLine() ;
                final String reciever = this.in.readLine() ;
                final String message = this.in.readLine() ;

                if ( sender.equals( this.clientID ) )
                    {
                    this.messageConfirmation.remove( message ) ;
                    }
                else if ( sender.endsWith( "REQ" ) )
                    {
                    String line = "" ;

                    while ( true )
                        {
                        line = this.in.readLine() ;

                        if ( "REQ".equals( line ) )
                            {
                            break ;
                            }

                        this.messageQueue.add( line ) ;
                        }

                    this.req = false ;
                    }
                else
                    {
                    this.messageQueue.add( message ) ;
                    }

                }

            }
        catch ( final IOException e )
            {
            stop() ;
            e.printStackTrace() ;
            }

        }


    /**
     * @param recieverID
     * @param message
     *
     * @since 1.0
     */
    public void sendMessage( final String recieverID,
                             final String message )
        {

        try
            {
            this.out.write( this.clientID + '\n' ) ;
            this.out.write( recieverID + '\n' ) ;
            this.out.write( message + '\n' ) ;
            this.out.flush() ;
            this.messageConfirmation.add( message ) ;
            }
        catch ( final IOException e )
            {
            System.out.println( "Error Sending Message." ) ;
            e.printStackTrace() ;
            }

        }


    /**
     * @param recipientID
     *     The second id the server uses to search for messages in the message history file. The
     *     first being this clients id.
     *
     * @throws IOException
     *     If the bufferedWriter throws an IOException.
     *
     * @since 1.0
     */
    public void messageRequest( final String recipientID ) throws IOException
        {

        this.req = true ;
        this.out.write( this.clientID + " REQ" + '\n' ) ;
        this.out.write( recipientID + '\n' ) ;
        this.out.flush() ;

        while ( this.req )
            {}

        }


    /**
     * Returns and removes the first entry in the messageQueue.
     *
     * @return The oldest message
     *
     * @since 1.0
     */
    public String msgQueuePoll()
        {

        return this.messageQueue.poll() ;

        }


    /**
     * Returns the oldest message without changing the queue.
     * 
     * @return The oldest message.
     *
     * @since 1.0
     */
    public String msgQueuePeek()
        {

        return this.messageQueue.peek() ;

        }


    /**
     * @param message
     *     is the String to be searched for.
     *
     * @return true if the String is not in the unconfirmed String queue.
     *
     * @since 1.0
     */
    public boolean confirmed( final String message )
        {

        return !this.messageConfirmation.contains( message ) ;

        }


    /**
     * @since 1.0
     */
    public void stop()
        {

        this.running = false ;

        try
            {
            this.clientSocket.close() ;
            }
        catch ( final IOException e )
            {
            e.printStackTrace() ;
            }

        }
// end class TCPClient


    /**
     * @param args
     *
     * @since 1.0
     */
    public static void main( final String[] args )
        {

        final String host = "localhost" ;  // Make sure this matches your TCPServer
        final int port = 9000 ;            // Same port your server uses
        TCPClient tcpClient = null ;

        try
            {
            System.out.println( "[TCPClient] Starting client test..." ) ;
            tcpClient = new TCPClient( "Client1", host, port ) ;

            Thread.sleep( 300 ) ; // Wait a bit for connection setup

            System.out.println( "[TCPClient] Sending test message to server..." ) ;
            tcpClient.sendMessage( "Server1", "Hello from test client!" ) ;

            Thread.sleep( 500 ) ;

            System.out.println( "[TCPClient] Confirmed? " +
                                tcpClient.confirmed( "Hello from test client!" ) ) ;

            System.out.println( "[TCPClient] Sending another message..." ) ;
            tcpClient.sendMessage( "Server1", "Another test message" ) ;

            Thread.sleep( 500 ) ;

            System.out.println( "[TCPClient] Peeking message queue: " +
                                tcpClient.msgQueuePeek() ) ;
            System.out.println( "[TCPClient] Polling message queue: " +
                                tcpClient.msgQueuePoll() ) ;
            System.out.println( "[TCPClient] Message queue after poll: " +
                                tcpClient.msgQueuePeek() ) ;

            try
                {
                System.out.println( "[TCPClient] Requesting message history (REQ)..." ) ;
                tcpClient.messageRequest( "Server1" ) ;
                }
            catch ( final IOException e )
                {
                System.out.println( "[TCPClient] REQ failed (server may not support it)." ) ;
                }

            Thread.sleep( 500 ) ;

            System.out.println( "[TCPClient] Dumping message queue after REQ:" ) ;
            String msg ;

            while ( ( msg = tcpClient.msgQueuePoll() ) != null )
                {
                System.out.println( " - " + msg ) ;
                }

            System.out.println( "[TCPClient] Stopping client..." ) ;
            tcpClient.stop() ;
            System.out.println( "[TCPClient] Test completed." ) ;
            }
        catch ( final Exception e )
            {
            e.printStackTrace() ;
            }
        finally
            {

            if ( tcpClient != null )
                {
                tcpClient.stop() ;
                }

            }

        }

    }
