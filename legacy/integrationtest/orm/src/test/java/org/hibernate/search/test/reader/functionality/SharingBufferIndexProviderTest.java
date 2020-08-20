/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.reader.functionality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.test.reader.functionality.ExtendedSharingBufferReaderProvider.MockIndexReader;

import org.junit.Test;

/**
 * Emulates a stress condition on the SharingBufferReaderProvider, to make sure index lifecycle is properly managed.
 *
 * @author Sanne Grinovero
 */
public class SharingBufferIndexProviderTest {

	private final ExtendedSharingBufferReaderProvider readerProvider = new ExtendedSharingBufferReaderProvider();
	private final CountDownLatch startSignal = new CountDownLatch( 1 );
	private final Runnable searchTask = new SearchTask();
	private final Runnable changeTask = new ChangeTask();
	private final Runnable directorySwitchTask = new DirectorySwitchTask();
	private final AtomicInteger countDoneSearches = new AtomicInteger();
	private final AtomicInteger countDoneIndexmods = new AtomicInteger();
	private static final int SEARCHES_NUM = 10000;

	@Test
	public void testStressingMock() throws InterruptedException {
		readerProvider.initialize();
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( 50 ); //much chaos
		for ( int i = 0; i < SEARCHES_NUM; i++ ) {
			executor.execute( makeTask( i ) );
		}
		executor.shutdown();
		startSignal.countDown();
		executor.awaitTermination( 500, TimeUnit.SECONDS );
		assertTrue( "memory leak: holding a reference to some unused IndexReader", readerProvider.areAllOldReferencesGone() );
		for ( MockIndexReader reader : readerProvider.getCreatedIndexReaders() ) {
			if ( readerProvider.isReaderCurrent( reader ) ) {
				assertTrue( "the most current reader should be open", ! reader.isClosed() );
			}
			else {
				assertTrue( "an IndexReader is still open", reader.isClosed() );
			}
		}
		assertEquals( SEARCHES_NUM, countDoneSearches.get() );
		assertEquals( SEARCHES_NUM / 10, countDoneIndexmods.get() );
	}

	private Runnable makeTask(int i) {
		if ( i % 100 == 0 ) {
			return directorySwitchTask;
		}
		if ( i % 10 == 0 ) {
			return changeTask;
		}
		else {
			return searchTask;
		}
	}

	private class SearchTask implements Runnable {
		@Override
		public void run() {
			try {
				startSignal.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				//manage termination:
				return;
			}
			IndexReader fakeOpenReader = readerProvider.openIndexReader();
			Thread.yield();
			readerProvider.closeIndexReader( fakeOpenReader );
			countDoneSearches.incrementAndGet();
		}
	}

	private class ChangeTask extends SearchTask {
		@Override
		public void run() {
			super.run();
			Thread.yield();
			readerProvider.currentDPWasWritten();
			countDoneIndexmods.incrementAndGet();
		}
	}

	private class DirectorySwitchTask extends ChangeTask {
		@Override
		public void run() {
			super.run();
			Thread.yield();
			readerProvider.swithDirectory();
		}
	}

}
