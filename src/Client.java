import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Proxy;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{
    private BufferedReader in;
    private PrintWriter out;
    private Socket client;
    private  boolean done = false;
    private static String serverAddress;
    private final int PORT = 4567;
    private StringBuffer messageBuffer;

    public Client(String serverAddress){
        this.serverAddress = serverAddress;
        this.messageBuffer = new StringBuffer();
    }
    @Override
    public void run() {
    try {
        client = new Socket("localhost", PORT);
        out = new PrintWriter(client.getOutputStream(),true);
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        // create a thread in which the client will run
        InputHandler handler = new InputHandler();
        Thread t = new Thread(handler);
        t.start();

        // get messages from the server and print them in the console
        String inMessage;
        while ((inMessage = in.readLine()) != null){
            System.out.println(inMessage);
            // append messages to messageBuffer instead
//            messageBuffer.append(inMessage);
        }

    }catch (IOException e){
    // handle failure of client to run
//        System.err.println("Error running the client. Error: "+e.getMessage());
        shutdown();
    }
    }

    /**
     * Shutdown client - Disconnect from the server
     */
    public void shutdown(){
        done = true;

        try {
            // close all IO streams
            in.close();
            out.close();
            if(!client.isClosed()){
                client.close();
            }
        }catch (IOException e){
            // ignore client unable to disconnect
        }
    }

    /**
     * Handle users input. i.e. getting and sending message
     */
    class InputHandler implements Runnable{

        @Override
        public void run() {
            try{
                while (!done){
                    // get client's message
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String message;
                    while((message =  reader.readLine()) != null) {
                        // check if user quits
                        if (message.startsWith("/leave")) {
                            reader.close();
                            // notify server this client quit
                            out.println(message);
                            shutdown();
                        } else {
                            // send message to other clients via the server
                            out.println(message);
                            // print messages from the buffer
//                            System.out.println(messageBuffer.toString());
                            // clear the message buffer to store new incoming messages
//                            messageBuffer.setLength(0);
                        }
                    }
                }
            }catch (IOException e){
                // Shutdown client connection
//                System.err.println("Error running input handler");
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        // Create a new client instance
        if(args.length == 0){
            System.out.println("You have not specified chat server address. Using default: 127.0.0.1");
            serverAddress = "localhost";
        }else{
            serverAddress = args[0];
        }

        Client client = new Client(serverAddress);
        client.run();
    }
}
