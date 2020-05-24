public class Probe extends Packet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int reply_id;

    Probe(int s_id, int reply_id ){
        super.m_id = 10;
        super.s_id = s_id;
        this.reply_id = reply_id;
    }
}