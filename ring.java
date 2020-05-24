
// for file reading and writing
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.PrintWriter;
import java.util.Scanner;
import java.io.FileNotFoundException;

// for data structures for storing ports, input_messages
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;

//Sockets
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
// NIO sockets
import java.nio.channels.DatagramChannel;
// import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;

// Serializing objects
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;



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


    // Timing and clocks
    static long join_time; // 12
    static long leave_time ; // 13
    static boolean running ; //25
    static long start_time; // 26
    static long stop_time; //27
    static long elapsed_time; //28

    // DataGram Channel and Select
    static DatagramChannel datagramChannel; 
    static DatagramSocket socket;
    static DatagramPacket packet;
    static InetAddress local_ip;
    static InetSocketAddress address;
    static InetSocketAddress to_address;
    static Selector select;
    static SelectionKey key;
    static int readyChannels;

    // Bytearray Buffers and 
    static int BUFFER_SIZE = 1024; // 16

    static ByteBuffer send_byte_buffer; // 17
    static byte[] send_byte; // 18
    static ByteArrayOutputStream BOS_SEND; // 19
    static ObjectOutputStream OOS_SEND; // 20

    static ByteBuffer receive_buffer; // 21
    static byte[] receive_byte; // 22
    static ByteArrayInputStream BIS_REC; // 23
    static ObjectInputStream OIS_REC; // 24
    static Object read_object;

    // Map {time_in_milliseconds : message_string}. They are in sorted order because of treemap
    static SortedMap<Integer, String> messages_map = new TreeMap<Integer, String>();

    public static void main(String[] args) throws Exception { // 7

        

        cmdLineArgsParser(args); // 8
        configFileParser(configScan, config); // 9
        inputFileParser(inputScan, input); //14
        Thread.sleep(join_time);

        // System.out.println("The process with id " + my_port + "is awake" + "after" + join_time );
        start_clock();
        setupSocket(my_port); //15

        // Sending message
        // if(my_port != 3456){
        String message_post = "hello from port " + my_port;
        Post post = new Post(my_port, 0, message_post);

        BOS_SEND = new ByteArrayOutputStream();
        OOS_SEND = new ObjectOutputStream(BOS_SEND);
        OOS_SEND.writeObject(post);
        OOS_SEND.flush();

        send_byte = new byte[BUFFER_SIZE];
        send_byte = BOS_SEND.toByteArray();
        send_byte_buffer = ByteBuffer.allocate(BUFFER_SIZE);
        send_byte_buffer = ByteBuffer.wrap(send_byte);

        to_address = new InetSocketAddress(local_ip, 3456);
        datagramChannel.send(send_byte_buffer, to_address);
        send_byte_buffer.clear();
        OOS_SEND.close();
        // }


    
        // String message = "Hello World";
        // ByteBuffer buf = Charset.forName("UTF-8").encode(message);
        // // outputWriter.println(message + "Sending preceding message to port 3456 ");
        // to_address = new InetSocketAddress(local_ip, 3456);
        // datagramChannel.send(buf, to_address);
        // buf.clear();

        System.out.println("Entering while loop");
        // int count = 0;
        // while(count < 1000)
        while(get_elapsed_time() < leave_time){
            readyChannels = select.selectNow();

            if(readyChannels == 0) continue;
            Set<SelectionKey> selectedKeys = select.selectedKeys();
            // outputWriter.println(selectedKeys);
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while(keyIterator.hasNext()) {

                SelectionKey temp_key = keyIterator.next();
                // outputWriter.println(temp_key.isReadable() + " " + temp_key.isWritable());
                if (temp_key.isReadable()) {
                    // a channel is ready for reading
                    receive_buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    datagramChannel.receive(receive_buffer);
                    receive_buffer.flip();
                    receive_byte = new byte[receive_buffer.remaining()];
                    receive_buffer.get(receive_byte,0,receive_byte.length);


                    BIS_REC = new ByteArrayInputStream(receive_byte);
                    OIS_REC = new ObjectInputStream(new BufferedInputStream(BIS_REC));
                    read_object = OIS_REC.readObject();
                    Post p = (Post) read_object;
                    OIS_REC.close();
                    outputWriter.println(p.s_id +" "+p.message );

                    
                    // ByteBuffer received = ByteBuffer.allocate(100);
                    // datagramChannel.receive(received);
                    // received.flip();
                    // String received_string = new String(received.array(),0,received.remaining(), "UTF-8");
                    // outputWriter.println("Received String : " + received_string);
                    // received.clear();
                } 
                // else if (temp_key.isWritable()) {
                //     // a channel is ready for writing
                // }
                keyIterator.remove();
                // count++;

            }
        }
        outputWriter.close();
        System.out.println("closing file");    

    }

    public static void setupSocket(int my_port){
        try{
            datagramChannel = DatagramChannel.open();
            socket = datagramChannel.socket();
            select = Selector.open();
            datagramChannel.configureBlocking(false);
            key = datagramChannel.register(select, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            local_ip = InetAddress.getLocalHost();
            address = new InetSocketAddress(local_ip, my_port);
            socket.bind(address);
            // to be sent to this address
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
            leave_time = ((Integer.parseInt(line_split[0])*60) + Integer.parseInt(line_split[1])) * 1000;

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

    public static void start_clock() {
        start_time = System.currentTimeMillis();
        running = true;
    }
    public static void stop_clock() {
        stop_time = System.currentTimeMillis();
        running = false;
    }
    public static long get_elapsed_time() {
        if (running) {
          elapsed_time = System.currentTimeMillis() - start_time;
        } else {
          elapsed_time = stop_time - start_time;
        }
        return elapsed_time;
    }

}