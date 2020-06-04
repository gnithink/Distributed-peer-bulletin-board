
// for file reading and writing
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;

import java.io.PrintWriter;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.lang.IllegalArgumentException;

// NIO sockets
import java.nio.channels.DatagramChannel;
import java.nio.ByteBuffer;


// for data structures for storing ports, input_messages
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
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

// Election Mechanism
import java.util.Random;

@SuppressWarnings("serial")
public class ring extends Exception {

    //NIO sockets byte buffers
    static ByteBuffer send_byte_buffer; // 17
    static ByteBuffer receive_buffer; // 21
    static DatagramChannel datagramChannel;

    // list of class variables 
    static File output; // 1
    static File config; // 2
    static File input; // 3
    static PrintWriter outputWriter; // 4
    static Scanner configScan; // 5
    static Scanner inputScan; // 6

    static ArrayList<Integer> ports_array = new ArrayList<Integer>(); //10
    static int my_port; // 11

    // Timing and clocks
    static long join_time = -1; // 12
    static long leave_time = -1 ; // 13
    static long start_time = -1; // need for the function get_time_to_print
    static long stop_time = -1;
    static long elapsed_time = -1;
    static boolean running;


    // DataGram Channel and Select
    static DatagramSocket socket = null;
    static DatagramPacket packet = null;
    static InetAddress local_ip = null;
    static InetSocketAddress address = null;
    static InetSocketAddress to_address = null;


    // Bytearray Buffers and received packet object
    static int BUFFER_SIZE = 1024; // 16
 
    static byte[] send_byte = null; // 18
    static ByteArrayOutputStream BOS_SEND = null; // 19
    static ObjectOutputStream OOS_SEND = null; // 20

    static byte[] receive_byte = null; // 22
    static ByteArrayInputStream BIS_REC = null; // 23
    static ObjectInputStream OIS_REC = null; // 24
    static Object received_packet_object = null; //25

    // Node Discovery
    // unique id for each packet sent. Just increment the value after every packet sent.
    static int packet_id = 0;
    static int next_hop = -1;
    static int previous_hop = -1;
    static boolean probing_handshake_success = false;
    static long sleep = 1000;

    // start and end port also used to to create ports array
    static int start_port = -1; 
    static int end_port = -1;

    // List of sets to for error checking 
    static SortedSet<Integer> received_nak_set = new TreeSet<Integer>(); 
    static SortedSet<Integer> received_probes_set = new TreeSet<Integer>();
    static SortedSet<Integer> received_ack_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_probe_set = new TreeSet<Integer>();
    static SortedSet<Integer> received_response_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_ack_set = new TreeSet<Integer>();
    static SortedSet<Integer> sent_nak_set = new TreeSet<Integer>();

    // Map {time_in_milliseconds : message_string}. They are in sorted order because of treemap
    static SortedMap<Long, String> messages_map = new TreeMap<Long, String>();

    // Receive Thread
    // making it static throws an error. So keeping it private
    receive_thread recv_messages;

    // Sending messages Thread
    send_thread send_messages;

    //All statement to be logged are concatenated to one string LOG
    public static String LOG = "";

    // Election mechanism
    Random random_timeout = new Random();
    static boolean is_leader_elected = false;
    static int election_id; // this election id is used for the token id as well while generating the token
    static int token_id = -1;
    static long token_time_stamp = -1 ; // used to get messages that we supposed to be posted before each election
    static SortedSet<Integer> known_tokens = new TreeSet<Integer>(); 
    static long ring_broken_timeout = 1000; // waits for 1 second to call socket timeout exception
    static boolean election_started = false;

    // For fixing node dynamics\
    static int received_port = -1;
    static long message_start_time = -1;
    static long message_end_time = -1;
    static long turn_around_time = -1;

    public static void main(String[] args) throws Exception, IllegalArgumentException{
        
        start_clock();
        cmdLineArgsParser(args); // 8 parse the command line arguments
        configFileParser(configScan, config); // 9 parse the configuration file
        inputFileParser(inputScan, input); //14  parse the input file
        // setupSocket(my_port);
        Thread.sleep(join_time); // Sleeping till the join_time.
        try{   
            setupSocket(my_port); // setting up the socket only after the start time.
        }
        catch (IllegalArgumentException e){
            System.out.println("Error setting up the socket");
            e.printStackTrace();
            System.exit(1);
        }        
        
        ring RING = new ring(); 
        try{
            bulletin_board_cycle(RING); // starting the send and the receive threads
        }
        catch (Exception e){
            System.out.println("Error in initiating the bulleting board cycle");
            e.printStackTrace();
            System.exit(1);
        }
        
        outputWriter.print(LOG);
        outputWriter.close();
        socket.close();
        
        System.out.println("Everything complete");
    }

