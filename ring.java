
// for file reading and writing
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.FileNotFoundException;

// for data structures for storing ports, input_messages
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

//Sockets
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
// NIO sockets
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;


@SuppressWarnings("serial")
public class ring extends Exception {

    // list of class variables 
    static File output; // 1
    static File config; // 2
    static File input; // 3
    static PrintWriter outputWriter; // 4
    static Scanner configScan; // 5
    static Scanner inputScan; // 6

    static ArrayList<Integer> ports_array = new ArrayList<Integer>(); //10
    static int my_port = 0; // 11
    static int join_time = 0; // 12
    static int leave_time = 0; // 13

    static DatagramChannel datagramChannel; 
    static DatagramSocket socket;
    static DatagramPacket packet;
    static InetAddress local_ip;
    static InetSocketAddress address;
    static InetSocketAddress to_address;
    // static Selector select;

    // Map {time_in_milliseconds : message_string}. They are in sorted order because of treemap
    static SortedMap<Integer, String> messages_map = new TreeMap<Integer, String>();

    public static void main(String[] args) throws Exception { // 7

        cmdLineArgsParser(args); // 8
        configFileParser(configScan, config); // 9
        inputFileParser(inputScan, input); //14

        setupSocket(my_port);

        String message = "Hello World";
        ByteBuffer buf = Charset.forName("UTF-8").encode(message);
        System.out.println("Sending buffer" + buf);
        if(my_port == 3460){
            outputWriter.write(message + "Sending preceding message to port 3456");
            outputWriter.close();
        }
        
        datagramChannel.send(buf, to_address );
        buf.clear();

        // packet = new DatagramPacket(buf, buf.length, local_ip, 3456);
        // socket.send(packet);


        
        ByteBuffer received = ByteBuffer.allocate(100);
        // DatagramPacket received_packet = new DatagramPacket(received, received.length);
        // if(my_port == 3456){
            System.out.println("before" + received);
            datagramChannel.receive(received);
            received.flip();
            System.out.println("after " + received);
            String received_string = new String(received.array(),0,received.remaining(), "UTF-8");
            System.out.println("converted to string length " + received_string.length());
        // }
        
        
        System.out.println(received_string);
        outputWriter.write("Received String :" + received_string);
        // received.clear();
        
        // socket.receive(received_packet);
        // outputWriter.write("The received packet is :::" + data(received));
        // if(my_port == 3456){
            
        //     System.out.println("my port is " + my_port);
        // }
        outputWriter.close();

    }
    // public static StringBuilder data(byte[] a) 
    // { 
    //     if (a == null) 
    //         return null; 
    //     StringBuilder ret = new StringBuilder(); 
    //     int i = 0; 
    //     while (a[i] != 0) 
    //     { 
    //         ret.append((char) a[i]); 
    //         i++; 
    //     } 
    //     return ret; 
    // } 

    // public static StringBuilder data(Byte[] a) 
    // { 
    //     if (a == null) 
    //         return null; 
    //     StringBuilder ret = new StringBuilder(); 
    //     int i = 0; 
    //     while (a[i] != 0) 
    //     { 
    //         ret.append((char) a[i]); 
    //         i++; 
    //     } 
    //     return ret; 
    // } 

    public static void setupSocket(int my_port){

        try{
            datagramChannel = DatagramChannel.open();
            
            // socket = new DatagramSocket(my_port);
            socket = datagramChannel.socket();
            // select = Selector.open();
            // datagramChannel.configureBlocking(false);
            // datagramChannel.register(select, SelectionKey.OP_ACCEPT);
            local_ip = InetAddress.getLocalHost();
            address = new InetSocketAddress(local_ip, my_port);
            socket.bind(address);
            
            // to be sent to this address
            to_address = new InetSocketAddress(local_ip, 3456);
        }
        catch(Exception e){
            System.out.println("Error while setting up the socket");
            e.printStackTrace();
        }


    }

