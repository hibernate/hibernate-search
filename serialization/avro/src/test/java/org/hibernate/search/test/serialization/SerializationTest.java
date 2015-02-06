/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.util.AttributeImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.test.util.SerializationTestHelper;
import org.hibernate.search.test.util.SerializationTestHelper.SerializableStringReader;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Hardy Ferentschik
 */
public class SerializationTest {

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	private SerializationProvider serializationProvider;

	@Before
	public void setUp() {
		ServiceManager serviceManager = new StandardServiceManager(
				new SearchConfigurationForTest(),
				null
		);
		serializationProvider = serviceManager.requestService( SerializationProvider.class );
	}

	@Test
	public void testWorkSerialization() throws Exception {
		LuceneWorkSerializer workSerializer = new LuceneWorkSerializerImpl(
				serializationProvider,
				searchFactoryHolder.getSearchFactory()
		);

		List<LuceneWork> works = buildLuceneWorkList();

		// serialize
		byte[] bytes = workSerializer.toSerializedModel( works );

		// de-serialize
		List<LuceneWork> copyOfWorks = workSerializer.toLuceneWorks( bytes );

		// make sure serialized and de-serialized work list are the same
		assertThat( copyOfWorks ).hasSize( works.size() );
		for ( int index = 0; index < works.size(); index++ ) {
			SerializationTestHelper.assertLuceneWork( works.get( index ), copyOfWorks.get( index ) );
		}
	}

	private List<LuceneWork> buildLuceneWorkList() throws Exception {
		List<LuceneWork> works = new ArrayList<LuceneWork>();
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( new OptimizeLuceneWork( RemoteEntity.class ) ); //class won't be send over
		works.add( new PurgeAllLuceneWork( RemoteEntity.class ) );
		works.add( new PurgeAllLuceneWork( RemoteEntity.class ) );
		works.add( new DeleteLuceneWork( 123l, "123", RemoteEntity.class ) );
		works.add( new DeleteLuceneWork( "Sissi", "Sissi", RemoteEntity.class ) );
		works.add(
				new DeleteLuceneWork(
						new URL( "http://emmanuelbernard.com" ),
						"http://emmanuelbernard.com",
						RemoteEntity.class
				)
		);

		Document doc = buildDocumentWithNumericFields();
		Map<String, String> analyzers = new HashMap<String, String>();
		analyzers.put( "godo", "ngram" );
		works.add( new AddLuceneWork( 123, "123", RemoteEntity.class, doc, analyzers ) );

		doc = buildDocumentWithMultipleMixedTypeFields();
		works.add( new UpdateLuceneWork( 1234, "1234", RemoteEntity.class, doc ) );

		works.add( new AddLuceneWork( 125, "125", RemoteEntity.class, new Document() ) );
		return works;
	}

	private Document buildDocumentWithMultipleMixedTypeFields() {
		Document doc;
		doc = new Document();
		Field field = new Field(
				"StringF",
				"String field",
				Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
		field.setBoost( 3f );
		doc.add( field );

		field = new Field(
				"StringF2",
				"String field 2",
				Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
		doc.add( field );

		byte[] array = new byte[4];
		array[0] = 2;
		array[1] = 5;
		array[2] = 5;
		array[3] = 8;
		field = new Field( "binary", array, 0, array.length );
		doc.add( field );

		SerializableStringReader reader = new SerializableStringReader();
		field = new Field( "ReaderField", reader, Field.TermVector.WITH_OFFSETS );
		doc.add( field );

		List<List<AttributeImpl>> tokens = SerializationTestHelper.buildTokenStreamWithAttributes();

		CopyTokenStream tokenStream = new CopyTokenStream( tokens );
		field = new Field( "tokenstream", tokenStream, Field.TermVector.WITH_POSITIONS_OFFSETS );
		field.setBoost( 3f );
		doc.add( field );
		return doc;
	}

	private Document buildDocumentWithNumericFields() {
		Document doc = new Document();
		Field numField = new DoubleField( "double", 23d, Store.NO );
		doc.add( numField );
		numField = new IntField( "int", 23, Store.NO );
		doc.add( numField );
		numField = new FloatField( "float", 2.3f, Store.NO );
		doc.add( numField );
		numField = new LongField( "long", 23l, Store.NO );
		doc.add( numField );
		return doc;
	}

}
