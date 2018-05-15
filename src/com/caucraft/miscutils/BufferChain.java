package test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BufferChain<T> {
	
	private ReentrantLock lock;
	private List<T[]> chain;
	private int array;
	private int bindex;
	private long startLen;
	private long endLen;
	
	BufferChain(T[]... chain) {
		this.lock = new ReentrantLock();
		this.chain = new ArrayList<>(chain.length);
		for (T[] ba : chain) {
			if (ba.length != 0) {
				this.chain.add(ba);
				endLen += ba.length;
			}
		}
	}
	
	public void addStart(T[] ba) {
		if (ba.length == 0) {
			return;
		}
		lock.lock();
		chain.add(0, ba);
		startLen += ba.length;
		array++;
		lock.unlock();
	}
	
	public void addEnd(T[] ba) {
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
	 * @param bytes The number of Ts to skip in the buffer.
	 * @return True if the given number of Ts were able to be skipped, false
	 * otherwise.
	 */
	public boolean skip(long Ts) {
		if (Ts > 0 && Ts > endLen || Ts < 0 && -Ts > startLen) {
			return false;
		}
		lock.lock();
		long newIndex = bindex + Ts;
		int newArray = array;
		T[] a;
		while (newIndex < 0 && newArray >= 0) {
			newArray--;
			a = chain.get(newArray);
			newIndex += a.length;
//			startLen -= a.length;
//			endLen += a.length;
		}
		while (newIndex > 0 && newIndex >= chain.get(newArray).length) {
			a = chain.get(newArray);
			newIndex -= a.length;
//			startLen += a.length;
//			endLen -= a.length;
			newArray++;
		}
		startLen += Ts;
		endLen -= Ts;
		array = newArray;
		bindex = (int)newIndex;
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
	 * @return True if the given number of Ts were able to be skipped, false
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
	
	public T getFirst() {
		lock.lock();
		try {
			T[] a = chain.get(0);
			T v = a[0];
			lock.unlock();
			return v;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}
	
	public T getLast() {
		lock.lock();
		try {
			T[] a = chain.get(chain.size() - 1);
			T v = a[a.length - 1];
			lock.unlock();
			return v;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}
	
	public T get() {
		lock.lock();
		try {
			T[] a = chain.get(array);
			T v = a[bindex];
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
	
	public T last() {
		lock.lock();
		try {
			int bi2 = bindex - 1;
			int a2 = array;
			if (bi2 < 0) {
				a2--;
				bi2 = chain.get(a2).length - 1;
			}
			T v = chain.get(a2)[bi2];
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
}
