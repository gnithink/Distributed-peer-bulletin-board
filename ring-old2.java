
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
import java.net.SocketTimeoutException;


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
    static DatagramSocket socket = null;
    static DatagramPacket packet;
    static InetAddress local_ip;
    static InetSocketAddress address;
    static InetSocketAddress to_address;


    // Bytearray Buffers and 
    static int BUFFER_SIZE = 1024; // 16

 
    static byte[] send_byte; // 18
    static ByteArrayOutputStream BOS_SEND; // 19
    static ObjectOutputStream OOS_SEND; // 20

    
    static byte[] receive_byte; // 22
    static ByteArrayInputStream BIS_REC; // 23
    static ObjectInputStream OIS_REC; // 24
    static Object received_packet_object = null; //25

    // Node Discovery
    // unique id for each packet sent. Just increment the value after every packet sent.
    static int packet_id = 0;
    static int next_hop = Integer.MAX_VALUE;
    static int previous_hop = -1;
    static boolean receive_probe_zero = false;
    static boolean receive_probe_ack = false;
    static boolean receive_probe_nak = false;

    // start and end port also used to to create ports array
    static int start_port;
    static int end_port;
    

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

    // Receive Thread
    // making it static throws an error. So keeping it private
    receive_thread recv_messages_till_timeout;

    // Sending messages Thread
    send_thread send_messages;

    public static void main(String[] args) throws Exception{
        Thread.sleep(join_time);
        cmdLineArgsParser(args); // 8
        configFileParser(configScan, config); // 9
        inputFileParser(inputScan, input); //14
        start_clock();
        setupSocket(my_port);
        outputWriter.println("Hello World");
        
        ring RING = new ring();
        


        try{
        ring_function(RING);
        
        }
        catch (Exception e){
            System.out.println("Error calling ring_function");
            e.printStackTrace();
        }
        // System.out.println(received_probes_set);
        

    }

    class send_thread extends Thread{
        int to_send_port;
        Probe probe_packet;
        int temp_node;
        @Override
        public void run(){
            try{
                receive_probe_ack = false;
                for (int i = 0; i < ports_array.size(); i++){
                    to_send_port = ports_array.get(i);
                    probe_packet = new Probe(my_port, packet_id);
                    packet_id++;
                    send_packet(to_send_port, probe_packet);
                    Thread.sleep(50);
                    
                }
                Thread.sleep(2000);
                // System.out.println("AT send thread " + received_probes_set);
                Iterator<Integer> iterator = received_probes_set.iterator();
                while(iterator.hasNext()){
                    temp_node = iterator.next();
                    if(temp_node == previous_hop){
                        ProbeAck ack = new ProbeAck(my_port, packet_id);
                        packet_id++;
                        send_packet(previous_hop, ack);   
                        Thread.sleep(50);
                    }
                    else{
                        ProbeNak nak = new ProbeNak(my_port, packet_id);
                        packet_id++;
                        send_packet(temp_node, nak);
                        Thread.sleep(50);

                    }
                }
                System.out.println("\n" + my_port + " " + received_response_set);
                
            }

            
            catch (Exception e){
                e.printStackTrace();
            }
            
            

        }
    }

    public void send(){
        send_messages = new send_thread();
        send_messages.start();
        
    }

    class receive_thread extends Thread{
        @Override
        public void run(){
            try{
                long time_out = 2000;
                Packet recv_packet = null;
                long recv_timer = System.currentTimeMillis();
                while(System.currentTimeMillis() - recv_timer < time_out){
                    recv_packet = (Packet) receive_packet();

                    if (recv_packet == null){
                        continue;
                    }
                    
                    if(recv_packet.m_id == 10){
                        if(!received_probes_set.contains(recv_packet.s_id) ){
                            outputWriter.println("PROBE received from " + recv_packet.s_id);
                        }
                        received_probes_set.add(recv_packet.s_id);
                        
                        if(previous_hop == -1 || Math.floorMod(recv_packet.s_id - my_port, end_port - start_port) > Math
                        .floorMod(previous_hop - my_port, end_port - start_port)){
                            
                            
                            if(!sent_ack_set.contains(recv_packet.s_id)){
                                outputWriter.println("SENDED ACK to " + recv_packet.s_id);
                            }
                            sent_ack_set.add(recv_packet.s_id); 
                            previous_hop = recv_packet.s_id;
                            outputWriter.println(get_time_to_print() + " previous hop is changed to client " + previous_hop);
                            receive_probe_ack = true;
                            recv_packet = null;
                            
                        }
                    }
                }
                

                time_out = 5000;
                long ack_nak_timer = System.currentTimeMillis();

                while(System.currentTimeMillis() - ack_nak_timer < time_out){
                    recv_packet = (Packet) receive_packet();

                    if (recv_packet == null){
                        continue;
                    }
                    if(recv_packet.m_id == 11){

                        if(!received_ack_set.contains(recv_packet.s_id)){
                            outputWriter.println("ACK received from " + recv_packet.s_id);
                            
                        }
                        received_response_set.add(recv_packet.s_id);
                        received_ack_set.add(recv_packet.s_id);
                        
                        if(next_hop == Integer.MAX_VALUE || Math.floorMod(recv_packet.s_id - my_port, end_port - start_port) > Math
                        .floorMod(next_hop - my_port, end_port - start_port)){
                            
                            outputWriter.println(get_time_to_print() + "next hop is changed to client " + recv_packet.s_id);
                            next_hop = recv_packet.s_id;
                            recv_packet = null;
                        }
                        
                    }
                    else if(recv_packet.m_id == 12){
                        
                        if(!received_nak_set.contains(recv_packet.s_id)){
                            outputWriter.println("NAK received from " + recv_packet.s_id );
                            outputWriter.close();
                            
                            
                        }
                        received_response_set.add(recv_packet.s_id);
                        received_nak_set.add(recv_packet.s_id);
                        recv_packet = null;
                    }
                    if(next_hop != Integer.MAX_VALUE && previous_hop != -1){
                        outputWriter.println("\n" + " myport: " + my_port + " next hop: " + next_hop + " previous hop " + previous_hop + " found");
                        outputWriter.close();
                        break;
                    }
                }
                System.out.println("\n" + my_port + " " + received_response_set);
                System.out.println("\n" + " myport: " + my_port + " next hop: " + next_hop + " previous hop " + previous_hop + " found");
                outputWriter.close();

            }
            catch (SocketTimeoutException e){
                e.printStackTrace();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }


    }

    public void receive(){
        recv_messages_till_timeout = new receive_thread();
        recv_messages_till_timeout.start();
    }

    @SuppressWarnings("deprecation")
    public static void ring_function(ring RING){
        RING.receive();
        RING.send();
        // RING.recv_messages_till_timeout.stop();
        // RING.send_messages.stop();
    }



    public static void send_packet(int port, Object message_packet) throws Exception { //26
        

        BOS_SEND = new ByteArrayOutputStream();
        OOS_SEND = new ObjectOutputStream(BOS_SEND);
        OOS_SEND.flush();
        OOS_SEND.writeObject(message_packet);
        OOS_SEND.flush();

        send_byte = new byte[BUFFER_SIZE];
        send_byte = BOS_SEND.toByteArray();
        packet = new DatagramPacket(send_byte, send_byte.length);
        packet.setAddress(InetAddress.getByName("localhost"));
        packet.setPort(port);
        socket.send(packet);

        OOS_SEND.close();


    }




    public static Object receive_packet() throws Exception{ // 27
        
        receive_byte = new byte[BUFFER_SIZE];
        packet = new DatagramPacket(receive_byte, receive_byte.length);
        socket.receive(packet);

        BIS_REC = new ByteArrayInputStream(receive_byte);
        OIS_REC = new ObjectInputStream(new BufferedInputStream(BIS_REC));
        received_packet_object = OIS_REC.readObject();

        OIS_REC.close();
        return received_packet_object;    
    }

    public static void setupSocket(int my_port){
        try{
            socket = new DatagramSocket(my_port);
            // socket.setSoTimeout(10);
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