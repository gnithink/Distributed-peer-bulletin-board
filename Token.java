public class Token extends Packet {
    int token_id;
    Token(int s_id, int token_id){
        super.s_id = s_id;
        super.m_id = 30;
        this.token_id = token_id;
    }
    
    
}