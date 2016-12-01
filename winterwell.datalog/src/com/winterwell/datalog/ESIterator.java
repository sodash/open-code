//package com.winterwell.datalog;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.NoSuchElementException;
//
//import com.winterwell.es.client.SearchRequestBuilder;
//import com.winterwell.es.client.SearchResponse;
//import com.winterwell.utils.Utils;
//
///**
// * Takes in an ES search, iterates over it on the basis of setFrom() and setSize()  
// * @author alexn
// *
// */
//public class ESIterator implements Iterator<Map> {
//
//	private static int DEFAULT_BLOCK_SIZE = 10000;
//	private Integer blockSize;
//	private SearchRequestBuilder search;
//	private List<Map> currentBlock;
//	private Integer blockCounter;
//	private Integer pageCounter;
//	
//	public ESIterator(SearchRequestBuilder search){
//		this(search, DEFAULT_BLOCK_SIZE);
//	}
//
//	public ESIterator(SearchRequestBuilder search, int blockSize){
//		this.search = search;
//		this.blockSize = blockSize;
//		this.blockCounter = 0;
//		this.pageCounter = 0;
//	}
//
//	/**
//	 * Checks whether there is a next element  
//	 */
//	@Override
//	public boolean hasNext() {
//		// Do we have no data block? Or are we at the end of one? If either, load the next block
//		if (isTimeToLoadNewBlock()) loadNewBlockFromES();
//		// Is the block empty? Or spent
//		if (blockIsEmpty()) return false;
//		if (partialBlockIsSpent()) return false;
//		return true;
//	}
//
//	/**
//	 * This is the case when a block has reached its total, and that total is less than the block size
//	 * @return
//	 */
//	private boolean partialBlockIsSpent() {
//		if ((blockCounter.equals(currentBlock.size())) 
//				&& (currentBlock.size() < DEFAULT_BLOCK_SIZE)) return true;
//		return false;
//	}
//
//	/**
//	 * This is the case when a block has reached its total, and that total is less than the block size
//	 * @return true - if this the case
//	 */
//	private boolean fullBlockIsSpent() {
//		if ((blockCounter.equals(currentBlock.size())) 
//				&& (currentBlock.size() == DEFAULT_BLOCK_SIZE)) return true;
//		return false;
//	}
//	
//	/**
//	 * This happens when a new block is returned, but has nothing in it.
//	 * @return true - if this the case
//	 */
//	private boolean blockIsEmpty() {
//		if (new Integer(0).equals(currentBlock.size())) return true;
//		return false;
//	}
//
//	/**
//	 * Go to ES - load a block.
//	 */
//	private void loadNewBlockFromES() {
//		search.setFrom(pageCounter*blockSize).setSize(blockSize);
//		SearchResponse response = search.get();
//		pageCounter++;
//		blockCounter = 0;
//		if ( ! response.isSuccess()) {
//			throw Utils.runtime(response.getError());
//		}
//		currentBlock = response.getHits();
//	}
//
//	private boolean isTimeToLoadNewBlock() {
//		if (currentBlock == null) return true;
//		if (fullBlockIsSpent()) return true;
//		return false;
//	}
//
//	@Override
//	public Map next() {
//		// hasNext will do a block load if needed.
//		if (!hasNext()) throw new NoSuchElementException();
//		Map out = currentBlock.get(blockCounter);
//		blockCounter++;
//		return out;
//	}
//
//	@Override
//	public void remove() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("ESIterators don't like removes");
//	}
//	
//	@Deprecated // For testing
//	public static void setBlockSize(int bs){
//		DEFAULT_BLOCK_SIZE = bs;
//	}
//}
