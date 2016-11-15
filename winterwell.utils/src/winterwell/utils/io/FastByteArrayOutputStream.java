package winterwell.utils.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Code by Philip Isenhour published on Java Techniques as a public tutorial.
 * 
 * ByteArrayOutputStream implementation that doesn't synchronize methods and
 * doesn't copy the data on toByteArray().
 */
public class FastByteArrayOutputStream extends OutputStream {
	/**
	 * Buffer and size
	 */
	protected byte[] buf = null;
	protected int size = 0;

	/**
	 * Constructs a stream with buffer capacity size 5K
	 */
	public FastByteArrayOutputStream() {
		this(5 * 1024);
	}

	/**
	 * Constructs a stream with the given initial size
	 */
	public FastByteArrayOutputStream(int initSize) {
		size = 0;
		buf = new byte[initSize];
	}

	private void checkSize() {
		if (size < 0)
			throw new IndexOutOfBoundsException("Buffer overan! Too much data!");
	}

	/**
	 * Returns the byte array containing the written data. Note that this array
	 * will almost always be larger than the amount of data actually written.
	 */
	public byte[] getByteArray() {
		return buf;
	}

	/**
	 * @return the bytes and just the bytes written by this output stream
	 */
	public byte[] getByteArrayCutToSize() {
		byte[] chopped = Arrays.copyOf(buf, size);
		return chopped;
	}

	/**
	 * Returns a ByteArrayInputStream for reading back the written data
	 */
	public InputStream getInputStream() {
		return new FastByteArrayInputStream(buf, size);
	}

	/**
	 * @return the number of bytes written
	 */
	public int getSize() {
		return size;
	}

	public void reset() {
		size = 0;
	}

	/**
	 * Ensures that we have a large enough buffer for the given size.
	 */
	private void verifyBufferSize(int sz) {
		if (sz > buf.length) {
			byte[] old = buf;
			buf = new byte[Math.max(sz, 2 * buf.length)];
			System.arraycopy(old, 0, buf, 0, old.length);
			old = null;
		}
	}

	@Override
	public final void write(byte b[]) {
		verifyBufferSize(size + b.length);
		System.arraycopy(b, 0, buf, size, b.length);
		size += b.length;
		checkSize();
	}

	@Override
	public final void write(byte b[], int off, int len) {
		verifyBufferSize(size + len);
		System.arraycopy(b, off, buf, size, len);
		size += len;
		checkSize();
	}

	@Override
	public final void write(int b) {
		verifyBufferSize(size + 1);
		buf[size++] = (byte) b;
		checkSize();
	}

}
