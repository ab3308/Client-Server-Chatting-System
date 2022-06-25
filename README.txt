==============================================
||             HOSTING A SERVER             ||
==============================================

    To host a server, read this:

	-------------------
	RUNNING THE SERVER:
	-------------------

	1.  Direct your command line to the correct directory for the serverTesting class.
	2.  Compile the server class using 'javac serverTesting.java'
	3.  Run the server using 'java serverTesting'
	4.  The server should now be running and accepting connections.
	    If not, see the console for details regarding the error.
    	5.  To shut the server down cleanly, enter 'EXIT' on the terminal.
            This will disconnect all connected clients, before safely closing.

	----------------
	CHANGE THE PORT:
	----------------

	You can change the port the server binds to
	when running the server from the command line.

	To do so, use '-csp <port number>', where you input the desired port number.
	I.e.    java serverTesting -csp 14001

	If not specified, the server defaults to port number 14001.

==============================================
||       USING THE SERVER AS A CLIENT       ||
==============================================

    Clients, read this:

	-----------------------
	CONNECTING AS A CLIENT:
	-----------------------

	1.  Ensure the ChatClient and ChatBot classes are in the same folder.
	2.  Direct the command line to this folder.
	3.  Compile the classes using 'javac *.java'
	4.  Run the client class using 'java ChatClient'
	5.  You will be prompted to enter a username, this is to be identified in the chat system.
	6.  After entering a username, you will be able to send/receive messages from other users.

	--------------------------------
	CHANGING THE PORT OR IP ADDRESS:
	--------------------------------

	- Changing the port:

	    To change the port, use '-ccp <port number>' in your command line
	    when running the ChatClient class.

	    I.e. 'java ChatClient -ccp 14001'

	    Otherwise, the Client binds to port 14001 by default.

    	- Changing the IP address:
          To change the IP address, use '-cca <IP address>' in your command line.
          
	  I.e. 'java ChatClient -cca 192.168.10.250'
	  Otherwise, 'localhost' is used as the default address.

    	- You can change both when running from the command line:

          I.e. 'java ChatClient -cca 192.168.10.250 -ccp 14001'

	------------------
	USING THE CHATBOT:
	------------------

	1.  Firstly, ensure both the chatbot and yourself are connected.
	2.  To interact with the chatbot, prefix your message with 'BOT.'.
	    I.e. BOT. hi
    	3.  The chatbot will then issue a response if it is a known prompt.
            Otherwise, it will reply informing you it is not a valid bot prompt.
    	4.  You can use the chatbot for basic questions, covered below.

	--------------
	KNOWN PROMPTS:
	--------------

	Below are a set of prompts to interact with the chatbot. They are not case-sensitive.

	To use them, simply type in the general format:
	'BOT.<message prompt>', i.e. 'BOT.hello'.
	Then, send this as if it were a normal message.

	-   'Hi' / 'Hello'  -   Greet the bot :)
	-   'Random fact'   -   Ask the bot for an interesting fact!

	Currently, only a few commands are available.
	More can be added in a further iteration based on user requests.

==============================================
||            CONNECTING A BOT              ||
==============================================

    -   Connecting a bot is the exact same process as connecting as a client.

    -   Instead of 'ChatClient', run 'java Chatbot' from your command line (after compiling with javac)

        Once again, you can bind to a certain port or address using -cbp or -cba respectively.

        >Change ChatBot port: 'java ChatBot -cbp <new port>'
        >Change ChatBot address: 'java ChatBot -cba <new address>'
        >Or change both: 'java ChatBot -cba <new address> -cbp <new port>'

    -   This will attempt to connect the bot.

    -   Depending on the outcome, you will be informed of it's status.

    -   If connected, the bot will respond to other clients with scripted responses
        if a prompt is met. These prompts are detailed above.

    -   To use the prompts, prefix your message: 'BOT.'
        I.e. 'BOT. hi'
        Otherwise, the bot will disregard your message.

==============================================
||        INFORMATION FOR DEVELOPERS        ||
==============================================