    class send_thread extends Thread{
        
        
        @Override
        public void run(){
            int to_send_port;
            Probe probe_packet;
            is_leader_elected = false;
            
            // sending probes through the entire cycle from start_range to end_range once
            for (int i = 0; i < ports_array.size(); i++){

                try{
                    // probing_handshake_success = false;
                    to_send_port = ports_array.get(i);
                    probe_packet = new Probe(my_port, packet_id);
                    packet_id++;
                    send_packet(to_send_port, probe_packet);
                    Thread.sleep(30);
                }
                catch (Exception e){
                    System.out.println("Error while sending probe packet");
                    e.printStackTrace();
                }
                if(probing_handshake_success){
                    break;
                }
            }
            
            // if the next and previous hop are set, probing hand_shake is successful
            // sending election message after a random timeout of 100 milli sec.
            if(probing_handshake_success){
                try{
                    probe_packet = new Probe(my_port, packet_id);
                    packet_id++;
                    send_packet(next_hop, probe_packet);
                    int random_time = random_timeout.nextInt(100);
                    // LOG += "Sleeping for random time " + random_time +" ms\n";
                    Thread.sleep(random_time);
                }
                catch (Exception e){
                    System.out.println("Error while sleeping before starting an election");
                    e.printStackTrace();
                    System.exit(1);
                }
                try{
                    if(!election_started){

                    election_id = random_timeout.nextInt(100000);
                    
                    Election election_packet =  new Election(my_port, election_id, my_port);
                    send_packet(next_hop, election_packet);
                    LOG += get_time_to_print() + " started election, send election message to client " + next_hop + "\n";
                    }
                    
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Error while sending the election packet");
                }
                
            }
        }
    }

