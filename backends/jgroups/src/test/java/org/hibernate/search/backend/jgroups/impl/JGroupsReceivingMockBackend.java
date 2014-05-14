/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * JGroupsReceivingMockBackend; a JGroups based BackendQueueProcessor useful to verify
 * receiver state from tests.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 * @since 4.3
 */
public class JGroupsReceivingMockBackend extends JGroupsBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private volatile CountDownLatch threadTrap;
	private volatile boolean failOnMessage = false;
	private volatile boolean receivedAnything = false;

	public JGroupsReceivingMockBackend() {
		super( new MasterNodeSelector() );
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		receivedSomething();
		countDownAndJoin();
	}

	private void receivedSomething() {
		receivedAnything = true;
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		//Unused
		receivedSomething();
		countDownAndJoin();
	}

	public void resetThreadTrap() {
		threadTrap = new CountDownLatch( 2 );
	}

	public boolean wasSomethingReceived() {
		return receivedAnything;
	}

	public void countDownAndJoin() {
		if ( failOnMessage ) {
			throw new NullPointerException( "Simulated Failure" );
		}
		log.trace( "[PREJOIN] Timestamp: " + System.nanoTime() );
		try {
			threadTrap.countDown();
			//Basically we want to wait forever until we are awoken; we
			//cap the definition of "forever" to 2 minutes to abort the test
			//but this should not be necessary.
			//The main test thread will release us ASAP so a large timeout should not
			//affect the actual test duration.
			threadTrap.await( 2, TimeUnit.MINUTES );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
		log.trace( "[POSTJOIN] Timestamp: " + System.nanoTime() );
	}

	public int releaseBlockedThreads() {
		int count = (int) threadTrap.getCount();
		for ( int i = 0; i < count; i++ ) {
			threadTrap.countDown();
		}
		return count;
	}

	public void induceFailure() {
		failOnMessage = true;
	}

}
