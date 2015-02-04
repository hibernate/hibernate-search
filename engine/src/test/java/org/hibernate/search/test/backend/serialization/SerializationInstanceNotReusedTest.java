/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.serialization;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.document.Document;
import org.easymock.EasyMock;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * The implementations of SerializationProvider can choose (by SPI contract) to return
 * the same instance to multiple requestors as an optimisation, but this is correct to do only
 * if their implementation is threadsafe.
 * It's more common that implementors will opt to return different instances
 * when this might be a simpler strategy to satisfy the threadsafety requirement.
 *
 * For such cases, we need to verify that the various clients using such a Service will not
 * cache it internally in their own fields.
 *
 * This test verifies that an IndexManager won't reuse the instance multiple times; it won't
 * cover all configurable scenarios as there are many: threadsafety can't be guaranteed than
 * by careful design, so the point here is to spot the most obvious mistakes, to draw attention
 * on the problem in case of creative refactoring.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1637")
public class SerializationInstanceNotReusedTest {

	private CountingSerializationProvider countingServiceInstance = new CountingSerializationProvider();

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Book.class )
			.withService( SerializationProvider.class, countingServiceInstance );

	@Test
	public void testPropertiesIndexing() {
		ExtendedSearchIntegrator searchFactory = factoryHolder.getSearchFactory();
		IndexManager indexManager = searchFactory.getIndexManagerHolder().getIndexManager( "books" );
		Assert.assertNotNull( indexManager );
		Assert.assertEquals( 0, countingServiceInstance.serializerGetCount.get() );
		Assert.assertEquals( 0, countingServiceInstance.deserializerGetCount.get() );

		//Serialize some work:
		indexManager.getSerializer().toSerializedModel( makeSomeWork() );
		Assert.assertEquals( 1, countingServiceInstance.serializerGetCount.get() );
		Assert.assertEquals( 0, countingServiceInstance.deserializerGetCount.get() );

		//Serialize again, note the point of the test is to request all references again:
		indexManager.getSerializer().toSerializedModel( makeSomeWork() );
		Assert.assertEquals( 2, countingServiceInstance.serializerGetCount.get() );
		Assert.assertEquals( 0, countingServiceInstance.deserializerGetCount.get() );

		//Now Deserialize:
		indexManager.getSerializer().toLuceneWorks( makeSomeSerializedWork() );
		Assert.assertEquals( 2, countingServiceInstance.serializerGetCount.get() );
		Assert.assertEquals( 1, countingServiceInstance.deserializerGetCount.get() );

		//Now Deserialize again:
		indexManager.getSerializer().toLuceneWorks( makeSomeSerializedWork() );
		Assert.assertEquals( 2, countingServiceInstance.serializerGetCount.get() );
		Assert.assertEquals( 2, countingServiceInstance.deserializerGetCount.get() );
	}

	private byte[] makeSomeSerializedWork() {
		//Random: we won't deserialize this
		return new byte[]{ 0, 1, 2};
	}

	private List<LuceneWork> makeSomeWork() {
		List<LuceneWork> list = new LinkedList<>();
		//just some random data:
		list.add( new AddLuceneWork( Integer.valueOf( 5 ), "id:5", Book.class, new Document() ) );
		list.add( new AddLuceneWork( Integer.valueOf( 6 ), "id:6", Book.class, new Document() ) );
		return list;
	}

	@Indexed(index = "books")
	private static class Book {
		@DocumentId long id;
		@Field String title;
	}

	/**
	 * This SerializationProvider mocks Serializer and Deserializer, it won't actually do the work
	 * but it will keep count of how many times each service is being requested.
	 */
	private static class CountingSerializationProvider implements SerializationProvider {

		private final AtomicInteger serializerGetCount = new AtomicInteger();
		private final AtomicInteger deserializerGetCount = new AtomicInteger();
		private final Serializer mockSerializer = EasyMock.createNiceMock( Serializer.class );
		private final Deserializer mockDeserializer = EasyMock.createNiceMock( Deserializer.class );

		CountingSerializationProvider() {
			EasyMock.replay( mockSerializer, mockDeserializer );
		}

		@Override
		public Serializer getSerializer() {
			serializerGetCount.incrementAndGet();
			return mockSerializer;
		}

		@Override
		public Deserializer getDeserializer() {
			deserializerGetCount.incrementAndGet();
			return mockDeserializer;
		}
	}

}
