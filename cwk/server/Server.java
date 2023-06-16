import java.io.*;
import java.net.*;
import java.text.BreakIterator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.PrintWriter;

public class Server {
    //define executor ,I/O for server and logging
    //To store aution items, I have implemented using nested hashmaps
    //the executor has a fixed thread pool of 30 threads as per spec
    private final Executor handler = Executors.newFixedThreadPool(30);
    private final HashMap<String, HashMap<Double,String>>bidTable = new HashMap<>();
    private FileWriter fileWriter;
    private PrintWriter logWriter;

    //method to run the server
    //establish connection between the server with the client inside the try block
    public void run() {
       
        try (ServerSocket serverSocket = new ServerSocket(6700)) {

            int port = serverSocket.getLocalPort();
            System.out.println("Server started on port " + port + ".");
            while (true) {
                Socket socket = serverSocket.accept();
                handler.execute(new RequestHandler(socket));
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    //method to handle client requests. takes in the arguments as an array
    //takes in the host address output stream aswell
    private synchronized void handleRequest(String[] args, PrintWriter writer, String hostAddress) {
        try{
            //to handle item client request
            //checks for invalid argument length
            //checks if the item already exists in the auction (is case sensitive)
            //if item exists it sends Fail to client
            //else it sends Success to client 
            if (args[0].equals("item")) {
                if(args.length!=2){
                    writer.println("Error");
                }
                boolean checkDuplicates = false;
                for (String item : bidTable.keySet()) {
                    if (item.equalsIgnoreCase(args[1])){
                        checkDuplicates = true;
                        break;
                    }
                }
                if (checkDuplicates == true){
                    writer.println("Fail");
                }
                else{
                    HashMap<Double,String> bidHost = new HashMap<Double,String>();
                    bidHost.put(0.0,"<no bids>");
                    bidTable.put(args[1], bidHost);
                    writer.println("Success.");
                }

            }
            //to handle bid client request
            //checks for invalid argument length
            //checks if the item already exists in the auction
            //if item exists it then checks the bid amount
            //if the bid amount is higher than current amount, it returns Accepted to client, updates the bid value and updates the client hostname
            //if the bid amoint is lower than current amount, it returns Rejected to client and does not update the item information
            if (args[0].equals("bid")) {
                if(args.length!=3){
                    writer.println("Error");
                }
                if(!bidTable.containsKey(args[1])){
                    writer.println("NF");
                }        
                else  {
                    boolean bidIsLow = true;
                    Double bidAmount = Double.parseDouble(args[2]);
                    HashMap<Double, String> checkBid = bidTable.get(args[1]);
                    for (Iterator<Map.Entry<Double, String>> iterator = checkBid.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<Double, String> entry = iterator.next();
                        Double innerKey = entry.getKey();
                        String value = entry.getValue();
                        if (innerKey<bidAmount){
                            bidIsLow = false;
                            break;
                        }
                    }
                    if (bidIsLow) {
                        writer.println("Bid Low");
                    } 
                    else {
                        HashMap<Double, String> updateBid = bidTable.get(args[1]);
                        for (Iterator<Map.Entry<Double, String>> iterator = updateBid.entrySet().iterator(); iterator.hasNext(); ) {
                            Map.Entry<Double, String> entry = iterator.next();
                            Double innerKey = Double.parseDouble(args[2]);
                            String value = entry.getValue();
                            String newValue = hostAddress;
                            iterator.remove();
                            updateBid.put(innerKey, newValue);
                        }
                        writer.println("Bid OK");
                    }

                }    
            } 
            //to handle show client request
            // if there are no items in the auction it returns Empty to the client
            // if there are items, it loops through the nested hashmap and sends the data as a string
            //data is sent with delimiters to allow easy line by line client output
            else if (args[0].equals("show")) {
                if (bidTable.isEmpty()) {
                    writer.println("Empty");
                }
                else{
                    for (String item : bidTable.keySet()) {
                        StringBuilder bidList = new StringBuilder();
                        HashMap<Double, String> bids = bidTable.get(item);
                        for (Double amount : bids.keySet()) {
                            String host = bids.get(amount);
                            bidList.append(amount).append(" : ").append(host).append(" ");
                        }
                        writer.print(item + " : " + bidList+",");
                }
            }
            //flush the writer
            writer.flush();
        }
    }
    catch(NullPointerException n){
        System.out.println("Client Closed Connection Abrubtly");
    }
}

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
    //define request handler
    private class RequestHandler implements Runnable {
        private final Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            //try to create log file
            //return error message if log file cannot be created
            try {
                fileWriter = new FileWriter("log.txt", true);
            } catch (IOException e) {
                System.out.println("Error creating log file: " + e.getMessage());
                return;
            }
            logWriter = new PrintWriter(fileWriter); 
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                String clientRequest = reader.readLine();
                //split the client request into array for the request handler
                String[] strArray = clientRequest.split(" ");
                //get client address
                String clientAddress = socket.getInetAddress().getHostAddress();
                //handle the request
                handleRequest(strArray, writer,clientAddress);
                //log the request into logfile and close the logfile
                logWriter.println(LocalDate.now()+" | "+ LocalTime.now()+" | "+clientAddress +" | "+clientRequest);
                logWriter.close();
            } 
            //catch I/O errors
            catch (IOException e) {
                System.out.println("Error handling client request: " + e.getMessage());
            }
            //catch invalid client side requests
            catch(NullPointerException n){
                System.out.println("Invalid Request Recieved");
            }
        }
    }
}
