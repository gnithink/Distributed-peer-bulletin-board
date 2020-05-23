import java.io.*;

import java.nio.charset.Charset;
import java.nio.*;

public class writer {
    public static void writeFile3() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter("out1.txt"));
     
        for (int i = 0; i < 10; i++) {
            pw.write("something");
        }
     
        pw.close();
    }
    public static void byte_tostring(){
        String babel = "Hello world";
        System.out.println(babel);
        //Convert string to ByteBuffer:
        ByteBuffer babb = ByteBuffer.allocate(100);
        System.out.println(babb);
        babb = Charset.forName("UTF-8").encode(babel);
        System.out.println(babb);
        try{
            
            //Convert ByteBuffer to String
            System.out.println(new String(babb.array(), "UTF-8"));
        }
        catch(Exception e){
            e.printStackTrace();
        }   
    }
    public static void main(String[] args) throws IOException{
        // writeFile3();
        byte_tostring();
        
    }
    
}