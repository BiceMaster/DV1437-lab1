/*
 *  Copyright Mattias Liljeson Sep 14, 2011
 */
package gameserver;

import common.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.swing.ImageIcon;

/**
 *
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class GameServer {
    private int pollingRate = 60;
    private Map<Integer,Car> cars;
    private boolean done = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Game Server started");
        
        GameServer gameServ = new GameServer();
        gameServ.run(); 
    }
    
    public GameServer(){
        cars = new HashMap<Integer,Car>();
    }
    
    public void run(){
        // Create clientHandlerThread thread to handle connection with clients
        ClientHandler clientHandler = new ClientHandler(this);
        Thread clientHandlerThread = new Thread(clientHandler);
        clientHandlerThread.start();
        
        // Create ExitListener thread to listen to exit message from std input
        Thread exitListenerThread = new Thread(new ExitListener());
        exitListenerThread.start();
        
        // Fetch racecourse
        // TODO: Remove hard coding of host and port below
        RaceCourse raceCourse = fetchRaceCourse("localhost", 5678);
        
        // TODO: isolate ugly code below
        Image img = raceCourse.frictionMaskImg.getImage();
        BufferedImage buffImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics g = buffImg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        
        // Enter main loop
        long prevTime = System.currentTimeMillis();
        while(!done){
            //Poll clients, update, send new positions to clients
            
            //DEBUG: test algorithm
            double tmp1 = System.currentTimeMillis() - prevTime;
            double tmp2 = (1000/pollingRate);
            boolean tmp3 = (prevTime - System.currentTimeMillis()) > (1000/pollingRate);
            //!DEBUG
            
            if((System.currentTimeMillis() - prevTime) > (1000/pollingRate)){
                clientHandler.pollClients();
                
                CarUpdate[] carUpdatesArray = new CarUpdate[cars.size()];
                int i = 0;
                for(Map.Entry<Integer, Car> entry : cars.entrySet()){
                    Car car = entry.getValue();
                    //if(cars.size() > 1) //TODO: add 2player limit
                        car.update(pollingRate, buffImg);
                    carUpdatesArray[i] = car.getCarUpdate();
                    i++;
                }
                
                RaceUpdate update = new RaceUpdate(carUpdatesArray);
                clientHandler.sendRaceUpdate(update);
            }
        }
        System.exit(0);
    }
    public void addCar(int id, Car car){
        cars.put(id,car);
    }
    
    
    public void updateKeyStates(int id, KeyStates keyStates){
        Car car = cars.get(id);
        if(car != null){
            car.setKeyStates(keyStates);
            // TODO: throw exception?
        }
    }
    
    private class ExitListener implements Runnable{

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String msg = "";
            
            // Handle exit msg
            try{
                msg = br.readLine();
            }catch(IOException ignore){}
            if(msg.equals("exit")){
                System.out.println("Exiting from gs");
                done = true;
            }
        }
        
    }
    public RaceCourse fetchRaceCourse(String hostname, int port){
        //TODO: remove hard coding below
        Channel channel = new Channel(hostname, port);
        RaceCourse raceCourse;
        boolean success = true;
        channel.connect();
        channel.openStreams();
        raceCourse = (RaceCourse)channel.readObject();
        
        if(raceCourse != null){
            System.out.println("Received payload:");
            System.out.println(raceCourse.toString());
            success = true;
        }
        
        channel.closeStreams();
        channel.closeSockets();
        return raceCourse;
    }
    
}
