/*
 *  Copyright Mattias Liljeson Sep 14, 2011
 */
package gameserver;

import common.*;
import java.awt.Color;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class ClientHandler implements Runnable{
    public final static String SERVER_HOSTNAME = "localhost";
    public final static int COMM_PORT = 5679;
    //ServerSocket servSock;
    private HashMap<Integer, ClientConnection> clientConnections; // must be a map, not ArrayList, since removing clients from an array list would fuck up id mapping.
    private int nextClientID = 0; //Client ID counter. Increases with every connected client. Cannot use clients.size() since it decreases when clients are removed. We want a unique number.
    private GameServer server;
    private Channel channel;
    
    public ClientHandler(GameServer server){
//        try{
//            servSock = new ServerSocket(COMM_PORT);
//        }catch(IOException ignore){}
        channel = new Channel(COMM_PORT);
        
        this.server = server;
        clientConnections = new HashMap<Integer, ClientConnection>();
        System.out.println("ClientHandler started");
    }
    
    @Override
    public void run() {
        // Wait for clients to connect.
        // When a client has connected, create a new ClientConnection
        Socket clientSock = null;
        channel.startServer();
        
        while(true){
            clientSock = channel.accept();
            System.out.println("A new client has connected");

            if(clientSock != null){
                ClientConnection clientConn = new ClientConnection(clientSock, this, nextClientID);
                
                //--------------------------------------------------------------
                // TODO: break out this functionality into the clientConn thread 
                // which also waits for ClientInit message
//                synchronized(this) {
//                    clientConnections.put(nextClientID, clientConn);
//                }
                // TODO: Add a car for the client, fetch car color etc
//                Car clientCar = new Car(400,200,0, Color.red);
//                server.addCar(nextClientID, clientCar);
		
                // Increase the id counter to prepare for the next client 
                // connection
                nextClientID++;
				//--------------------------------------------------------------
                Thread thread = new Thread(clientConn);
                thread.start();
                clientSock = null;
            }

            System.out.println("Client has been served by ClientHandler. "
                    + "Now looking for new connections");
        }
    }
    
    public boolean addClient(int id, ClientConnection conn, Car car){
        boolean success = false;
        synchronized(this) {
            if(clientConnections.get(id) == null) {
                server.addCar(id, car);
                clientConnections.put(id, conn);
                success = true;
            }
        }
        return success;
    }
    
    public void pollClients(){
        synchronized(this) {
            for(Integer clientID : clientConnections.keySet()){
                clientConnections.get(clientID).poll();
            }
        }
    }
    
    public boolean removeClient(int id) {
        boolean result = false;
        synchronized(this) {
            if(clientConnections.remove(id) != null) {
                result = true;
				server.removeCar(id);
			}
        }
        return result;
    }
    
    public void sendRaceUpdate(RaceUpdate update){
        synchronized(this) {
            for(Integer clientID : clientConnections.keySet()){
                clientConnections.get(clientID).sendRaceUpdate(update);
            }
        }
    }
    
    public void updateKeyStates(int id, KeyStates keyStates){
        server.updateKeyStates(id, keyStates);
    }
}
