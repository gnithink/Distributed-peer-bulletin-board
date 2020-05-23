public class Post extends Packet {

    int seq_no;
    String message;
    
    Post(int s_id, int seq_no,String message){
        super.s_id = s_id;
        super.m_id = 0;
        this.seq_no = seq_no;
        this.seq_no = seq_no;
        
    }
    
}