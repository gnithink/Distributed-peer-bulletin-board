public class ProbeAck extends Packet{
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int packet_id;

    ProbeAck(int s_id, int packet_id){
        super.m_id = 11;
        super.s_id = s_id;
        this.packet_id = packet_id;
    }
}