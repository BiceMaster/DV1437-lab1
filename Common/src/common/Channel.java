/*
 *  Copyright Mattias Liljeson Sep 29, 2011
 */
package common;

import java.io.*;
import java.net.*;

/**
 *
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class Channel {
    // Common
    private int connPort;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private int channelState = CHANNEL_STATE_LIMBO;
    public final static int CHANNEL_STATE_SERVER = 0;
    public final static int CHANNEL_STATE_CLIENT = 1;
    public final static int CHANNEL_STATE_LIMBO = 3;
    
    // As Client
    private String srvHostname;
    private Socket cliSock;
    
    // As Server
    ServerSocket srvSock;
    
    public Channel(Socket socket){
        channelState = CHANNEL_STATE_CLIENT;
        cliSock = socket;
        connPort = socket.getPort();
        srvHostname = socket.getInetAddress().getHostAddress();
    }
    
    public Channel(String serverHostname, int connectionPort){
        channelState = CHANNEL_STATE_CLIENT;
        srvHostname = serverHostname;
        connPort = connectionPort;
    }
    
    public Channel(int connectionPort){
        channelState = CHANNEL_STATE_SERVER;
        connPort = connectionPort;
    }
    
    public Socket accept(){
        Socket tmp = null;
        try{
            tmp = srvSock.accept();
        }catch(IOException ex){
            System.out.println("Error when waiting for connection");
            ex.printStackTrace();
        }
        return tmp;
    }
    
    public boolean closeStreams(){
        boolean success = true;
        // Close streams
        try{
            if(outStream != null)
                outStream.close();
            if(inStream != null)
                inStream.close();
        }catch(IOException ex){
            System.out.println("Error when closing streams");
            success = false;
        }
        return success;
    }
    
    public boolean closeSockets(){
        boolean success = true;
        // Close sockets
        try{
            if(channelState == CHANNEL_STATE_CLIENT)
                cliSock.close();
            else if(channelState == CHANNEL_STATE_SERVER)
                srvSock.close();
            else if(channelState == CHANNEL_STATE_LIMBO)
                System.out.println("channelstate is LIMBO. No socket to close");
        }catch(IOException ex){
            System.out.println("Couldn't close socket. Has it been opened?");
        }
        return success;
    }
    
    
    public boolean connect(){
        boolean success = true;
        try{
            cliSock = new Socket(srvHostname, connPort);
        }
        catch (UnknownHostException ex){
            System.out.println("Host: " + srvHostname + " doesn't exist");
            success = false;
        }
        catch(IOException ex){
            System.out.println("Couldn't connect to: " + srvHostname + " : " + connPort);
            ex.printStackTrace();
            success = false;
        }
        return success;
    }
    
    public boolean openStreams(){
        boolean success = true;
        try{
            outStream = new ObjectOutputStream(cliSock.getOutputStream());
        }catch(IOException ex){
            System.out.println("Error in output stream creation");
            success = false;
        }
        
        try{
            inStream = new ObjectInputStream(cliSock.getInputStream()); 
        }catch(IOException ex){
            System.out.println("Error in input stream creation");
            success = false;
        }
        return success;
    }
    
    public Object readObject(){
        Object tmp = null;
        try{
            tmp = inStream.readObject();
        }catch(ClassNotFoundException ex){
            System.out.println("Other Class than KeyStates received, " + ex);
        }catch(IOException ex){
            System.out.println("Problem with socket input");
        }
        return tmp;
    } 
    
    public void runAsClient(String serverHostname, int connectionPort){
        channelState = CHANNEL_STATE_CLIENT;
        srvHostname = serverHostname;
        connPort = connectionPort;
    }
    
    public void runAsServer(int connectionPort){
        channelState = CHANNEL_STATE_SERVER;
        connPort = connectionPort;
    }
    
    public boolean sendObject(Object obj){
        boolean success = false;
        try{
            if(outStream != null){
                outStream.writeObject(obj);  // send serilized payload
                outStream.flush();
                outStream.reset();
                success = true;
            }else{
                System.out.println("output stream not opened");
            }
        }catch(IOException ex){
            System.out.println("Error when sending object through stream");
            success = false;
        }
        return success;
    }
    
    public boolean startServer(){
        boolean success = true;
        
        try{
            srvSock = new ServerSocket(connPort);
        }catch(IOException ignore){
            success = false;
            System.out.println("couldn't create server socket");
        }
        
        return success;
    }
}
