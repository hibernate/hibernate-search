/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.backends.jgroups;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.MasterNodeSelector;
import org.hibernate.search.backend.spi.BackendQueueProcessor;


/**
 * JGroupsReceivingMockBackend; a JGroups based BackendQueueProcessor useful to verify
 * receiver state from tests.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 * @since 4.3
 */
public class JGroupsReceivingMockBackend extends JGroupsBackendQueueProcessor implements BackendQueueProcessor {

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
		System.out.println( "[PREJOIN] Timestamp: " + System.nanoTime() );
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
		System.out.println( "[POSTJOIN] Timestamp: " + System.nanoTime() );
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
