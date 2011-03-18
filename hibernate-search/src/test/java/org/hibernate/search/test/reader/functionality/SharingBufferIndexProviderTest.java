/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.reader.functionality;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.IndexReader;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.reader.functionality.ExtendedSharingBufferReaderProvider.MockIndexReader;
import org.hibernate.search.test.reader.functionality.ExtendedSharingBufferReaderProvider.TestManipulatorPerDP;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sanne Grinovero
 */
public class SharingBufferIndexProviderTest {
	
	private final ExtendedSharingBufferReaderProvider readerProvider = new ExtendedSharingBufferReaderProvider();
	private final CountDownLatch startSignal = new CountDownLatch(1);
	private final Runnable searchTask = new SearchTask();
	private final Runnable changeTask = new ChangeTask();
	private final AtomicInteger countDoneSearches = new AtomicInteger();
	private final AtomicInteger countDoneIndexmods = new AtomicInteger();
	private static final int SEARCHES_NUM = 50000;
	private static final Random random = new Random();
	
	@Test
	public void testStressingMock() throws InterruptedException {
		readerProvider.initialize(null, null);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool( 200 );//much chaos
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
	
	private DirectoryProvider[] getRandomAvailableDPs() {
		int arraySize = random.nextInt( readerProvider.manipulators.size() - 1 ) + 1;
		DirectoryProvider[] array = new DirectoryProvider[arraySize];
		List<DirectoryProvider> availableDPs = new ArrayList<DirectoryProvider>( readerProvider.directoryProviders );
		for (int i=0; i<arraySize; i++){
			int chosenDpIndex = random.nextInt( availableDPs.size() );
			array[i] = availableDPs.get( chosenDpIndex );
			availableDPs.remove( array[i] );
		}
		return array;
	}
	
	private class SearchTask implements Runnable {
		public void run() {
			try {
				startSignal.await();
			} catch (InterruptedException e) {
				//manage termination:
				return;
			}
			IndexReader fakeOpenReader = readerProvider.openReader( getRandomAvailableDPs() );
			Thread.yield();
			readerProvider.closeReader( fakeOpenReader );
			countDoneSearches.incrementAndGet();
		}
	}
	
	private class ChangeTask extends SearchTask {
		public void run() {
			super.run();
			Thread.yield();
			DirectoryProvider[] randomEvailableDPs = getRandomAvailableDPs();
			for ( DirectoryProvider dp : randomEvailableDPs ) {
				TestManipulatorPerDP testManipulatorPerDP = readerProvider.manipulators.get( dp.getDirectory() );
				testManipulatorPerDP.setIndexChanged();
			}
			countDoneIndexmods.incrementAndGet();
		}
	}

}
