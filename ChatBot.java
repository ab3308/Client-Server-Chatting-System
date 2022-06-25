import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Locale;
import java.util.Random;

import static java.lang.Integer.parseInt;

/*  Class used to connect a chatbot to the server  */
public class ChatBot {

    private Socket serverSocket;
    private InputStreamReader serverInputStream;
    private BufferedReader serverInpReader;
    private PrintWriter serverOutWriter;
    volatile Boolean botIsActive = true;//active until shutdown procedure

    /*  Constructor for ChatBot  */
    public ChatBot(String serverAddress, int serverPort) {
        try {
            //initialise socket to connect to server
            serverSocket = new Socket(serverAddress, serverPort);
            System.out.println("\nConnected to server:\n>Address: " +serverAddress +"\n>Port: "+serverPort);

            //initialised input/output streams etc
            serverInputStream = new InputStreamReader(serverSocket.getInputStream());
            serverInpReader = new BufferedReader(serverInputStream);
            serverOutWriter = new PrintWriter(serverSocket.getOutputStream(), true);

        } catch (SocketException socketException) {
            //error creating bot -> shutdown
            System.out.println("\nUnable to connect to server:\n>Address: " +serverAddress +"\n>Port: "+serverPort);
            shutdownBot();
        } catch (IOException e) {
            //error creating bot -> shutdown
            System.out.println("\nUnable to fully launch chatbot.");
            shutdownBot();
        }
    }

    /*  Method called to run instance of ChatBot  */
    public void go(){
        try {
            String fromServer;
            while (botIsActive && (fromServer = serverInpReader.readLine()) !=null) {
                //while bot active and server sending messages
                if (fromServer.contains("BOT.")) {
                    //if bot command included, message passed to new instance of responseThread
                    SendResponseThread responseThread = new SendResponseThread(serverSocket, fromServer);
                    responseThread.start();
                }
            }
        } catch (SocketException socketException) {
            //server closed
            System.out.println("Server socket closed (server shutdown).");
        } catch (IOException ioException) {
            System.out.println(ioException);
        }finally{
            //bot no longer active -> shutdown
            shutdownBot();
        }
    }

    /*  Method to cleanly shutdown instance of ChatBot  */
    public void shutdownBot(){
        if(botIsActive) {
            try {
                System.out.println("\nShutting down bot...");

                //if component has been initialised, it gets closed, if not - ignore
                if (serverInpReader != null) {
                    serverInpReader.close();
                }
                if (serverInputStream != null) {
                    serverInputStream.close();
                }
                if (serverOutWriter != null) {
                    serverOutWriter.close();
                }
                if (serverSocket != null) {
                    serverSocket.close();
                }

                botIsActive = false;//no longer active - shutdown
                System.out.println("Clean shutdown complete.");
            } catch (Exception causeOfShutdownFailure) {
                System.out.println("Cannot close bot cleanly. Forcing shutdown.\n" + causeOfShutdownFailure);
                System.exit(0);
            }
        }

    }

    /*  Thread to deal with messages that prompt bot  */
    class SendResponseThread extends Thread{

        Socket outputSocket;
        String clientMessage;

        String[] factsArray= {"The Eiffel Tower can be 15 cm taller during the summer",
                "Australia is wider than the moon", "It's illegal to own just one guinea pig in Switzerland",
                "The Spanish national anthem has no words", "The Japanese word 'Kuchi zamishi' is the act of eating when you're not hungry bcause your mouth is lonely"};
        //random facts from https://www.cosmopolitan.com/uk/worklife/a33367076/fun-facts-random/

        /*  Constructor method to create instance of send thread  */
        SendResponseThread(Socket serverSocket, String incomingMessage){
            //initialises message string (and removes BOT. prefix) and output socket
            clientMessage=incomingMessage.toLowerCase(Locale.ROOT);
            outputSocket=serverSocket;
        }

        /*  Method called to return a random fact from the facts array  */
        public String getFact(){
            Random random = new Random();
            return factsArray[random.nextInt(factsArray.length-1)];
        }

        /*  Main method of SendResponseThread instance  */
        public void run() {
            //print to console of person running bot to inform progress
            System.out.println("Responding to: " + clientMessage);
            String response="[BOT]: ";

            //comparing message with known prompts to return an appropriate message
            if(clientMessage.contains("hi") || clientMessage.contains("hello")){
                response=response+"Hello there!";
            }else if(clientMessage.contains("random fact")){
                response = response + getFact();
            }else{
                response = response + "That is not a valid bot message. See README.txt for details";
            }
            serverOutWriter.println(response);//sends response back to server
        }
    }

    /*  Main method of ChatBot  */
    public static void main(String[] args){
        // used to find any arguments input on command line
        int count = 0;
        int portNo = 14001;
        String ipAddress = "localhost";

        while (count < args.length) {

            //while there are remaining arguments, loops through and checks conditions
            if (args[count].equalsIgnoreCase("-cbp")) {
                //if following argument is valid number, passed as port number
                try {
                    portNo = parseInt(args[count + 1]);
                }catch(NumberFormatException numberFormatException){
                    System.out.println("Usage: not a valid number input for port.");
                }

            } else if (args[count].equalsIgnoreCase("-cba")) {
                //passes following argument as the address
                ipAddress = args[count + 1];
            }
            count++;
        }
        //initialise and start instance of chatbot
        ChatBot chatBot = new ChatBot(ipAddress, portNo);
        chatBot.go();
    }
}