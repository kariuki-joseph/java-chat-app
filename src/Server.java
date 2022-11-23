import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private Socket client; // the client that will connect to this chat server
    private ArrayList<ConnectionHandler> connections; // store multiple connected clients
    private ServerSocket server;
    private boolean done; // know when a server is done and can shut down
    // Create a Thread pool to store client connections since they are short-lived, you do not have to create a new thread for each of them
    private ExecutorService pool;
    private final int PORT = 4567;

    public Server(){
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
             server = new ServerSocket(PORT);
             System.out.println("Chat Application Server running on port "+PORT);
             pool = Executors.newCachedThreadPool();

             // use a while loop to accept multiple connections
            while (!done) {
                // accept a client connection
                client = server.accept();
                System.out.println("New Client Connected");

                // add client to server's list of connected clients
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                // execute the 'run' method in the handler instance
                pool.execute(handler);
            }

        } catch (Exception e) {
            // show error trace
            System.err.println("Failed to start server.");
            e.printStackTrace();
        }
    }

    /**
     * Broadcast a message to all connected clients
     */
    public void broadcast(String message){
        for (ConnectionHandler handler: connections){
            handler.sendMessage(message);
        }
    }

    /**
     * Shutdown the server
     */
    public void shutdown(){
        try {
            done = true;
            if(!server.isClosed()) {
                server.close();
            }

            // close all connection handlers too
            for (ConnectionHandler handler: connections){
                handler.shutdown();
            }
        }catch (IOException e){
            // ignore server shutdown failure
        }

    }

    // handles multiple client connections
    class ConnectionHandler implements  Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;


        public ConnectionHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                // prompt the client to enter their nickname
                out.println("Please enter your nickname: ");
                nickname = in.readLine();
                // broadcast to all connected clients that the user connected
                broadcast(nickname+" joined the chat!");

                // process clients message
                String message;
                // create a loop that always asks for new messages from the client
                while ((message = in.readLine()) != null){
                    if (message.startsWith("/leave")){
                        // leave the chat room
                        broadcast(nickname+" left the chat!");
                        shutdown();
                    }else{
                        // broadcast message to connected clients
                        broadcast("\'"+nickname+"\'" +"> "+message);
                    }
                }
            }catch (IOException e){
                // shutdown this client connection
                shutdown();
            }
        }


        /**
         * Send message to client
         */
        public void sendMessage(String message){
            out.println(message);
        }

        /**
         * Shutdown client connection
         */
        public void shutdown(){
            try{
                // close all IO streams
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }
            }catch (IOException e){
                // handle client's failure to disconnect
            }
        }
    }


    public static void main(String[] args) {
        // Start our chat server
        Server server = new Server();
        server.run();
    }
}
