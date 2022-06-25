import java.io.*;
import java.net.Socket;

import static java.lang.Integer.parseInt;

/*  Class used to connect to server as a client  */
public class ChatClient {

    /*  Defining socket, threads and reader used  */
    private Socket serverSocket;
    private ReceiveMessagesThread receiveMessagesThread;
    private SendMessagesThread sendMessagesThread;
    volatile Boolean clientActive;


    /*  Constructor method to create instance of ChatClient  */
    public ChatClient(String address, int port){
        //Attempts to connect client to server. If failed, informs user and calls close() method.
        try {
            serverSocket = new Socket(address, port);//change to pass through port no
            System.out.println("\nConnected to server:\n>Address: " + address +"\n>Port: "+port);
            clientActive=true;
        } catch (IOException e) {
            System.out.println("\nUnable to connect to server:\n[ADDRESS]: " + address + "\n[PORT]: " + port + "\nPlease check server details and try again.");
            clientActive=false;
            closeClient();
        }
    }

    /*  Method called to start running instance of ChatClient  */
    public void go() {

        //initialising threads
        receiveMessagesThread = new ReceiveMessagesThread(serverSocket);
        sendMessagesThread = new SendMessagesThread(serverSocket);

        //starting threads
        sendMessagesThread.start();
        receiveMessagesThread.start();

    }

    /*  Method called to cleanly close ChatClient instance  */
    public void closeClient(){

        //ensures shutdown cycle only occurs once
        if(clientActive) {

            clientActive=false;

            System.out.println("\nClosing connections...");

            /*  If component has been initialised -> closes, else -> ignore  */
            if (serverSocket != null) {
                closeResource(serverSocket);
            }

            /*  Calling .terminate() methods of threads  */
            if (receiveMessagesThread != null) {
                receiveMessagesThread.terminate();
            }
            if (sendMessagesThread != null) {
                sendMessagesThread.terminate();
            }

            if (!sendMessagesThread.sendThreadActive && !receiveMessagesThread.receiveThreadActive
                    && (serverSocket.isClosed() || serverSocket == null)) {
                System.out.println("Clean shutdown complete.");
            } else {
                System.out.println("\nFailed to shutdown cleanly. Forcing shutdown.");
                System.exit(0);
            }
        }
    }

    /*  Method called to close a resource, i.e. BufferedReader  */
    public void closeResource(Closeable resource){
        try{
            resource.close();
        } catch (IOException ioException) {
            System.out.println("Unable to close: "+resource);
        }
    }

    /*  Thread used to receive + display messages from server  */
    class ReceiveMessagesThread extends Thread {

        /*  Necessary socket and readers to receive from server  */
        Socket serverSocket;
        InputStreamReader serverInputStream;
        BufferedReader serverInputReader;
        volatile boolean receiveThreadActive;

        /*  Constructor method to create instance of thread  */
        ReceiveMessagesThread(Socket socket) {
            serverSocket = socket;//initialising socket using socket passed in
            receiveThreadActive=true;
        }

        /*  Main method called to run instance of ReceiveMessagesThread  */
        public void run() {
            try {
                //initialise input stream + reader
                serverInputStream = new InputStreamReader(serverSocket.getInputStream());
                serverInputReader = new BufferedReader(serverInputStream);
                String fromServer;
                while ((fromServer=serverInputReader.readLine())!=null) {
                    //loops through reading + displaying messages from server as long as they are valid
                    System.out.println(fromServer);

                }
            } catch (IOException ioException) {
                /*SocketException -> breaks out of .readline() -> end loop
                  Cause: socket closed in terminate() method or server was shutdown.
                  Both -> close ChatClient instance */
                System.out.println("\nServer socket closed.");
            }finally{
                //closes ChatClient instance altogether
                closeClient();
            }
        }

        /*  Method used to cleanly shutdown thread  */
        public void terminate() {

            /* Closing server sockets breaks out of the .readLine()
               -> socket exception -> breaks loop and ends thread.
               Also close input components.    */

            if(serverInputReader!=null) {
                closeResource(serverInputReader);
            }
            if(serverInputStream!=null) {
                closeResource(serverInputStream);
            }
            if(serverSocket!=null) {
                closeResource(serverSocket);
            }
            receiveThreadActive=false;

        }

    }

    /*  Thread used to send messages to server (other clients)  */
    class SendMessagesThread extends Thread {

        Socket serverSocket;
        PrintWriter serverOutWriter;
        InputStreamReader userInpStrReader = new InputStreamReader(System.in);
        BufferedReader userInputReader = new BufferedReader(userInpStrReader);
        volatile boolean sendThreadActive;

        /*  Constructor method to create instance of SendMessagesThread  */
        SendMessagesThread(Socket passedSocket) {
            serverSocket = passedSocket;
            sendThreadActive=true;
        }

        /*  Method to return the username input upon connecting to server  */
        public String getUsername(){
            String username;

            try {
                //attempts to read username
                System.out.println("Enter username: ");
                username = userInputReader.readLine();
            }catch(Exception e){
                //if error produced, defaults to 'user'
                System.out.println("Unable to read username. Defaulting to 'user'");
                username = "User";
            }

            if(sendThreadActive) {
                //only prints message if shutdown process not started
                System.out.println("\nWelcome " + username + "!\nFeel free to send messages :)");
            }

            return username;//returns username
        }

        /*  Main code run when thread started  */
        public void run(){

            try {
                String userID = getUsername();
                serverOutWriter = new PrintWriter(serverSocket.getOutputStream(), true);

                while (true) {
                    //loops through taking input and printing to server (w/ userID)
                    //broken by terminate() method
                    String userInputStr = userInputReader.readLine();
                    serverOutWriter.println("["+userID+"]: "+userInputStr);
                }

            } catch (IOException ioException) {
                System.out.println("User input stream closed.");
            } finally{
                //calls close method of ChatClient instance - only reached when always-true broken - server shutdown
                closeClient();
            }
        }

        /*  Method to close thread and it's components/resources  */
        public void terminate() {

            sendThreadActive=false;

            System.out.println("Please hit enter to complete clean shutdown...");
            //if user doesn't hit enter, hangs on .readline() - cannot call .close() - waits for readline()

            /*  Closing resources etc if they have been initialised  */
            if(userInputReader!=null) {
                closeResource(userInputReader);
            }
            if(serverSocket!=null){
                closeResource(serverSocket);
            }
            if(serverOutWriter !=null) {
                closeResource(serverOutWriter);
            }


        }
    }

    /*  Main code for ChatClient class  */
    public static void main(String[] args){

        //pre-defined default values if no value input in args for them
        int count = 0;
        int portNo = 14001;
        String ipAddress = "localhost";

        while (count < args.length) {
            //loops through args and checks if user trying to bind to certain port or address
            if (args[count].equalsIgnoreCase("-ccp")) {
                //attempts to parse following argument as port number
                try {
                    portNo = parseInt(args[count + 1]);
                }catch(NumberFormatException numberFormatException){
                    System.out.println("Usage: not a valid number input for port.");
                }
            } else if (args[count].equalsIgnoreCase("-cca")) {
                //assigns following argument as ipAddress
                ipAddress = args[count + 1];
            }
            count++;

        }

        //creating instance of ChatClient
        ChatClient client = new ChatClient(ipAddress, portNo);
        if(client.clientActive) {
            //if successfully launched, starts the client methods
            client.go();
        }
    }
}

