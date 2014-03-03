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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.JGroupsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jgroups.JGroupsChannelProvider;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.SearchFactoryHolder;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.test.util.TestForIssue;
import org.jgroups.TimeoutException;
import org.junit.Rule;
import org.junit.Test;


/**
 * Verifies sync / async guarantees of the JGroups backend.
 * The JGroups stack used needs to have the RSVP protocol with ack_on_delivery enabled
 * (the default configuration we use does).
 *
 * Run the test using JVM parameters:
 * -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=127.0.0.1
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2013 Red Hat Inc.
 * @since 4.3
 */
@TestForIssue(jiraKey = "HSEARCH-1296")
public class SyncJGroupsBackendTest {

	private static final String JGROUPS_CONFIGURATION = "jgroups-testing-udp.xml";

	@Rule
	public SearchFactoryHolder slaveNode = new SearchFactoryHolder( Dvd.class, Book.class, Drink.class, Star.class )
		.withProperty( "hibernate.search.default.worker.backend", "jgroupsSlave" )
		.withProperty( "hibernate.search.dvds.worker.execution", "sync" )
		.withProperty( "hibernate.search.dvds.jgroups.delegate_backend", "blackhole" )
		.withProperty( "hibernate.search.dvds.jgroups.messages_timeout", "200" )
		.withProperty( "hibernate.search.books.worker.execution", "async" )
		.withProperty( "hibernate.search.drinks.jgroups." + JGroupsBackendQueueProcessor.BLOCK_WAITING_ACK, "true" )
		.withProperty( "hibernate.search.stars.jgroups." + JGroupsBackendQueueProcessor.BLOCK_WAITING_ACK, "false" )
		.withProperty( JGroupsChannelProvider.CONFIGURATION_FILE, JGROUPS_CONFIGURATION )
		;

	@Rule
	public SearchFactoryHolder masterNode = new SearchFactoryHolder( Dvd.class, Book.class, Drink.class, Star.class )
		.withProperty( "hibernate.search.default.worker.backend", JGroupsReceivingMockBackend.class.getName() )
		.withProperty( JGroupsChannelProvider.CONFIGURATION_FILE, JGROUPS_CONFIGURATION )
		;

	@Test
	public void testSynchAsConfigured() {
		JGroupsBackendQueueProcessor dvdsBackend = extractJGroupsBackend( "dvds" );
		Assert.assertTrue( "dvds index was configured with a syncronous JGroups backend", dvdsBackend.blocksForACK() );
		JGroupsBackendQueueProcessor booksBackend = extractJGroupsBackend( "books" );
		Assert.assertFalse( "books index was configured with an asyncronous JGroups backend", booksBackend.blocksForACK() );
		JGroupsBackendQueueProcessor drinksBackend = extractJGroupsBackend( "drinks" );
		Assert.assertTrue( "drinks index was configured with a syncronous JGroups backend", drinksBackend.blocksForACK() );
		JGroupsBackendQueueProcessor starsBackend = extractJGroupsBackend( "stars" );
		Assert.assertFalse( "stars index was configured with an asyncronous JGroups backend", starsBackend.blocksForACK() );

		JGroupsReceivingMockBackend dvdBackendMock = extractMockBackend( "dvds" );

		dvdBackendMock.resetThreadTrap();
		boolean timeoutTriggered = false;
		try {
			//DVDs are sync operations so they will timeout:
			final long presendTimestamp = System.nanoTime();
			System.out.println( "[PRESEND] Timestamp: " + presendTimestamp );
			storeDvd( 1, "Hibernate Search in Action" );
			final long postsendTimestamp = System.nanoTime();
			final long differenceInMilliseconds = TimeUnit.MILLISECONDS.convert( (postsendTimestamp - presendTimestamp), TimeUnit.NANOSECONDS );
			System.out.println( "[POSTSEND] Timestamp: " + postsendTimestamp + " Diff: " + differenceInMilliseconds + " ms." );
		}
		catch (SearchException se) {
			//Expected: we're inducing the RPC into timeout by blocking receiver processing
			Throwable cause = se.getCause();
			Assert.assertTrue( "Cause was not a TimeoutException but a " + cause, cause instanceof TimeoutException );
			timeoutTriggered = true;
		}
		finally {
			//release the receiver
			dvdBackendMock.releaseBlockedThreads();
		}
		Assert.assertTrue( "The backend didn't receive any message: something wrong with the test setup of network configuration", dvdBackendMock.wasSomethingReceived() );
		Assert.assertTrue( timeoutTriggered );

		JGroupsReceivingMockBackend booksBackendMock = extractMockBackend( "books" );
		booksBackendMock.resetThreadTrap();
		//Books are async so they should not timeout
		storeBook( 1, "Hibernate Search in Action" );

		//Block our own thread awaiting for the receiver.
		//If we raced past it we would release the receiver, not bad either
		//as it would also proof we are async.
		booksBackendMock.countDownAndJoin();

		dvdBackendMock.induceFailure();
		boolean npeTriggered = false;
		try {
			storeDvd( 2, "Byteman in Action" ); //not actually needing Byteman here
		}
		catch (SearchException se) {
			//Expected: we're inducing the RPC into NPE
			Throwable cause = se.getCause().getCause();
			Assert.assertTrue( "Cause was not a NullPointerException but a " + cause, cause instanceof NullPointerException );
			Assert.assertEquals( "Simulated Failure", cause.getMessage() );
			npeTriggered = true;
		}
		Assert.assertTrue( npeTriggered );
	}

