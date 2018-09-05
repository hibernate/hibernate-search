/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.util.SerializationTestHelper;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1795")
public class DocValuesSerializationTest {

	private static final IndexedTypeIdentifier remoteTypeId = new PojoIndexedTypeIdentifier( RemoteEntity.class );

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	private LuceneWorkSerializer workSerializer;

	@Before
	public void setUp() {
		ServiceManager serviceManager = getTestServiceManager();
		workSerializer = serviceManager.requestService( LuceneWorkSerializer.class );
	}

	private ServiceManager getTestServiceManager() {
		SearchConfigurationForTest searchConfiguration = new SearchConfigurationForTest();
		return new StandardServiceManager(
				new SearchConfigurationForTest(),
				new BuildContextForTest( searchConfiguration ) {

					@Override
					public ExtendedSearchIntegrator getUninitializedSearchIntegrator() {
						return searchFactoryHolder.getSearchFactory();
					};
				}
		);
	}

	@Test
	public void testSerializationOfSortedSetDocValuesField() throws Exception {
		Document document = new Document();
		document.add( new SortedSetDocValuesField( "foo", new BytesRef( "hello" ) ) );
		document.add( new SortedSetDocValuesField( "foo", new BytesRef( "world" ) ) );
		List<LuceneWork> expectedWorkList = buildLuceneWorks( document );

		serializeDeserializeAndAssert( expectedWorkList );
	}

	@Test
	public void testSerializationOfSortedDocValuesField() throws Exception {
		Document document = new Document();
		document.add( new SortedDocValuesField( "foo", new BytesRef( "world" ) ) );
		List<LuceneWork> expectedWorkList = buildLuceneWorks( document );

		serializeDeserializeAndAssert( expectedWorkList );
	}

	@Test
	public void testSerializationOfBinaryDocValuesField() throws Exception {
		Document document = new Document();
		document.add( new BinaryDocValuesField( "foo", new BytesRef( "world" ) ) );
		List<LuceneWork> expectedWorkList = buildLuceneWorks( document );

		serializeDeserializeAndAssert( expectedWorkList );
	}

	@Test
	public void testSerializationOfNumericDocValuesField() throws Exception {
		Document document = new Document();
		document.add( new NumericDocValuesField( "foo", 22L ) );
		List<LuceneWork> expectedWorkList = buildLuceneWorks( document );

		serializeDeserializeAndAssert( expectedWorkList );
	}

	@Test
	public void testSerializationOfMultipleDocValuesFields() throws Exception {
		Document document = new Document();
		document.add( new NumericDocValuesField( "foo", 22L ) );
		document.add( new BinaryDocValuesField( "foo", new BytesRef( "world" ) ) );
		document.add( new SortedSetDocValuesField( "foo", new BytesRef( "hello" ) ) );
		document.add( new SortedSetDocValuesField( "foo", new BytesRef( "world" ) ) );
		document.add( new SortedDocValuesField( "foo", new BytesRef( "world" ) ) );
		List<LuceneWork> expectedWorkList = buildLuceneWorks( document );

		serializeDeserializeAndAssert( expectedWorkList );
	}

	private void serializeDeserializeAndAssert(List<LuceneWork> expectedWorkList) {
		// serialize
		byte[] bytes = workSerializer.toSerializedModel( expectedWorkList );

		// de-serialize
		List<LuceneWork> actualWorkList = workSerializer.toLuceneWorks( bytes );

		// make sure serialized and de-serialized work list are the same
		SerializationTestHelper.assertLuceneWorkList( expectedWorkList, actualWorkList );
	}

	private List<LuceneWork> buildLuceneWorks(Document document) {
		List<LuceneWork> works = new ArrayList<>();
		works.add( new AddLuceneWork( 123, "123", remoteTypeId, document ) );
		return works;
	}
}