    class receive_thread extends Thread{
        @Override
        public void run(){
            
            
            Packet recv_packet = null;
            long recv_timer = System.currentTimeMillis();
            while(System.currentTimeMillis() - recv_timer < leave_time){
                try{
                    recv_packet = (Packet) receive_packet();
                    
                    if(recv_packet.m_id == 10 ){ // PROBE
                        // if(!received_probes_set.contains(recv_packet.s_id) ){
                        //     LOG += "PROBE received from " + recv_packet.s_id + "\n";
                        //     LOG += received_probes_set + "\n";
                        // }
                        // received_probes_set.add(recv_packet.s_id);

                        if(previous_hop == -1){
                            set_previous_hop(recv_packet);
                        }
                        else if (previous_hop < recv_packet.s_id && previous_hop > my_port){
                            set_previous_hop(recv_packet);
                        }
                        else if(previous_hop > my_port && recv_packet.s_id < my_port){
                            set_previous_hop(recv_packet);
                        }
                        else if(recv_packet.s_id < my_port && previous_hop < recv_packet.s_id ){
                            set_previous_hop(recv_packet);
                        }
                        else if (recv_packet.s_id == previous_hop){
                            previous_hop = recv_packet.s_id;
                            // LOG += get_time_to_print() + " previous hop is changed to client " + previous_hop + "\n";
                            ProbeAck ack = new ProbeAck(my_port, packet_id);
                            packet_id++;
                            send_packet(recv_packet.s_id, ack);
                        }

                        else{
                            // if(!sent_nak_set.contains(recv_packet.s_id)){
                            //     LOG += "SENDED NAK to " + recv_packet.s_id + "\n";
                            // }
                            // sent_nak_set.add(recv_packet.s_id);

                            ProbeNak nak = new ProbeNak(my_port, packet_id);
                            packet_id++;
                            send_packet(recv_packet.s_id, nak);
                        }

                        // if(!sent_ack_set.contains(recv_packet.s_id)){
                        //     LOG += "SENDED ACK to " + recv_packet.s_id + "\n";
                        // }
                        // sent_ack_set.add(recv_packet.s_id); 

                    }
                        
                    else if(recv_packet.m_id == 11){

                        // if(!received_ack_set.contains(recv_packet.s_id) ){
                        //     LOG += "ACK received from " + recv_packet.s_id + "\n";
                        //     LOG += "NAK set" + received_ack_set + "\n";
                        // }
                        // received_ack_set.add(recv_packet.s_id);

                        if (next_hop == -1){
                            LOG += get_time_to_print() + " ring is broken\n";
                            set_next_hop(recv_packet);                            
                        }
                        else if(next_hop > recv_packet.s_id && next_hop > my_port){
                            set_next_hop(recv_packet);
                        }
                        else if(next_hop > recv_packet.s_id && next_hop < my_port){
                            set_next_hop(recv_packet);
                        }
                        else if(next_hop < my_port && recv_packet.s_id > my_port){
                            set_next_hop(recv_packet);
                        }  
                    }

                    else if(recv_packet.m_id == 12){
                        if(!received_ack_set.contains(recv_packet.s_id) ){
                            LOG += "NAK received from " + recv_packet.s_id + "\n";
                        }
                        received_ack_set.add(recv_packet.s_id);
                    }

                    if(!probing_handshake_success){
                        continue;
                    }
                    
                    if(recv_packet.m_id == 20 && !is_leader_elected){
                        Election recv_election = (Election) recv_packet;
                        Probe probe_packet;
                        probe_packet = new Probe(my_port, packet_id);
                        packet_id++;
                        send_packet(next_hop, probe_packet);
                        
                        if( my_port > recv_election.best_client_id){
                            election_started = true;
                            // LOG += "next hop is " + next_hop +  "  election starter " + recv_election.s_id + "\n"; 
                            LOG += get_time_to_print() +" relayed election message, replaced leader "+recv_election.best_client_id + " to " + my_port + "\n";
                            recv_election.best_client_id = my_port;
                            
                            Election temp_election = new Election(recv_election.s_id, recv_election.election_id, my_port);  
                            send_packet(next_hop, temp_election);
                        }
                        else if(my_port < recv_election.best_client_id){
                            election_started = true;
        
                            LOG += get_time_to_print() +" relayed election message, leader: client " + recv_election.best_client_id + "\n";
                            send_packet(next_hop, recv_election);
                        }
                        else{
                            
                            Elected elected = new Elected(recv_election.s_id, recv_election.election_id, my_port);
                            packet_id++;
                            // LOG += "ELECTED PACKET SENT \n";
                            is_leader_elected = true;
                            send_packet(next_hop, elected);
                        }
                        
                    }
                
                    else if(recv_packet.m_id == 21 && next_hop != -1 && previous_hop != -1){
                        Elected recv_elected = (Elected) recv_packet;
                        // Probe probe_packet;
                        // probe_packet = new Probe(my_port, packet_id);
                        // packet_id++;
                        // send_packet(next_hop, probe_packet);

                        if(my_port < recv_elected.leader_id){
                            is_leader_elected = true;
                            send_packet(next_hop, recv_elected);
                        }
                        else if (my_port > recv_elected.leader_id){
                            is_leader_elected = false;
                            send_packet(next_hop, recv_elected);                            
                        }
                        else{
                            LOG += get_time_to_print() + " leader selected\n";
                            token_id = recv_elected.election_id;
                            is_leader_elected = true;

                            LOG += get_time_to_print() + " new token generated " + token_id + "\n"; 
                            send_messages();
                        }
                        
                    }
                    if(!is_leader_elected){
                        continue;
                    }
                
                    else if(recv_packet.m_id == 30 && previous_hop == received_port){
                        Token recv_token = (Token) recv_packet;
                        token_time_stamp = System.currentTimeMillis() - start_time;
                        token_id = recv_token.token_id;
                        if(!known_tokens.contains(token_id)){
                            LOG += get_time_to_print() + " token " + token_id + " was received \n";
                        }
                        send_messages();

                    }
                    else if(recv_packet.m_id == 0 && previous_hop == received_port){
                        Post recv_post = (Post) recv_packet;
                        
                        if(recv_post.s_id == my_port){
                            if(!messages_map.containsKey(recv_post.seq_no)){
                                LOG += "Message key " + recv_post.seq_no +" is not present in the message map " + recv_post.message + "\n";
                            }
                            else{
                                LOG += get_time_to_print() + " post \"" + recv_post.message + "\" was delivered to all successfully\n";
                                messages_map.remove(recv_post.seq_no);
                                message_end_time = System.currentTimeMillis();
                                turn_around_time = message_end_time - message_start_time;

                                if (100 * turn_around_time > 1000){
                                    ring_broken_timeout = 100 * turn_around_time;
                                    // LOG += "100 * TAT is " + ring_broken_timeout + "\n";
                                }
                                else{
                                    ring_broken_timeout = 1000;
                                }

                                send_messages();
                            }
                        }
                        else{
                            LOG += get_time_to_print() + " post \"" + recv_post.message + "\" from client " + recv_post.s_id + " was relayed \n";
                            send_packet(next_hop, recv_post);

                        }

                    }    

                }
                catch (SocketTimeoutException e){

                    resetting_static_variables(); // resetting so that probing process can continue
                    try{
                    Thread.sleep(10);
                    }
                    catch (InterruptedException f){
                        f.printStackTrace();

                    }
                    send_messages = new send_thread();
                    send_messages.start();
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("There was a problem while processing server messages");
                    socket.close();
                    System.exit(1);
                }
            }   
        }
    }

