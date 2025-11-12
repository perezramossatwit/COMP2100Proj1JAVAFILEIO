
package application ;

import java.io.BufferedReader ;
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.io.OutputStreamWriter ;
import java.net.Socket ;
import java.util.concurrent.ConcurrentLinkedQueue ;

/**
 * @author Ben
 *
 * @version 1.0 2025-11-06 Initial implementation
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
    private volatile boolean running = true ;
    private final ConcurrentLinkedQueue<String> messageQueue ;
    private final ConcurrentLinkedQueue<String> messageConfirmation;


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


    @Override
    public void run()
        {

        try
            {

            while ( this.running )
                {
                String line = this.in.readLine() ;
                line = this.in.readLine() ;
                String message = this.in.readLine() ;
                this.messageQueue.add( message ) ;
                }

            }
        catch ( final IOException e )
            {
            stop() ;
            e.printStackTrace() ;
            }

        }


    /**
     * 
     * @param recieverID
     * @param message
     *
     * @since 1.0
     */
    public void sendMessage( final String recieverID, final String message )
        {
           
            try
                {
                this.out.write(recieverID+ '\n');
                this.out.write( this.clientID + '\n');
                this.out.write( message + '\n');
                this.out.flush();
                }
            catch ( IOException e )
                {
                System.out.println("Error Sending Message.");
                e.printStackTrace() ;
                }
            
        }


    /**
     * @return the oldest message
     *
     * @since 1.0
     */
    public String poll()
        {

        return this.messageQueue.poll() ;

        }


    /**
     * @return the oldest message.
     *
     * @since 1.0
     */
    public String peek()
        {

        return this.messageQueue.peek() ;

        }


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

    }
// end class TCPClient