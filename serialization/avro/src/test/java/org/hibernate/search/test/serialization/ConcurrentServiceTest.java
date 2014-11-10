/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verify behaviour of the AvroSerializationProvider when used concurrently
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1637")
public class ConcurrentServiceTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	@Test
	public void verifyAvroSerializerInUse() {
		SerializationProvider serializationProvider = searchFactoryHolder.getSearchFactory().getServiceManager().requestService( SerializationProvider.class );
		assertTrue( "Wrong serialization provider", serializationProvider instanceof AvroSerializationProvider );
	}

	@Test
	public void concurrentSerialization() throws Exception {
		final LuceneWorkSerializer serializer = extractSerializer();
		final ConcurrentRunner runner = new ConcurrentRunner( new TaskFactory() {
			@Override
			public Runnable createRunnable(int i) throws Exception {
				return new SerializingThread( serializer, SerializationTest.buildWorks() );
			}
		});
		runner.execute();
	}

	@Test
	public void concurrentDeserialization() throws Exception {
		final LuceneWorkSerializer serializer = extractSerializer();
		final byte[] serializedModel = serializer.toSerializedModel( SerializationTest.buildWorks() );
		final ConcurrentRunner runner = new ConcurrentRunner( new TaskFactory() {
			@Override
			public Runnable createRunnable(int i) throws Exception {
				return new DeserializingThread( serializer, serializedModel );
			}
		});
		runner.execute();
	}

	private LuceneWorkSerializer extractSerializer() {
		return searchFactoryHolder.getSearchFactory()
				.getIndexManagerHolder()
				.getIndexManager( RemoteEntity.class.getName() )
				.getSerializer();
	}

	private static class SerializingThread implements Runnable {

		private final LuceneWorkSerializer serializer;
		private final List<LuceneWork> workList;

		public SerializingThread(LuceneWorkSerializer serializer, List<LuceneWork> workList) {
			this.serializer = serializer;
			this.workList = workList;
		}

		@Override
		public void run() {
			serializer.toSerializedModel( workList );
		}

	}

	private static class DeserializingThread implements Runnable {

		private final LuceneWorkSerializer serializer;
		private final byte[] serializedModel;

		public DeserializingThread(LuceneWorkSerializer serializer, byte[] serializedModel) {
			this.serializer = serializer;
			this.serializedModel = serializedModel;
		}

		@Override
		public void run() {
			serializer.toLuceneWorks( serializedModel );
		}

	}

}