Classes and their methods:

    ----------------
    ChatServer.java:
    ----------------
    (Extends thread)

    - ChatServer(int port):
        >Uses port number to create a server socket used to host the server.
        >Initialises concurrent hashmap (CHM) to track active threads and
         corresponding client sockets.

    - serverShutdown(CHM, ServerSocket):
        >Loops through CHM: closes sockets and calls .terminate() methods of their
         corresponding server worker threads.
        >Then closes the server socket if not already closed.

    - main(String[] args):
        >It takes args passed from the command line and assigns these values
         appropriately depending on the command (port)
        >Creates + starts instance of ChatServer.
        >Creates + starts instance of ExitThread.
        >Then continues to loop and accept client connections until the user calls
         the EXIT method. When connections are accepted, an instance of serverThread
         is created to deal with the connection.

    ----------------
    ExitThread.java:
    ----------------
    (Extends thread)

    - ExitThread(ServerSocket):
        >Initialises input stream/reader
        >Passes through server socket

    - run():
        >Main code run when thread instance started
        >Loops until user input equals 'exit'
        >When this conditions is met, it breaks out and begins the server shutdown process.

    - shutdown():
        >Closes the input streams etc used by the ExitThread instance

    ------------------
    ServerThread.java:
    ------------------
    (Extends thread)

    - ServerThread(Socket, CHM):
        >Constructor to pass through the client's socket that it is handling along
         with the CHM to track all connected clients.

    - terminate():
        >Removes it's corresponding client from the CHM
        >Closes client socket
        >Closes reader/stream used by ServerThread instance

    - run():
        >Reads input from it's client
        >Sends this message to all connected clients in CHM.
        >In the process of sending message, identifies when a client has forced shutdown on their end:
            Calls .terminate() method on their corresponding worker thread
            Removes client from CHM

    ----------------
    ChatClient.java:
    ----------------

    - ChatClient(String address, int port):
        >Constructor - initialise server socket - make connection to server

    - go():
        >Initialises + starts instance of ReceiveMessagesThread
        >Initialises + starts instance of SendMessagesThread

    - closeClient():
        >Closes server socket if not already closed
        >Calls .terminate() methods of receive and send message threads.
        >Completes shutdown of ChatClient instance

    - closeResource(Closeable resource):
        >Called throughout class to close resources cleanly

    - main(String[] args):
        >Checks args for user input on command line - if so binds to certain port/address
        >Creates instance of ChatClient
        >If successful, calls .go() method on this instance.

    ---------------------------
    ReceiveMessagesThread.java:    (sub-class of ChatClient)
    ---------------------------

    - ReceiveMessagesThread(Socket socket):
        >Constructor to pass through server socket to receive messages from
        >Sets thread active status to true

    - run():
        >Loops through taking inputs from the server socket input stream
        >Prints these inputs to the user
        >Loop broken when server socket closed

    - terminate():
        >Closes reader/input stream used to read from the socket (server)
        >Closes server socket if not already
        >Changes thread active status to false

    ------------------------
    SendMessagesThread.java:    (sub-class of ChatClient)
    ------------------------

    - SendMessagesThread(Socket passedSocket):
        >Constructor to pass through server socket to send messages to
        >Sets thread active status to true

    - String getUsername():
        >Method to take user input for a username to identify them on the system

    - run():
        >Loops through taking inputs from client until shutdown process begins
         This requires user to hit enter to complete shutdown,
         otherwise the code hangs on .readline() when attempting to .close() the reader.
        >Sends client input as messages to server
        >Ends when server shutdown process initiated

    - terminate():
        >Closes reader/input stream used to read from the client console
        >Closes server socket if not already
        >Changes thread active status to false

    -------------
    ChatBot.java:
    -------------

    - ChatBot(String serverAddress, int serverPort):
        >Constructor - initialises server socket to connect to server
        >Initialises input stream/reader and output writer to interact with server
        >Changes bot active status to true if successful

    - go():
        >While bot active, loops messages received from server
        >If messages contains bot prompt, passes message to new instance of
         SendResponseThread. Then starts this instance.

    - shutdownBot():
        >Closes bot components (readers, socket etc)
        >If unsuccessful, forces shutdown using System.exit()

    - main(String[] args):
        >Checks args for user input on command line - if so binds to certain port/address
        >Creates and starts instance of ChatBot using constructor, then .go()

    ------------------------
    SendResponseThread.java:    (sub-class of ChatBot.java)
    ------------------------

    - SendResponseThread(Socket, String):
        >Constructor - passes through and initialises server socket
        >Passes through client message

    - getFact():
        >Returns random fact from factsArray

    - run():
        >Compares client message against known prompts
        >Send appropriate response back to server
