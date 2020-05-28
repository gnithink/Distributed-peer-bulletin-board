
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
import java.util.SortedSet;
import java.util.TreeSet;

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
    static Object received_packet_object = null; //25

    // Node Discovery
    // unique id for each packet sent. Just increment the value after every packet sent.
    static int packet_id = 0;
    static int next_hop = Integer.MAX_VALUE;
    static int previous_hop = 0;

    // List of sets to for error checking 
    static SortedSet<Integer> received_nak_set = new TreeSet<Integer>(); 
    static SortedSet<Integer> received_probes_set = new TreeSet<Integer>();
    static SortedSet<Integer> received_ack_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_probe_set = new TreeSet<Integer>();
    static SortedSet<Integer> received_response_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_ack_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_nak_set = new TreeSet<Integer>();

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

        node_discovery();
        // election
        // elected message
        // post
        
        // String message_post = "hello from port " + my_port;
        // Post post = new Post(3460, 0, message_post);
        // send_packet(3456, post);

        // System.out.println("Entering while loop");
        // while(get_elapsed_time()<leave_time){

        //     if (receive_packet() != null){
        //         Post p = (Post) receive_packet();
        //         outputWriter.println(p.s_id +" "+p.message );
        //         received_packet_object = null;
        //     }
        // }

        // String message = "Hello World";
        // ByteBuffer buf = Charset.forName("UTF-8").encode(message);
        // // outputWriter.println(message + "Sending preceding message to port 3456 ");
        // to_address = new InetSocketAddress(local_ip, 3456);
        // datagramChannel.send(buf, to_address);
        // buf.clear();

        
        // int count = 0;
        // while(count < 1000)
        outputWriter.close();
        // System.out.println("closing file");    

    }

    public static void node_discovery() throws Exception{
        int time_out = 10000;
        int to_send_port;
        Probe probe_packet;
        Packet received_packet = null;
        long current_time_outer = System.currentTimeMillis();
        long receive_timer;
        int probe_count =0;
        

        
        // System.out.println("\n" + my_port+ " "+ ports_array);
        // while(System.currentTimeMillis() - current_time_outer < time_out){
            for(int i = 0; i < ports_array.size(); i++){
                to_send_port = ports_array.get(i);
                probe_packet = new Probe(my_port, packet_id);
                packet_id++;
                send_packet(to_send_port, probe_packet);
                // outputWriter.println("SENT PROBE to: " + to_send_port);
                sent_probe_set.add(to_send_port);
                receive_timer = System.currentTimeMillis();
                while(System.currentTimeMillis() - receive_timer < 5){
                    received_packet = (Packet) receive_packet();

                }
                
                    
                    if(received_packet == null){
                        // System.out.println("This node does not exist: " + to_send_port);
                        continue;
                    }

                    // probe m_id = 10
                    if(received_packet.m_id == 10){
                        
                        if(!received_probes_set.contains(received_packet.s_id) ){
                            outputWriter.println("PROBE receiced from " + received_packet.s_id);
                        }
                        received_probes_set.add(received_packet.s_id);
                        if(previous_hop == 0 ){
                            ProbeAck ack = new ProbeAck(my_port, packet_id);
                            packet_id++;
                            if(!sent_ack_set.contains(received_packet.s_id)){
                                // outputWriter.println("SENDED ACK to " + received_packet.s_id);
                            }
                            sent_ack_set.add(received_packet.s_id); 
                            previous_hop = received_packet.s_id;
                            outputWriter.println(get_time_to_print() + " previous hop is changed to client " + previous_hop);
                            send_packet(received_packet.s_id, ack);
                            check_ack_or_nak();
                        }

                    
                        if(received_packet.s_id < my_port && received_packet.s_id > previous_hop){
                            
                            ProbeAck ack = new ProbeAck(my_port, packet_id);
                            packet_id++;
                            send_packet(received_packet.s_id, ack);
                            if(!sent_ack_set.contains(received_packet.s_id)){
                                // outputWriter.println("SENDED ACK to " + received_packet.s_id);
                            }
                            previous_hop = received_packet.s_id;
                            outputWriter.println(get_time_to_print() + " previous hop is changed to client " + previous_hop);
                            sent_ack_set.add(received_packet.s_id); 
                            
                            check_ack_or_nak();
                            
                        }
                        else{
                            
                            ProbeNak nak = new ProbeNak(my_port, packet_id);
                            if(!sent_nak_set.contains(received_packet.s_id)){
                                // outputWriter.println("SENDED NAK to " + received_packet.s_id);
                            }
                            sent_nak_set.add(received_packet.s_id);
                            send_packet(received_packet.s_id, nak);
                            packet_id++;
                            check_ack_or_nak();  
                        }

                    }

                    else if(received_packet.m_id == 11){

                        if(!received_ack_set.contains(received_packet.s_id)){
                        outputWriter.println("ACK received from " + received_packet.s_id);
                        received_response_set.add(received_packet.s_id);
                        }
            
                        received_ack_set.add(received_packet.s_id);
                        
                        
                        if(next_hop == Integer.MAX_VALUE){
                            outputWriter.println(get_time_to_print() + " next hop is changed to client " + received_packet.s_id);
                            next_hop = received_packet.s_id;
                        }
            
                        if(received_packet.s_id > my_port && received_packet.s_id < next_hop){
                            
                            outputWriter.println(get_time_to_print() + " next hop is changed to client " + received_packet.s_id);
                            next_hop = received_packet.s_id;
                        }
                        
                    }
            
                    else if(received_packet.m_id == 12){
                        
                        if(!received_nak_set.contains(received_packet.s_id)){
                            outputWriter.println("NAK received from " + received_packet.s_id );
                            received_response_set.add(received_packet.s_id);
                        }
                        received_nak_set.add(received_packet.s_id);
                        
                        
                    }
                    
                    if(next_hop != Integer.MAX_VALUE && previous_hop != 0){
                        outputWriter.println("\n" + " myport: " + my_port + " next hop: " + next_hop + " previous hop " + previous_hop + " found");
                        break;
                    } 
                    probe_count++;
                
                 
            }
        // }

        // outputWriter.println("\n" + " myport: " + my_port + " next hop " + next_hop + " and " + previous_hop + " found");
        if(!received_response_set.isEmpty()){
            if(my_port < received_response_set.first()){
                previous_hop = received_response_set.last();
                outputWriter.println(get_time_to_print() + " previous hop is changed to client " + previous_hop);
            }
            if(my_port > received_response_set.last()){
                next_hop = received_response_set.first();
                outputWriter.println(get_time_to_print() + " next hop is changed to client " + next_hop);
            }
        }
        outputWriter.println("\n" + " myport: " + my_port + " next hop: " + next_hop + " previous hop " + previous_hop + " found");
        System.out.println("\n" + my_port + " " + received_response_set);
    }

    public static void check_ack_or_nak() throws Exception{
        Packet ack_or_nak_packet = null;
        // int probe_count = 0;
        long receive_timer;
        
            receive_timer = System.currentTimeMillis();
            while(System.currentTimeMillis() - receive_timer < 5){
                ack_or_nak_packet = (Packet) receive_packet();
            }
            
            if(ack_or_nak_packet == null){
                return;
            }
            if(ack_or_nak_packet.m_id == 11){

                if(!received_ack_set.contains(ack_or_nak_packet.s_id)){
                outputWriter.println("ACK received from " + ack_or_nak_packet.s_id);
                }

                received_ack_set.add(ack_or_nak_packet.s_id);
                received_response_set.add(ack_or_nak_packet.s_id);
                
                if(next_hop == Integer.MAX_VALUE){
                    outputWriter.println(get_time_to_print() + " next hop is changed to client " + ack_or_nak_packet.s_id);
                    next_hop = ack_or_nak_packet.s_id;
                }

                if(ack_or_nak_packet.s_id > my_port && ack_or_nak_packet.s_id < next_hop){
                    
                    outputWriter.println(get_time_to_print() + " next hop is changed to client " + ack_or_nak_packet.s_id);
                    next_hop = ack_or_nak_packet.s_id;
                }
                
            }

            else if(ack_or_nak_packet.m_id == 12){
                
                if(!received_nak_set.contains(ack_or_nak_packet.s_id)){
                    outputWriter.println("NAK received from " + ack_or_nak_packet.s_id );
                }
                received_nak_set.add(ack_or_nak_packet.s_id);
                received_response_set.add(ack_or_nak_packet.s_id);
                
            }
            return;
        

    }

    


    public static void send_packet(int port, Object message_packet) throws Exception { //26
        
        // readyChannels = select.selectNow();

        // Set<SelectionKey> selectedKeys = select.selectedKeys();
        // Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        // while(keyIterator.hasNext()) {
        //     SelectionKey temp_key = keyIterator.next();
        //     keyIterator.remove();
        //     if (temp_key.isWritable()) {
                BOS_SEND = new ByteArrayOutputStream();
                OOS_SEND = new ObjectOutputStream(BOS_SEND);
                OOS_SEND.writeObject(message_packet);
                OOS_SEND.flush();

                send_byte = new byte[BUFFER_SIZE];
                send_byte = BOS_SEND.toByteArray();
                send_byte_buffer = ByteBuffer.allocate(BUFFER_SIZE);
                send_byte_buffer = ByteBuffer.wrap(send_byte);

                to_address = new InetSocketAddress(local_ip, port);
                datagramChannel.send(send_byte_buffer, to_address);
                
                send_byte_buffer.clear();
                OOS_SEND.close();
        //     }
            
        // }

    }

    public static Object receive_packet() throws Exception{ // 27
        long read_timer = System.currentTimeMillis();
        readyChannels = select.select();
        // key.interestOps(0);
        // key = datagramChannel.register(select, SelectionKey.OP_READ);
        Set<SelectionKey> selectedKeys = select.selectedKeys();
        // outputWriter.println(key.readyOps() & key.OP_READ);
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while(keyIterator.hasNext()) {
            SelectionKey temp_key = keyIterator.next();
            if (temp_key.isReadable()) { 
                receive_buffer = ByteBuffer.allocate(BUFFER_SIZE);
                datagramChannel.receive(receive_buffer);
                receive_buffer.flip();
                receive_byte = new byte[receive_buffer.remaining()];
                receive_buffer.get(receive_byte,0,receive_byte.length);

                BIS_REC = new ByteArrayInputStream(receive_byte);
                OIS_REC = new ObjectInputStream(new BufferedInputStream(BIS_REC));
                received_packet_object = OIS_REC.readObject();
    
                OIS_REC.close();
            } 
            keyIterator.remove();
        }

        return received_packet_object;    
    }

    public static void setupSocket(int my_port){
        try{
            datagramChannel = DatagramChannel.open();
            socket = datagramChannel.socket();
            select = Selector.open();
            datagramChannel.configureBlocking(false);
            key = datagramChannel.register(select, SelectionKey.OP_WRITE | SelectionKey.OP_READ );
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

    public static String get_time_to_print(){
        String string_sec;
        String string_min;
        String string_time;

        long milli_time = System.currentTimeMillis() - start_time;
        int total_time_sec = (int) milli_time/1000;
        int time_min = (int) total_time_sec / 60;
        int time_sec = (int) total_time_sec % 60;

        if(time_sec < 10){
            string_sec = "0" + String.valueOf(time_sec) +":";
        }
        else{
            string_sec = String.valueOf(time_sec) + ":";
        }
        string_min = String.valueOf(time_min) + ":";
        string_time = string_min + string_sec;
        return string_time;
    }

}