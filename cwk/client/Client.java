import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    //define the socket, Input and output Stream
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    //method to connect to the server
    public void connect() throws IOException {
        //create new sockets ,Input and output stream
        //connects to port 6700
        socket = new Socket("localhost", 6700);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    //method to close the socket and catches any exceptions that may occour
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    //method to send request from client to server.
    //also retures the server response as a string
    public String sendRequest(String request) {
        writer.println(request);
        try {
            String response = reader.readLine();
            return response;
        } catch (IOException e) {
            System.out.println("Error receiving response: " + e.getMessage());
        }
        return "";
    }

    public static void main(String[] args) {
        //generic request message to make sure that client handles too little
        //and too man arguments
        if (args.length == 0 || args.length>3) {
            System.out.println("Usage: java Client [request]");
            System.exit(0);
        }
        //I implement the whole client send/recieve request process in 
        //a try block
        try {
            Client client = new Client();
            client.connect();
            //the first conditional statement handles sever response for the show command
            //if there are no items in the auction prints respective message
            //if there are items extract them from server and read line by line
            if (args[0].equals("show")){
                String response = client.sendRequest(args[0]);
                if (response.equals("Empty")){
                    System.out.println("There are no items in the auction");
                }
                else{
                    String[] lines = response.split(",");
                    for (String i: lines){
                        System.out.println(i);
                    }
                }
            }
            //this conditional handles server response for item command
            //prints a usage message if the command was entered incorrectly
            //if the item exists it returns Failure (it is case sensitive)
            //if the item does not exist it returns Success.
            else if (args[0].equals("item")){
                if (args.length !=2){
                    System.out.println("Usage: java item itemname");
                    return;
                }
                String response = client.sendRequest(args[0] +" "+args[1]);
                if (response.equals("Success.")){
                    System.out.println(response);
                }
                else if(response.equals("Fail")){
                    System.out.println("Failure.");
                }
                else if (response.equals("Error")){
                    System.out.println("Usage: java Client item itemname");
                }
            }

            //this conditional handles server response for bid command
            //prints a usage message if the command was entered incorrectly
            //prints Rejected if the item is not there in the auction
            //if the bid is lower it prints Rejected
            //if the bid is higher it prints Accepted
            else if (args[0].equals("bid")){
                if (args.length !=3){
                    System.out.println("Usage: java Client bid itemname amount");
                    return;
                }
                String response = client.sendRequest(args[0] +" "+args[1]+" "+args[2]);
                if (response.equals("NF")){
                    System.out.println("Rejected");
                }
                else if (response.equals("Bid Low")){
                    System.out.println("Rejected");
                }
                else if (response.equals("Bid OK")){
                    System.out.println("Accepted");
                }
                else if (response.equals("Error")){
                    System.out.println("Usage: java Client bid itemname price");
                }
            }
            //to handle any other invalid commands
            else{
                System.out.println("Usage: Java Client [request]");
                return;
            }
            //close the client
            client.close();
        } 
        catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
        catch (NullPointerException e) {
            System.out.println("Did Not Recieve Server Response");
        }
    }
}
