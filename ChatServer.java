import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static java.lang.Integer.parseInt;

/* Class used to host a chat server */
public class ChatServer extends Thread {

    private ServerSocket serverSocket;
    private ConcurrentHashMap<Socket, ServerThread> socketThreadCHM;

    /* Constructor method for ChatServer class */
    public ChatServer(int port) {
        try {
            //Initialising server socket and ConcHashMap (track active threads/sockets)
            socketThreadCHM = new ConcurrentHashMap<>();
            serverSocket = new ServerSocket(port);

        } catch (IOException e) {
            //If error, server cannot be created -> call close() method
            System.out.println("Cannot create server.\n");
            serverShutdown(socketThreadCHM, serverSocket);
        }
    }

    /* Close method to shutdown server and all connected clients */
    public static void serverShutdown(ConcurrentHashMap<Socket, ServerThread> socketsThreadsMap, ServerSocket serverSocket) {
        try {

            for (Map.Entry<Socket, ServerThread> mapEntry : socketsThreadsMap.entrySet()) {
                //loops through active sockets/threads: closes sockets, calls .terminate() method for threads
                mapEntry.getKey().close();
                mapEntry.getValue().terminate();
                socketsThreadsMap.remove(mapEntry.getKey());
            }

            if(!serverSocket.isClosed()) {
                //closes the server socket - prevent further connections
                serverSocket.close();
            }

            System.out.println("\nClean shutdown.");

        } catch (IOException e) {
            //if unable to close cleanly, forces shutdown
            System.out.println(e);
            System.out.println("\nForcing shutdown...");
            System.exit(0);
        }


    }

    /* Main method to launch and run server */
    public static void main(String[] args) {

        //used to find args from command line
        int count = 0;
        int portNo = 14001;

        while (count < args.length) {
            //while there are remaing arguments, loops through to check for -csp to bind server port
            if (args[count].equalsIgnoreCase("-csp")) {
                //if command entered, attempts to bind to port specified
                try {
                    portNo = parseInt(args[count + 1]);
                }catch(NumberFormatException numberFormatException){
                    System.out.println("Usage: not a valid number input for port.");
                }

                break;//takes first port input if they input multiple
            }
            count++;
        }//getting args

        ChatServer server;
        //initialised within try-catch, defaults server to port 14001 + address localhost if error
        try {
            server = new ChatServer(portNo);//change to pass through port number
            System.out.println("Server started via port: "+portNo);
        } catch (Exception e) {
            System.out.println("\nError creating server on port:"+portNo);
            server = new ChatServer(14001);//change to pass through port number
            System.out.println("\nServer started via default port: 14001");
        }

        server.start();
        //starting server, then an exit thread to continuously check for EXIT command
        ExitThread exitThread = new ExitThread(server.serverSocket);
        exitThread.start();

        try {

            while (exitThread.exitThreadActive) {
                //loops through accepting connection while EXIT command not entered
                Socket clientSocket = server.serverSocket.accept();
                System.out.println("Connection accepted on: " + server.serverSocket.getLocalPort() + ":" + clientSocket.getPort());

                //starting worker thread to deal with the connection. Connection details added to CHM.
                ServerThread serverThread = new ServerThread(clientSocket, server.socketThreadCHM);
                serverThread.start();
                server.socketThreadCHM.put(clientSocket, serverThread);

            }
        }  catch (IOException e) {
            System.out.println("Cannot accept client connection: "+e);
            /* SocketException caused by exit thread when user hits 'EXIT'
            this breaks out of .accept() to enable server shutdown. */

        } finally {
            //If not - exitThread.isActive returns false after next connection - breaks .accept()
            System.out.println("\nClosing down server...");
            serverShutdown(server.socketThreadCHM, server.serverSocket);//closes server
        }
    }
}

/* Thread used to constantly check for EXIT command from server host */
class ExitThread extends Thread{
    //Necessary readers for user input
    InputStreamReader serverHostInputStream;
    BufferedReader serverHostInpReader;

    ServerSocket servSocketToClose;//closed in order to break out of .accept() in main()
    public volatile Boolean exitThreadActive = true;

    ExitThread(ServerSocket serverSocket){
        //constructor for ExitThread - initiates input readers and server socket
        serverHostInputStream = new InputStreamReader(System.in) ;
        serverHostInpReader = new BufferedReader(serverHostInputStream);
        servSocketToClose = serverSocket;
    }

    public void run(){
        //loops through until user enters 'exit' in the console
        while(true){
            try {
                if (serverHostInpReader.readLine().equalsIgnoreCase("exit")) {
                    break;
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        //after user enters 'exit', calls shutdown() method to cleanly close thread
        shutdown();
    }

    public void shutdown(){
        //closes thread cleanly - closes readers etc
        try {
            serverHostInputStream.close();
            serverHostInpReader.close();
            servSocketToClose.close();
        }catch (IOException ioException) {
            System.out.println("Failure to close ExitThread instance cleanly.");
        }finally {
            //no matter what, thread no longer active, therefore false
            exitThreadActive = false;
        }
    }

}

/* Worker thread for the server to deal with connections */
class ServerThread extends Thread {
    //necessary socket, CHM and readers to deal with client
    Socket clientSocket;
    ConcurrentHashMap<Socket, ServerThread> socketThreadConcHashMap;
    BufferedReader clientReader;
    InputStreamReader clientCharStream;
    Boolean terminatedAlready = false;//prevents termination process occurring twice when server shutdown.

    ServerThread(Socket socketIn, ConcurrentHashMap<Socket, ServerThread> socketThreadMap) {
        //constructor for ServerThread - initialised CHM (track all clients) and socket for client dealing with
        socketThreadConcHashMap = socketThreadMap;
        clientSocket = socketIn;
    }

    public void terminate(){
        //method to close the client thread cleanly

        int thisClientPort = clientSocket.getPort();

        if(!terminatedAlready) {
            try {
                //if not closed already, closes socket and readers while removing client from CHM
                socketThreadConcHashMap.remove(clientSocket);
                clientSocket.close();
                clientReader.close();
                clientCharStream.close();

            } catch (IOException ioException) {
                System.out.println("Unable to terminate thread cleanly on port: " + thisClientPort);
            }
        }

    }

    public void run() {
        //method called when ServerThread instance started
        try {
            //initialising input stream + reader
            clientCharStream = new InputStreamReader(clientSocket.getInputStream());
            clientReader = new BufferedReader(clientCharStream);

            String clientInput;
            while ((clientInput = clientReader.readLine())!=null) {

                //waits until message received from it's client

                for (Map.Entry<Socket, ServerThread> mapEntry : socketThreadConcHashMap.entrySet()) {

                    if(mapEntry.getKey()!=null) {
                        //loops through active clients and sends the message to all
                        PrintWriter clientOut = new PrintWriter(mapEntry.getKey().getOutputStream(), true);
                        clientOut.println(clientInput);
                    }else{
                        //calls .terminate() method of other client's thread if no longer active. Removes from CHM.
                        mapEntry.getValue().terminate();
                        socketThreadConcHashMap.remove(mapEntry.getKey());
                    }
                }
            }
        }  catch (IOException e) {
            //error thrown when the client socket is closed, calls terminate method to close this thread
            terminate();
        }
    }
}
