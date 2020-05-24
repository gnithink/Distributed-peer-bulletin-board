public class Elected extends Packet{
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int election_id;
    int leader_id;

    Elected(int s_id, int election_id, int leader_id){
        super.m_id = 21;
        super.s_id = s_id;
        this.election_id = election_id;
        this.leader_id = leader_id;
    }
}