/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verify behaviour of the AvroSerializationProvider when used concurrently
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1637")
public class ConcurrentServiceTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	@Test
	public void concurrentSerialization() throws Exception {
		try ( ServiceReference<LuceneWorkSerializer> serializer = extractSerializer() ) {
			final ConcurrentRunner runner = new ConcurrentRunner(
					new TaskFactory() {
						@Override
						public Runnable createRunnable(int i) throws Exception {
							return new SerializingThread( serializer.get(), buildLuceneWorkList() );
						}
					}
			);
			runner.execute();
		}
	}

	@Test
	public void concurrentDeserialization() throws Exception {
		try ( ServiceReference<LuceneWorkSerializer> serializer = extractSerializer() ) {
			final byte[] serializedModel = serializer.get().toSerializedModel( buildLuceneWorkList() );
			final ConcurrentRunner runner = new ConcurrentRunner(
					new TaskFactory() {
						@Override
						public Runnable createRunnable(int i) throws Exception {
							return new DeserializingThread( serializer.get(), serializedModel );
						}
					}
			);
			runner.execute();
		}
	}

	private ServiceReference<LuceneWorkSerializer> extractSerializer() {
		return searchFactoryHolder.getSearchFactory()
				.getServiceManager()
				.requestReference( LuceneWorkSerializer.class );
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
			byte[] serializedModel = serializer.toSerializedModel( workList );
			assertEquals( "Unexpected count of work instance", 89, serializedModel.length );
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
			List<LuceneWork> luceneWorks = serializer.toLuceneWorks( serializedModel );
			assertEquals( "Unexpected count of work instance", 8, luceneWorks.size() );
		}
	}

	private List<LuceneWork> buildLuceneWorkList() throws Exception {
		List<LuceneWork> works = new ArrayList<>();
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( OptimizeLuceneWork.INSTANCE );
		IndexedTypeIdentifier type = new PojoIndexedTypeIdentifier( RemoteEntity.class );
		works.add( new OptimizeLuceneWork( type ) ); //won't be send over
		works.add( new PurgeAllLuceneWork( type ) );
		works.add( new PurgeAllLuceneWork( type ) );
		works.add( new DeleteLuceneWork( 123l, "123", type ) );
		works.add( new DeleteLuceneWork( "Foo", "Bar", type ) );
		works.add( new AddLuceneWork( 125, "125", type, new Document() ) );
		return works;
	}

}
