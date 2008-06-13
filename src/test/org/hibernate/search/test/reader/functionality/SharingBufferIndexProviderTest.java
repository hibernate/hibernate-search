package org.hibernate.search.test.reader.functionality;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.test.reader.functionality.TestableSharingBufferReaderProvider.MockIndexReader;

import junit.framework.TestCase;

/**
 * @author Sanne Grinovero
 */
public class SharingBufferIndexProviderTest extends TestCase {
	
	private final TestableSharingBufferReaderProvider readerProvider = new TestableSharingBufferReaderProvider();
	private final CountDownLatch startSignal = new CountDownLatch(1);
	private final Runnable searchTask = new SearchTask();
	private final Runnable changeTask = new ChangeTask();
	private final AtomicInteger countDoneSearches = new AtomicInteger();
	private final AtomicInteger countDoneIndexmods = new AtomicInteger();
	private static final int SEARCHES_NUM = 5000;
	
	public void testStressingMock() throws InterruptedException {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( 200 );//much chaos
		for ( int i = 0; i < SEARCHES_NUM; i++ ) {
			executor.execute( makeTask( i ) );
		}
		executor.shutdown();
		startSignal.countDown();
		executor.awaitTermination( 15, TimeUnit.SECONDS );
		assertTrue( "memory leak: holding a reference to some unused IndexReader", readerProvider.isMapEmpty() );
		MockIndexReader openReader = readerProvider.fakeOpenReader();
		for ( MockIndexReader reader : readerProvider.getCreatedIndexReaders() ) {
			if ( reader != openReader ) {
				assertTrue( "an IndexReader is still open", reader.isClosed() );
			}
		}
		assertTrue( "the most current reader should be open", ! openReader.isClosed() );
		assertEquals( SEARCHES_NUM, countDoneSearches.get() );
		assertEquals( SEARCHES_NUM/10, countDoneIndexmods.get() );
	}

	private Runnable makeTask(int i) {
		if ( i % 10 == 0) {
			return changeTask;
		}
		else {
			return searchTask;
		}
	}
	
	private class SearchTask implements Runnable {
		public void run() {
			try {
				startSignal.await();
			} catch (InterruptedException e) {
				//manage termination:
				return;
			}
			MockIndexReader fakeOpenReader = readerProvider.fakeOpenReader();
			Thread.yield();
			readerProvider.closeReader( fakeOpenReader );
			countDoneSearches.incrementAndGet();
		}
	}
	
	private class ChangeTask extends SearchTask {
		public void run() {
			super.run();
			Thread.yield();
			readerProvider.setToDirtyState();
			countDoneIndexmods.incrementAndGet();
		}
	}

}
