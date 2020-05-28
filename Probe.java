public class Probe extends Packet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int packet_id;

    Probe(int s_id, int packet_id ){
        super.m_id = 10;
        super.s_id = s_id;
        this.packet_id = packet_id;
    }
}