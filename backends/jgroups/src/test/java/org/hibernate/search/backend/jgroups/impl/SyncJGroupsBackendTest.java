/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessor;
import org.hibernate.search.backend.jgroups.impl.JGroupsReceivingMockBackend.JGroupsReceivingMockBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jgroups.TimeoutException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verifies sync / async guarantees of the JGroups backend.
 * The JGroups stack used needs to have the RSVP protocol with ack_on_delivery enabled
 * (the default configuration we use does).
 *
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 * @since 4.3
 */
@TestForIssue(jiraKey = "HSEARCH-1296")
public class SyncJGroupsBackendTest {

	private static final Log log = LoggerFactory.make();
	private static final String JGROUPS_CONFIGURATION = "testing-flush-loopback.xml";
	private static final long JGROUPS_MESSAGES_TIMEOUT = 1000;

	@Rule
	public SearchFactoryHolder slaveNode = new SearchFactoryHolder( Dvd.class, Book.class, Drink.class, Star.class )
		.withProperty( "hibernate.search.default.worker.backend", "jgroupsSlave" )
		.withProperty( "hibernate.search.dvds.worker.execution", "sync" )
		.withProperty( "hibernate.search.dvds.jgroups.messages_timeout", String.valueOf( JGROUPS_MESSAGES_TIMEOUT ) )
		.withProperty( "hibernate.search.books.worker.execution", "async" )
		.withProperty( "hibernate.search.drinks.jgroups." + JGroupsBackend.BLOCK_WAITING_ACK, "true" )
		.withProperty( "hibernate.search.stars.jgroups." + JGroupsBackend.BLOCK_WAITING_ACK, "false" )
		.withProperty( DispatchMessageSender.CONFIGURATION_FILE, JGROUPS_CONFIGURATION );

	@Rule
	public SearchFactoryHolder masterNode = new SearchFactoryHolder( Dvd.class, Book.class, Drink.class, Star.class )
		.withProperty( "hibernate.search.default.worker.backend", JGroupsReceivingMockBackend.class.getName() )
		.withProperty( "hibernate.search.dvds.jgroups.delegate_backend", "blackhole" )
		.withProperty( DispatchMessageSender.CONFIGURATION_FILE, JGROUPS_CONFIGURATION );

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

		JGroupsReceivingMockBackendQueueProcessor dvdBackendMock = extractMockBackend( "dvds" );

		dvdBackendMock.resetThreadTrap();
		boolean timeoutTriggered = false;
		try {
			//DVDs are sync operations so they will timeout:
			final long presendTimestamp = System.nanoTime();
			log.trace( "[PRESEND] Timestamp: " + presendTimestamp );
			storeDvd( 1, "Hibernate Search in Action" );
			final long postsendTimestamp = System.nanoTime();
			final long differenceInMilliseconds = TimeUnit.MILLISECONDS.convert( (postsendTimestamp - presendTimestamp), TimeUnit.NANOSECONDS );
			log.trace( "[POSTSEND] Timestamp: " + postsendTimestamp + " Diff: " + differenceInMilliseconds + " ms." );
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

		JGroupsReceivingMockBackendQueueProcessor booksBackendMock = extractMockBackend( "books" );
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
			Throwable cause = se.getCause().getCause().getCause();
			Assert.assertTrue( "Cause was not a NullPointerException but a " + cause, cause instanceof NullPointerException );
			Assert.assertEquals( "Simulated Failure", cause.getMessage() );
			npeTriggered = true;
		}
		Assert.assertTrue( npeTriggered );
	}

	@Test
	public void alternativeBackendConfiguration() {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( masterNode, "dvds" );
		JGroupsReceivingMockBackendQueueProcessor jgroupsProcessor = (JGroupsReceivingMockBackendQueueProcessor) backendQueueProcessor;
		BackendQueueProcessor delegatedBackend = jgroupsProcessor.getDelegate().getExistingDelegate();
		Assert.assertTrue( "dvds backend was configured with a delegate to blackhole but it's not using it", delegatedBackend instanceof BlackHoleBackendQueueProcessor );
	}

	@Test
	public void alternativeJGroupsTimeoutConfiguration() {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( slaveNode, "dvds" );
		JGroupsBackendQueueProcessor jgroupsProcessor = (JGroupsBackendQueueProcessor) backendQueueProcessor;
		long messageTimeout = jgroupsProcessor.getMessageTimeout();
		Assert.assertEquals( "message timeout configuration property not applied", JGROUPS_MESSAGES_TIMEOUT, messageTimeout );
	}

	private JGroupsReceivingMockBackendQueueProcessor extractMockBackend(String indexName) {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( masterNode, indexName );
		Assert.assertTrue( "Backend not using the configured Mock!", backendQueueProcessor instanceof JGroupsReceivingMockBackendQueueProcessor );
		return (JGroupsReceivingMockBackendQueueProcessor) backendQueueProcessor;
	}

	private JGroupsBackendQueueProcessor extractJGroupsBackend(String indexName) {
		BackendQueueProcessor backendQueueProcessor = extractBackendQueue( slaveNode, indexName );
		Assert.assertTrue( "Backend not using JGroups!", backendQueueProcessor instanceof JGroupsBackendQueueProcessor );
		return (JGroupsBackendQueueProcessor) backendQueueProcessor;
	}

	private static BackendQueueProcessor extractBackendQueue(SearchFactoryHolder node, String indexName) {
		return node.getSearchFactory().getIndexManagerHolder().getBackendQueueProcessor( indexName );
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
		TransactionContextForTest tc = new TransactionContextForTest();
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