    // 14th function
    public static void inputFileParser(Scanner inputScan, File input){

        String[] line;
        String[] line_split;
        int message_time;
        String message;

        try{
            while(inputScan.hasNextLine()){
                line = inputScan.nextLine().split("\t", 2);
                line_split = line[0].split(":");
                message_time = ((Integer.parseInt(line_split[0])*60) + Integer.parseInt(line_split[1])) * 1000;
                message = line[1];
                messages_map.put(message_time, message);
            }

        }

        catch(Exception e){
            System.out.println("Error parsing input file " + input.getName());
        }
    }

    //9th function
    public static void configFileParser(Scanner configScan, File config){
        String[] line;
        String[] line_split;
        int start_port;
        int end_port;
          
        try{
            // splitting line by space line[0] = client_port . line[1] = start_port-end_port
            line = configScan.nextLine().split(" ");  
            line_split = line[1].split("-");
            start_port = Integer.parseInt(line_split[0]);
            end_port = Integer.parseInt(line_split[1]);
            
            // splitting the line by space line[1] = my_port. line[1] = port_value
            line = configScan.nextLine().split(" ");
            my_port = Integer.parseInt(line[1]);

            // splitting the line by space line[0] = join_time. line[1] = minutes:seconds
            line = configScan.nextLine().split(" ");
            line_split = line[1].split(":");
            join_time = ((Integer.parseInt(line_split[0])*60) + Integer.parseInt(line_split[1])) * 1000;

            // splitting the line by space line[0] = leave_time. line[1] = minutes:seconds
            line = configScan.nextLine().split(" ");
            line_split = line[1].split(":");
            join_time = ((Integer.parseInt(line_split[0])*60) + Integer.parseInt(line_split[1])) * 1000;

            //filling up the ports array from my_ports + 1 to end_port
            for (int i = my_port +1; i <= end_port; i++){
                ports_array.add(i);
            }


            //filling up the ports_array from start_port to my_port-1
            for (int i = start_port; i < my_port; i++){
                ports_array.add(i);
            }
            // This way the ports_array is filled in the order in which the probing is to be done 

        }
        catch(Exception e){
            System.out.println("Error parsing config file " + config.getName());
            e.printStackTrace();
            System.exit(1);        }

    }

    // 8th Function
    public static void cmdLineArgsParser(String[] args){

        if(args.length != 6){
            System.out.println("Usage: Java ring -c <configfile> -i <inputfile> -o <outputfile> ");
            System.exit(1);
        }
        if(!args[0].contentEquals("-c")){
            System.out.println("First argument should be -c");
            System.out.println("Usage: Java ring -c <configfile> -i <inputfile> -o <outputfile> ");
            System.exit(1);
        }
        if(!args[2].contentEquals("-i")){
            System.out.println("Third argument should be -i");
            System.out.println("Usage: Java ring -c <configfile> -i <inputfile> -o <outputfile> ");
            System.exit(1);
        }
        if(!args[4].contentEquals("-o")){
            System.out.println("Fifth argument should be -o");
            System.out.println("Usage: Java ring -c <configfile> -i <inputfile> -o <outputfile> ");
            System.exit(1);
        }

        try{
            config = new File(args[1]);
            configScan = new Scanner(new BufferedReader(new FileReader(config))); 
        } 
        catch(Exception e){
            System.out.println("Error opening config file" + args[1]);
            e.printStackTrace();
            System.exit(1);

        }
        try{
            input = new File(args[3]);
            inputScan = new Scanner(new BufferedReader(new FileReader(input))); 
        } 
        catch(FileNotFoundException e){
            System.out.println("Error opening input file" + args[3]);
            e.printStackTrace();
            System.exit(1);
        }

    
        output = new File(args[5]);
        
        if(output.isFile()){
            System.out.println("Output file with name "+ args[5] + " already exists");
            System.out.println("Delete the existing output files and run or Use a different filename");
            System.exit(1);
        }
        try{
            outputWriter = new PrintWriter(new FileWriter(output));  
            
        } 
        catch (IOException e){
            System.out.println("Error creating the output file" + args[5]);
            e.printStackTrace();
        }

    }
}