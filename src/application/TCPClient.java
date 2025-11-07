
package application;

import java.io.BufferedReader ;
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.InputStreamReader ;
import java.io.OutputStreamWriter ;
import java.net.Socket ;

/**
 * 
 * @author Ben
 *
 * @version 1.0 2025-11-06 Initial implementation
 *
 *
 * @since 1.0
 */
public class TCPClient implements Runnable
    {

    String clientID ;
    Socket clientSocket ;
    BufferedReader in ;
    BufferedWriter out ;
    Thread thread ;
    public volatile boolean running = true;
    public String message;
    
    /**
     * 
     *
     * @since 1.0
     */
    public TCPClient(String id, String hostIP, int port) throws IOException
        {

        this.clientID = id;
        this.clientSocket = new Socket(hostIP, port);
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()) );
        this.out = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()) );
        this.thread = new Thread(this);
        this.thread.start();
        
        }


    @Override
    public void run()
        {

        try 
            {
                while(running)
                {
                String line = this.in.readLine() ;
                line = this.in.readLine() ;
                message = this.in.readLine() ;
                
                }
            } 
        catch( IOException e)
            {
                stop();
                e.printStackTrace();
            }

        }

        /**
     * 
     * @param message
     *
     * @since 1.0
     * 
     * Getter for the message 
     */
    public String sentMessage( )
        {

        return message;
        

        }


        public void stop()
        {
            running = false;
            this.clientSocket.close();
        }
    }
   // end class TCPClient