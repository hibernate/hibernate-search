/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verify behaviour of the AvroSerializationProvider when used concurrently
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1637")
public class ConcurrentServiceTest {

	private static final int THREADS = 300;

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	@Test
	public void verifyAvroSerializerInUse() {
		SerializationProvider serializationProvider = searchFactoryHolder.getSearchFactory().getServiceManager().requestService( SerializationProvider.class );
		assertTrue( "Wrong serialization provider", serializationProvider instanceof AvroSerializationProvider );
	}

	@Test
	public void concurrentSerialization() throws Exception {
		final AtomicBoolean somethingFailed = new AtomicBoolean( false );
		final LuceneWorkSerializer serializer = extractSerializer();
		final ExecutorService executor = Executors.newFixedThreadPool( THREADS );
		final CountDownLatch startLatch = new CountDownLatch( 1 );
		final CountDownLatch endLatch = new CountDownLatch( THREADS );
		for ( int i = 0; i < THREADS; i++ ) {
			executor.execute( new SerializingThread( serializer, startLatch, endLatch, somethingFailed, SerializationTest.buildWorks() ) );
		}
		executor.shutdown();
		startLatch.countDown();
		endLatch.await();
		Assert.assertFalse( somethingFailed.get() );
	}

	@Test
	public void concurrentDeserialization() throws Exception {
		final AtomicBoolean somethingFailed = new AtomicBoolean( false );
		final LuceneWorkSerializer serializer = extractSerializer();
		final ExecutorService executor = Executors.newFixedThreadPool( THREADS );
		final CountDownLatch startLatch = new CountDownLatch( 1 );
		final CountDownLatch endLatch = new CountDownLatch( THREADS );
		final byte[] serializedModel = serializer.toSerializedModel( SerializationTest.buildWorks() );
		for ( int i = 0; i < THREADS; i++ ) {
			executor.execute( new DeserializingThread( serializer, startLatch, endLatch, somethingFailed, serializedModel ) );
		}
		executor.shutdown();
		startLatch.countDown();
		endLatch.await();
		Assert.assertFalse( somethingFailed.get() );
	}

	private LuceneWorkSerializer extractSerializer() {
		return searchFactoryHolder.getSearchFactory().getIndexManagerHolder().getIndexManager( RemoteEntity.class.getName() ).getSerializer();
	}

	private static class SerializingThread implements Runnable {

		private final CountDownLatch startLatch;
		private final CountDownLatch endLatch;
		private final AtomicBoolean somethingFailed;
		private final LuceneWorkSerializer serializer;
		private final List<LuceneWork> workList;

		public SerializingThread(LuceneWorkSerializer serializer, CountDownLatch startLatch, CountDownLatch endLatch, AtomicBoolean somethingFailed, List<LuceneWork> workList) {
			this.serializer = serializer;
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.somethingFailed = somethingFailed;
			this.workList = workList;
		}

		@Override
		public void run() {
			try {
				startLatch.await(); //Maximize chances of working concurrently on the Serializer
				serializer.toSerializedModel( workList );
			}
			catch (InterruptedException|RuntimeException e) {
				e.printStackTrace();
				somethingFailed.set( true );
			}
			endLatch.countDown();
		}

	}

	private static class DeserializingThread implements Runnable {

		private final CountDownLatch startLatch;
		private final CountDownLatch endLatch;
		private final AtomicBoolean somethingFailed;
		private final LuceneWorkSerializer serializer;
		private final byte[] serializedModel;

		public DeserializingThread(LuceneWorkSerializer serializer, CountDownLatch startLatch, CountDownLatch endLatch, AtomicBoolean somethingFailed, byte[] serializedModel) {
			this.serializer = serializer;
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.somethingFailed = somethingFailed;
			this.serializedModel = serializedModel;
		}

		@Override
		public void run() {
			try {
				startLatch.await(); //Maximize chances of working concurrently on the Serializer
				serializer.toLuceneWorks( serializedModel );
			}
			catch (InterruptedException|RuntimeException e) {
				e.printStackTrace();
				somethingFailed.set( true );
			}
			endLatch.countDown();
		}

	}

}
