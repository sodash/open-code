/**
 * 
 */
package com.winterwell.web;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which will only handle files upto a maximum size.
 * <p>
 * NB: Apache have a similar class of the same name. We have our own because it's a very simple class, 
 * & I'd like to avoid a dependency.
 * @author daniel
 */
public class LimitedInputStream extends FilterInputStream {
	
	private final long maxSize;
	private int size;

	public LimitedInputStream(InputStream in, long maxSize) {
		super(in);
		this.maxSize= maxSize;
	}

	/** Called to check, whether the input streams
     * limit is reached.
     * @throws IOException The given limit is exceeded.
     */
    private void checkLimit() throws IOException {
        if (size > maxSize) {
            raiseError(maxSize);
        }
    }
    
    @Override
    public int read() throws IOException {
        int r = super.read();
        if (r != -1) {
            size++;
            checkLimit();
        }
        return r;

    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
    	int r = super.read(b, off, len);
        if (r > 0) {
            size += r;
            checkLimit();
        }
        return r;
    }
    
    /**
     * Called to indicate, that the input streams limit has
     * been exceeded.
     * @param pSizeMax The input streams limit, in bytes.
     * @param pCount The actual number of bytes.
     * @throws IOException The called method is expected
     *   to raise an IOException.
     */
    protected void raiseError(long _maxSize) {
    	throw new FileTooLargeException("File too large - exceeded "+_maxSize);
    }


}