	@Test
	public void alternativeBackendConfiguration() {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( slaveNode, "dvds" );
		JGroupsBackendQueueProcessor jgroupsProcessor = (JGroupsBackendQueueProcessor) backendQueueProcessor;
		BackendQueueProcessor delegatedBackend = jgroupsProcessor.getDelegatedBackend();
		Assert.assertTrue( "dvds backend was configured with a deleage to blackhole but it's not using it", delegatedBackend instanceof BlackHoleBackendQueueProcessor );
	}

	@Test
	public void alternativeJGroupsTimeoutConfiguration() {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( slaveNode, "dvds" );
		JGroupsBackendQueueProcessor jgroupsProcessor = (JGroupsBackendQueueProcessor) backendQueueProcessor;
		long messageTimeout = jgroupsProcessor.getMessageTimeout();
		Assert.assertEquals( "message timeout configuration property not applied", 200, messageTimeout );
	}

	private JGroupsReceivingMockBackend extractMockBackend(String indexName) {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( masterNode, indexName );
		Assert.assertTrue( "Backend not using the configured Mock!", backendQueueProcessor instanceof JGroupsReceivingMockBackend );
		return (JGroupsReceivingMockBackend) backendQueueProcessor;
	}

	private JGroupsBackendQueueProcessor extractJGroupsBackend(String indexName) {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( slaveNode, indexName );
		Assert.assertTrue( "Backend not using JGroups!", backendQueueProcessor instanceof JGroupsBackendQueueProcessor );
		return (JGroupsBackendQueueProcessor) backendQueueProcessor;
	}

	private static BackendQueueProcessor extractBackendQueue(SearchFactoryHolder node, String indexName) {
		IndexManager indexManager = node.getSearchFactory().getIndexManagerHolder().getIndexManager( indexName );
		Assert.assertNotNull( indexManager );
		DirectoryBasedIndexManager dbi = (DirectoryBasedIndexManager) indexManager;
		return dbi.getBackendQueueProcessor();
	}

	private void storeBook(int id, String string) {
		Book book = new Book();
		book.id = id;
		book.title = string;
		storeObject( book, id );
	}

	private void storeDvd(int id, String dvdTitle) {
		Dvd dvd1 = new Dvd();
		dvd1.id = id;
		dvd1.title = dvdTitle;
		storeObject( dvd1, id );
	}

	private void storeObject(Object entity, Serializable id) {
		Work work = new Work( entity, id, WorkType.UPDATE, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		slaveNode.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
	}

	@Indexed(index = "dvds")
	public static final class Dvd {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "books")
	public static final class Book {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "drinks")
	public static final class Drink {
		@DocumentId long id;
		@Field String title;
	}

	@Indexed(index = "stars")
	public static final class Star {
		@DocumentId long id;
		@Field String title;
	}

}
