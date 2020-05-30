public class Post extends Packet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    long seq_no;
    String message;
    
    Post(int s_id, long seq_no,String message){
        super.s_id = s_id;
        super.m_id = 0;
        this.seq_no = seq_no;
        this.message = message;
        
    }
    
}