    public static void set_next_hop(Packet recv_packet){
        next_hop = recv_packet.s_id;                    
        LOG += get_time_to_print() + " next hop is changed to client " + next_hop + "\n";
        probing_handshake_success = true;

    }
    public static void set_previous_hop(Packet recv_packet) throws Exception{
        previous_hop = recv_packet.s_id;
        LOG += get_time_to_print() + " previous hop is changed to client " + previous_hop + "\n";
        ProbeAck ack = new ProbeAck(my_port, packet_id);
        packet_id++;
        send_packet(recv_packet.s_id, ack);

    }

    

    public void send_messages() throws Exception{
        // getting the first message less than the token_time_stamp
        SortedMap<Long, String> messages_election_time = messages_map.headMap(token_time_stamp);

        // if there are no messages to send, send the token.
        // Else send the post message.
        if(messages_election_time.isEmpty()){
            Token token = new Token(my_port, token_id);
            send_packet(next_hop, token);
            if(!known_tokens.contains(token_id)){
                LOG += get_time_to_print() + " token " + token_id + " was sent to client " + next_hop + "\n";
                known_tokens.add(token_id);
            }
            token_id = -1;
            token_time_stamp = -1;
           
        }
        else{
            Long message_post_time = messages_election_time.firstKey();
            long seq_no = message_post_time;
            String message_to_post = messages_election_time.get(message_post_time);

            Post post = new Post(my_port, seq_no, message_to_post);
            send_packet(next_hop, post);

            LOG += get_time_to_print() + " post \"" + message_to_post + "\" was sent\n";
            message_start_time = System.currentTimeMillis();
        }

    }

    public void send(){
        send_messages = new send_thread();
        send_messages.start();
        
    }

    public void receive(){
        recv_messages = new receive_thread();
        recv_messages.start();
    }

    @SuppressWarnings("deprecation")
    private static void bulletin_board_cycle(ring RING) throws Exception{
        RING.receive();
        RING.send();

        
        try {
            Thread.sleep(leave_time - join_time);
        } catch (InterruptedException e) {

            System.err.println("Error while sleeping after and send and receive thread");
            e.printStackTrace();
            System.exit(1);
        }

        RING.recv_messages.stop();
        RING.send_messages.stop();
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
        received_port = packet.getPort();

        BIS_REC = new ByteArrayInputStream(receive_byte);
        OIS_REC = new ObjectInputStream(new BufferedInputStream(BIS_REC));
        received_packet_object = OIS_REC.readObject();

        OIS_REC.close();
        return received_packet_object;

    }

    public static void setupSocket(int my_port){
        try{
            socket = new DatagramSocket(my_port);
            socket.setSoTimeout((int) ring_broken_timeout); 
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
        long message_time;
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
            System.exit(1);       
        }

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


    public static void resetting_static_variables(){

        //NIO sockets byte buffers
        send_byte_buffer = null; // 17
        receive_buffer = null; // 21

        // Timing and clock
        send_byte = null; // 18
    
        receive_byte = null; // 22
        received_packet_object = null; //25
        // Not resetting previous and next hop to see if the issue gets fixed.
        next_hop = -1;
        previous_hop = -1;
        probing_handshake_success = false;
        sleep = 1000;

        // List of sets to for error checking 
        received_nak_set.clear(); 
        received_probes_set.clear();
        received_ack_set.clear();
        sent_probe_set.clear();
        received_response_set.clear();
        sent_ack_set.clear();
        sent_nak_set.clear();

        is_leader_elected = false;
        election_id = -1; // this election id is used for the token id as well while generating the token
        token_id = -1;
        token_time_stamp = -1 ; // used to get messages that we supposed to be posted before each election
        known_tokens.clear();

        // For fixing node dynamics
        received_port = -1;
        election_started = false;
        message_start_time = -1;
        message_end_time = -1;
        turn_around_time = -1;

    }
}