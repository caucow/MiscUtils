package com.caucraft.miscutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class ByteBufferChain {
	
	private ReentrantLock lock;
	private List<byte[]> chain;
	private int array;
	private int bindex;
	private long startLen;
	private long endLen;
	
	ByteBufferChain(byte[]... chain) {
		this.lock = new ReentrantLock();
		this.chain = new ArrayList<>(chain.length);
		for (byte[] ba : chain) {
			Objects.requireNonNull(ba);
			if (ba.length != 0) {
				this.chain.add(ba);
				endLen += ba.length;
			}
		}
	}
	
	public int getLocks() {
		return lock.getHoldCount();
	}
	
	public void addStart(byte[] ba) {
		Objects.requireNonNull(ba);
		if (ba.length == 0) {
			return;
		}
		lock.lock();
		chain.add(0, ba);
		startLen += ba.length;
		array++;
		lock.unlock();
	}
	
	public void addEnd(byte[] ba) {
		Objects.requireNonNull(ba);
		if (ba.length == 0) {
			return;
		}
		lock.lock();
		chain.add(ba);
		endLen += ba.length;
		lock.unlock();
	}
	
	public boolean removeStart() {
		lock.lock();
		if (array == 0) {
			lock.unlock();
			return false;
		}
		startLen -= chain.remove(0).length;
		array--;
		lock.unlock();
		return true;
	}
	
	public boolean removeEnd() {
		lock.lock();
		if (array >= chain.size() || array == chain.size() - 1 && bindex > 0) {
			lock.unlock();
			return false;
		}
		endLen -= chain.remove(chain.size() - 1).length;
		lock.unlock();
		return true;
	}
	
	/**
	 * @param bytes The number of bytes to skip in the buffer.
	 * @return True if the given number of bytes were able to be skipped, false
	 * otherwise.
	 */
	public boolean skip(long bytes) {
		lock.lock();
		if (bytes > 0 && bytes > endLen || bytes < 0 && -bytes > startLen) {
			lock.unlock();
			return false;
		}
		long newByteIndex = bindex + bytes;
		int newArray = array;
		byte[] a;
		while (newByteIndex < 0 && newArray >= 0) {
			newArray--;
			a = chain.get(newArray);
			newByteIndex += a.length;
//			startLen -= a.length;
//			endLen += a.length;
		}
		while (newByteIndex > 0 && newByteIndex >= chain.get(newArray).length) {
			a = chain.get(newArray);
			newByteIndex -= a.length;
//			startLen += a.length;
//			endLen -= a.length;
			newArray++;
		}
		startLen += bytes;
		endLen -= bytes;
		array = newArray;
		bindex = (int)newByteIndex;
		lock.unlock();
		return true;
	}
	
	public void lock() {
		lock.lock();
	}
	
	public void unlock() {
		lock.unlock();
	}
	
	/**
	 * @param index The index in the buffer to skip to.
	 * @return True if the given number of bytes were able to be skipped, false
	 * otherwise.
	 */
	public boolean goTo(long index) {
		lock.lock();
		boolean ret = skip(index - startLen);
		lock.unlock();
		return ret;
	}
	
	public long getIndex() {
		return startLen;
	}
	
	public long getRemaining() {
		return endLen;
	}
	
	public int getArrayIndex() {
		return array;
	}
	
	public int getByteIndex() {
		return bindex;
	}

	public byte get() {
		lock.lock();
		try {
			byte[] a = chain.get(array);
			byte v = a[bindex];
			bindex++;
			if (bindex >= a.length) {
				bindex -= a.length;
				array++;
			}
			++startLen;
			--endLen;
			lock.unlock();
			return v;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public char getChar() {
		lock.lock();
		try {
			ensureLength(2);
			char x = (char)((get() & 255) << 8 | (get() & 255));
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public short getShort() {
		lock.lock();
		try {
			ensureLength(2);
			short x = (short)((get() & 255) << 8 | (get() & 255));
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public int getInt() {
		lock.lock();
		try {
			ensureLength(4);
			int x = (get() & 255) << 24 | (get() & 255) << 16 | (get() & 255) << 8 | (get() & 255);
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public long getLong() {
		lock.lock();
		try {
			ensureLength(8);
			long x = (get() & 255L) << 56 | (get() & 255L) << 48 | (get() & 255L) << 40 | (get() & 255L) << 32
					| (get() & 255L) << 24 | (get() & 255L) << 16 | (get() & 255L) << 8 | (get() & 255L);
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public float getFloat() {
		lock.lock();
		try {
			ensureLength(4);
			float x = Float.intBitsToFloat(getInt());
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public double getDouble() {
		lock.lock();
		try {
			ensureLength(8);
			double x = Double.longBitsToDouble(getLong());
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}
	
	private void ensureLength(int len) {
		if (len > endLen) {
			throw new IllegalStateException("Unable to read value from ByteBufferChain (required bytes: " + len + ", remaining " + endLen);
		}
	}
	
	public byte last() {
		lock.lock();
		try {
			int bi2 = bindex - 1;
			int a2 = array;
			if (bi2 < 0) {
				a2--;
				bi2 = chain.get(a2).length - 1;
			}
			byte v = chain.get(a2)[bi2];
			array = a2;
			bindex = bi2;
			--startLen;
			++endLen;
			lock.unlock();
			return v;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public char lastChar() {
		lock.lock();
		try {
			ensureBackLength(2);
			char x = (char)((last() & 255) | (last() & 255) << 8);
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public short lastShort() {
		lock.lock();
		try {
			ensureBackLength(2);
			short x = (short)((last() & 255) | (last() & 255) << 8);
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public int lastInt() {
		lock.lock();
		try {
			ensureBackLength(4);
			int x = (last() & 255) | (last() & 255) << 8 | (last() & 255) << 16 | (last() & 255) << 24;
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public long lastLong() {
		lock.lock();
		try {
			ensureBackLength(8);
			long x = (last() & 255L) | (last() & 255L) << 8 | (last() & 255L) << 16 | (last() & 255L) << 24
					| (last() & 255L) << 32 | (last() & 255L) << 40 | (last() & 255L) << 48 | (last() & 255L) << 56;
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public float lastFloat() {
		lock.lock();
		try {
			ensureBackLength(4);
			float x = Float.intBitsToFloat(lastInt());
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public double lastDouble() {
		lock.lock();
		try {
			ensureBackLength(8);
			double x = Double.longBitsToDouble(lastLong());
			lock.unlock();
			return x;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}
	
	private void ensureBackLength(int len) {
		if (len > startLen) {
			throw new IllegalStateException("Unable to read value from ByteBufferChain (required bytes: " + len + ", remaining " + endLen);
		}
	}

}
