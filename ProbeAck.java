public class ProbeAck extends Packet{
    int reply_id;

    ProbeAck(int s_id, int reply_id){
        super.m_id = 11;
        super.s_id = s_id;
        this.reply_id = reply_id;
    }
}