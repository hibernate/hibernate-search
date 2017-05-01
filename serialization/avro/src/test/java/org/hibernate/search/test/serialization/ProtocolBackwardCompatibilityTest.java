/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.serialization;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.impl.StandardServiceManager;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.serialization.avro.impl.KnownProtocols;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.util.SerializationTestHelper;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This tests backwards compatibility between Avro protocol versions.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class ProtocolBackwardCompatibilityTest {

	private static final String RESOURCE_BASE_NAME = "persistent-work-avro-";
	private static final IndexedTypeIdentifier remoteTypeId = new PojoIndexedTypeIdentifier( RemoteEntity.class );

	@Rule
	public SearchFactoryHolder searchFactoryHolder = new SearchFactoryHolder( RemoteEntity.class );

	private LuceneWorkSerializer luceneWorkSerializer;

	@Before
	public void setUp() throws Exception {
		ServiceManager serviceManager = getTestServiceManager();
		luceneWorkSerializer = serviceManager.requestService( LuceneWorkSerializer.class );

		// check target directory -
		// we always write out a serialized version of a work list supported by the latest protocol
		// before upgrading the protocol this serialized version can be put under version control and
		// a new test can be added
		List<LuceneWork> workList = buildV10Works();
		workList.addAll( buildV11Works() );
		workList.addAll( buildV12Works() );
		serializeWithAvro( workList );
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
	public void testDeserializingVersion10ModelWithLatestDeserializer() throws Exception {
		// expected
		List<LuceneWork> expectedWorkList = buildV10Works();

		byte[] serializedModelV10 = loadSerializedWorkForMajorMinor( 1, 0 );

		// actual
		List<LuceneWork> actualWorkList = luceneWorkSerializer.toLuceneWorks( serializedModelV10 );

		SerializationTestHelper.assertLuceneWorkList( expectedWorkList, actualWorkList );
	}

	@Test
	public void testDeserializingVersion11ModelWithLatestDeserializer() throws Exception {
		// expected
		List<LuceneWork> expectedWorkList = buildV10Works();
		expectedWorkList.addAll( buildV11Works() );

		byte[] serializedModelV11 = loadSerializedWorkForMajorMinor( 1, 1 );

		// actual
		List<LuceneWork> actualWorkList = luceneWorkSerializer.toLuceneWorks( serializedModelV11 );

		SerializationTestHelper.assertLuceneWorkList( expectedWorkList, actualWorkList );
	}

	private List<LuceneWork> buildV10Works() throws Exception {
		List<LuceneWork> works = new ArrayList<>();
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( OptimizeLuceneWork.INSTANCE );
		works.add( new OptimizeLuceneWork( remoteTypeId ) ); //class won't be send over
		works.add( new PurgeAllLuceneWork( remoteTypeId ) );
		works.add( new PurgeAllLuceneWork( remoteTypeId ) );
		works.add( new DeleteLuceneWork( 123l, "123", remoteTypeId ) );
		works.add( new DeleteLuceneWork( "Sissi", "Sissi", remoteTypeId ) );
		works.add(
				new DeleteLuceneWork(
						new URL( "http://emmanuelbernard.com" ),
						"http://emmanuelbernard.com",
						remoteTypeId
				)
		);

		Document doc = new Document();
		Field numField = new DoubleField( "double", 23d, Store.NO );
		doc.add( numField );
		numField = new IntField( "int", 23, Store.NO );
		doc.add( numField );
		numField = new FloatField( "float", 2.3f, Store.NO );
		doc.add( numField );
		numField = new LongField( "long", 23l, Store.NO );
		doc.add( numField );

		Map<String, String> analyzers = new HashMap<>();
		analyzers.put( "godo", "ngram" );

		works.add( new AddLuceneWork( 123, "123", remoteTypeId, doc, analyzers ) );

		doc = new Document();
		Field field = new Field(
				"StringF",
				"String field",
				Field.Store.YES,
				Field.Index.ANALYZED,
				Field.TermVector.WITH_OFFSETS
		);
		field.setBoost( 3f );
		doc.add( field );

		field = new Field(
				"StringF2",
				"String field 2",
				Field.Store.YES,
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

		SerializationTestHelper.SerializableStringReader reader = new SerializationTestHelper.SerializableStringReader();
		field = new Field( "ReaderField", reader, Field.TermVector.WITH_OFFSETS );
		doc.add( field );

		List<List<AttributeImpl>> tokens = SerializationTestHelper.buildTokenStreamWithAttributes();

		CopyTokenStream tokenStream = new CopyTokenStream( tokens );
		field = new Field( "tokenstream", tokenStream, Field.TermVector.WITH_POSITIONS_OFFSETS );
		field.setBoost( 3f );
		doc.add( field );

		works.add( new UpdateLuceneWork( 1234, "1234", remoteTypeId, doc ) );
		works.add( new AddLuceneWork( 125, "125", remoteTypeId, new Document() ) );
		return works;
	}

	private List<LuceneWork> buildV11Works() throws Exception {
		List<LuceneWork> works = new ArrayList<>();
		works.add( FlushLuceneWork.INSTANCE );
		return works;
	}

	private List<LuceneWork> buildV12Works() throws Exception {
		List<LuceneWork> works = new ArrayList<>();
		Document document = new Document();
		document.add( new NumericDocValuesField( "foo", 22L ) );
		document.add( new BinaryDocValuesField( "foo", new BytesRef( "world" ) ) );
		document.add( new SortedSetDocValuesField( "foo", new BytesRef( "hello" ) ) );
		document.add( new SortedDocValuesField( "foo", new BytesRef( "world" ) ) );
		works.add( new AddLuceneWork( 123, "123", remoteTypeId, document ) );
		return works;
	}

	private byte[] serializeWithAvro(List<LuceneWork> works) throws Exception {
		byte[] serializedWork = luceneWorkSerializer.toSerializedModel( works );
		storeSerializedForm( serializedWork, KnownProtocols.MAJOR_VERSION, KnownProtocols.LATEST_MINOR_VERSION );
		return serializedWork;
	}

	private void storeSerializedForm(byte[] out, int major, int minor) throws IOException {
		Path targetDir = getTargetDir();
		Path outputFilePath = targetDir.resolve( RESOURCE_BASE_NAME + major + "." + minor );
		try (OutputStream outputStream = new FileOutputStream( outputFilePath.toFile() )) {
			outputStream.write( out );
			outputStream.flush();
		}
	}

	private byte[] loadSerializedWorkForMajorMinor(int major, int minor) throws Exception {
		URL url = getClass().getClassLoader().getResource( RESOURCE_BASE_NAME + major + "." + minor );
		URI uri = url.toURI();
		Path path = Paths.get( uri );
		return Files.readAllBytes( path );
	}

	private Path getTargetDir() {
		URI classesDirUri;

		try {
			classesDirUri = getClass().getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( e );
		}

		return Paths.get( classesDirUri ).getParent();
	}
}
