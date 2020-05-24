public class Election extends Packet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int election_id;
    int best_client_id;
    Election(int s_id, int election_id, int best_client_id){
        super.s_id = s_id;
        super.m_id = 20;
        this.election_id = election_id;
        this.best_client_id = best_client_id;
    }
}
