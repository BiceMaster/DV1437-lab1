/*
 *  Copyright Mattias Liljeson Sep 7, 2011
 */
package racecourseserver;

import common.*;
import java.io.*;
import java.net.*;


/**
 *
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class RaceCourseServer {
    public final static String SERVER_HOSTNAME = "localhost";
    public final static int COMM_PORT = 5678;
//    ServerSocket servSock;
    RaceCourse raceCourse;
    Channel channel;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Race Course Server started");
        
        RaceCourseServer raceCourseServ = new RaceCourseServer();
        raceCourseServ.run(); 
    }
    
    public RaceCourseServer(){
//        try{
//            servSock = new ServerSocket(COMM_PORT);
//        }catch(IOException ignore){}
        
        channel = new Channel(COMM_PORT);
        channel.startServer();
        raceCourse = RaceCourse.getInstance();
    }
    
    public void run(){
        //Create accepter thread to handle new map requests
        Thread accepter = new Thread(new RaceCourseAccepter());
        accepter.start();
        
        // Enter main loop which listens to the 'exit' signal
        // TODO: ugly way of shutting down subthreads, fix this
        boolean done = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        while(!done){
            try{
                input = br.readLine();
            }catch(IOException ignore){}
            
            if(input.equals("exit")){
                System.out.println("Exiting from rcs");
                done = true;
            }
        }
        System.exit(0);
    }
    private class RaceCourseAccepter implements Runnable{

        @Override
        public void run() {
            Socket clientSock = null;
            while(true){
                clientSock = channel.accept();
                System.out.println("A new client has connected");

                if(clientSock != null){
                    Thread thread = new Thread(new RaceCourseSend(clientSock, raceCourse));
                    thread.start();
                    clientSock = null;
                }

                System.out.println("Client has been served");
            }
        }
        
    }
    private class RaceCourseSend implements Runnable{
        private Socket sock;
        private RaceCourse payload;
        private Channel channel;
        
        public RaceCourseSend(Socket socket, RaceCourse payload){ 
            this.sock = socket;
            this.payload = payload;
            
            channel = new Channel(socket);
        }

        @Override
        public void run() {
            channel.openStreams();
            if(!channel.sendObject(payload)){
                System.out.println("Failed to send race course");
            }
            channel.closeStreams();
            
            try{
                // Leave time for the client to close its input stream
                Thread.sleep(1000); 
            }catch(InterruptedException ignore){}
            finally{
                System.out.println("In finally statement, closing sockets");
                channel.closeSockets();
                System.out.println("In finally statement, closed sockets");
            }
            
        }
    }
}
