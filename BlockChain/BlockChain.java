import java.util.ArrayList;
import java.util.HashMap;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
	public class Node {
        private Node parent;
        private Block block;
        private UTXOPool uPool;
        private int height;

        public Node(Block b, Node parent, UTXOPool pool) {
            this.block = b;
            this.parent = parent;
            this.uPool = pool;
            if (this.parent == null)
            	this.height = 1;
            else
            	this.height = this.parent.height + 1;
        }
        
        public UTXOPool getUTXOPool() {
        	return new UTXOPool(this.uPool);
        }
    }
	
	public static final int CUT_OFF_AGE = 10;
	private HashMap<ByteArrayWrapper, Node> chain;
    private Node maxHeightNode;
    private TransactionPool TxPool;
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	this.TxPool = new TransactionPool();
    	this.chain = new HashMap<>();
    	UTXOPool uPool = new UTXOPool();
    	addCoinbase(genesisBlock, uPool);
    	Node genesisNode = new Node(genesisBlock, null, uPool);
    	this.chain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
    	this.maxHeightNode = genesisNode;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
    	return this.maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
    	return this.maxHeightNode.getUTXOPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return this.TxPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null)
        	return false;
        
    	Node parentNode = chain.get(new ByteArrayWrapper(block.getPrevBlockHash()));
    	if (parentNode == null)
    		return false;
    	
    	TxHandler handler = new TxHandler(parentNode.getUTXOPool());
    	ArrayList<Transaction> txs = block.getTransactions();
    	Transaction[] txsArray = new Transaction[txs.size()];
    	txsArray = txs.toArray(txsArray);
    	Transaction[] validTxs = handler.handleTxs(txsArray);
    	if (txsArray.length != validTxs.length)
        	return false;
    	
        if ((parentNode.height + 1) <= maxHeightNode.height - CUT_OFF_AGE)
        	return false;
       	
        UTXOPool uPool = handler.getUTXOPool();
    	addCoinbase(block, uPool);
    	Node newNode = new Node(block, parentNode, uPool);
    	this.chain.put(new ByteArrayWrapper(block.getHash()), newNode);
    	if (parentNode.height + 1 > maxHeightNode.height) 
    		this.maxHeightNode = newNode;
    	return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        this.TxPool.addTransaction(tx);
    }
    
    public void addCoinbase(Block block, UTXOPool uPool) {
    	Transaction coinbase = block.getCoinbase();
    	for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            uPool.addUTXO(utxo, out);
        }
    }
}