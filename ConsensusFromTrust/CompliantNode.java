import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	private double p_graph;
	private double p_malicious;
	private double p_txDistribution;
	private double numRounds;
	private boolean[] followees;
	private int[] active;
	private Set<Transaction> pendingTx;
	private int round = 1;
	
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
    	this.followees = followees;
    	this.active = new int[followees.length];
    	for (int i = 0; i < active.length; i++) {
    		active[i] = 0;
		}
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    	this.pendingTx = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
    	round++;
    	Set<Transaction> send = new HashSet<Transaction>(this.pendingTx);
    	this.pendingTx.clear();
    	return send;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
    	for (Candidate c : candidates) {
    		if (round <= numRounds * 0.5) {
   	    		if (this.followees[c.sender]) {
   	    			this.pendingTx.add(c.tx);
   	    			this.active[c.sender]++;
   	    		}
    		}
    		else {
    			if (this.followees[c.sender] && active[c.sender] >= numRounds * 0.5)
        			this.pendingTx.add(c.tx);
    		}
    	}
    }
}