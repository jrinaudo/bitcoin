import java.util.ArrayList;

public class TxHandler {
	private UTXOPool uPool;
	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		this.uPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if:
	 * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
	 * (2) the signatures on each input of {@code tx} are valid, 
	 * (3) no UTXO is claimed multiple times by {@code tx},
	 * (4) all of {@code tx}s output values are non-negative, and
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	 *     values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		ArrayList<UTXO> unique = new ArrayList<UTXO>();
		double sumInputs = 0;
		double sumOutputs = 0;
		
		for (int i = 0; i < tx.numInputs(); i++) {
			UTXO u = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
			Transaction.Output o = this.uPool.getTxOutput(u);
			// (1)
			if (!this.uPool.contains(u))
				return false;
			// (2)
			if (!Crypto.verifySignature(o.address, tx.getRawDataToSign(i), tx.getInput(i).signature))
				return false;
			// (3)
			if (unique.contains(u))
				return false;
			
			unique.add(u);
			sumInputs += o.value;
		}
		
		for (int i = 0; i < tx.numOutputs(); i++) {
			// (4)
			if (tx.getOutput(i).value < 0)
				return false;
			
			sumOutputs += tx.getOutput(i).value;
		}
		
		// (5)
		return sumInputs >= sumOutputs;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions, checking each
	 * transaction for correctness, returning a mutually valid array of accepted transactions, and
	 * updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> accepted = new ArrayList<Transaction>();
		
		for (int i = 0; i < possibleTxs.length; i++) {
			if (possibleTxs[i] != null && isValidTx(possibleTxs[i])) {
				accepted.add(possibleTxs[i]);
				
				for (int j = 0; j < possibleTxs[i].numInputs(); j++) {
					UTXO u = new UTXO(possibleTxs[i].getInput(j).prevTxHash, possibleTxs[i].getInput(j).outputIndex);
					this.uPool.removeUTXO(u);
				}
				
				for (int j = 0; j < possibleTxs[i].numOutputs(); j++) {
					UTXO u = new UTXO(possibleTxs[i].getHash(), j);
					this.uPool.addUTXO(u, possibleTxs[i].getOutput(j));
				}
			}
		}
		
		Transaction[] acc = new Transaction[accepted.size()];
		return accepted.toArray(acc);
	}
}