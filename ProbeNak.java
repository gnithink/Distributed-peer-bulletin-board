public class ProbeNak extends Packet{

    int reply_id;
    ProbeNak(int s_id, int reply_id){
        super.m_id = 12;
        super.s_id = s_id;
        this.reply_id = reply_id;
    }
}