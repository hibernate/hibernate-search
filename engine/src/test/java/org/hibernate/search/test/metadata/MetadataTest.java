/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Similarity;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.test.util.FooAnalyzer;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-436")
public class MetadataTest {
	private static final List<String> TEST_INDEX_NAMES = new ArrayList<String>();

	static {
		TEST_INDEX_NAMES.add( "index-0" );
		TEST_INDEX_NAMES.add( "index-0" );
		TEST_INDEX_NAMES.add( "index-0" );
	}

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		ConfigContext configContext = new ConfigContext( new ManualConfiguration() );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testIsIndexed() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Foo.class );
		assertEquals( "Wrong indexed type", Foo.class, typeDescriptor.getType() );
		assertTrue( typeDescriptor.isIndexed() );
	}

	@Test
	public void testDefaultStaticBoost() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Foo.class );
		assertEquals( "The default boost should be 1.0f", 1.0f, typeDescriptor.getStaticBoost() );
	}

	@Test
	public void testExplicitStaticBoost() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Fubar.class );
		assertEquals( "The default boost should be 42.0f", 42.0f, typeDescriptor.getStaticBoost() );
	}

	@Test
	public void testDefaultDynamicBoost() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Foo.class );
		assertTrue( typeDescriptor.getDynamicBoost() instanceof DefaultBoostStrategy );
	}

	@Test
	public void testExplicitDynamicBoost() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Fubar.class );
		assertTrue( typeDescriptor.getDynamicBoost() instanceof DoublingBoost );
	}

	@Test
	public void testIdFieldDescriptor() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Fubar.class, "id" );
		String fieldName = fieldDescriptor.getName();
		assertEquals( "Wrong field name", "id", fieldName );
		assertTrue(
				"This field should be the id field",
				fieldDescriptor.getFieldType().equals( FieldDescriptor.Type.ID )
		);
	}

	@Test
	public void testFieldDescriptorLuceneOptions() {
		String fieldName = "my-snafu";
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, fieldName );

		assertEquals( "Wrong field name", fieldName, fieldDescriptor.getName() );
		assertEquals( Index.NO, fieldDescriptor.getIndex() );
		assertEquals( Analyze.NO, fieldDescriptor.getAnalyze() );
		assertEquals( Store.YES, fieldDescriptor.getStore() );
		assertEquals( Norms.NO, fieldDescriptor.getNorms() );
		assertEquals( TermVector.WITH_POSITIONS, fieldDescriptor.getTermVector() );
		assertEquals( 10.0f, fieldDescriptor.getBoost() );

		assertFalse( fieldDescriptor.indexNull() );
		assertNull( fieldDescriptor.indexNullAs() );

		assertFalse( fieldDescriptor.isNumeric() );
		assertNull( "For non numeric fields the precision step is undefined", fieldDescriptor.precisionStep() );
	}

	@Test
	public void testFieldDescriptorDefaultNullIndexOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertFalse( fieldDescriptor.indexNull() );
		assertNull( fieldDescriptor.indexNullAs() );
	}

	@Test
	public void testFieldDescriptorNullIndexOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "nullValue" );

		assertTrue( fieldDescriptor.indexNull() );
		assertEquals( "snafu", fieldDescriptor.indexNullAs() );
	}

	@Test
	public void testFieldDescriptorDefaultNumericOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertFalse( fieldDescriptor.isNumeric() );
		assertNull( "For non numeric fields the precision step is undefined", fieldDescriptor.precisionStep() );
	}

	@Test
	public void testFieldDescriptorExplicitNumericOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "numericField" );

		assertTrue( fieldDescriptor.isNumeric() );
		assertEquals( "Wrong precision step", new Integer( 16 ), fieldDescriptor.precisionStep() );
	}

	@Test
	public void testFieldDescriptorDefaultAnalyzer() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertNotNull( fieldDescriptor.getAnalyzer() );
		assertTrue( fieldDescriptor.getAnalyzer() instanceof StandardAnalyzer );
	}

	@Test
	public void testFieldDescriptorExplicitAnalyzer() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "custom" );

		assertNotNull( fieldDescriptor.getAnalyzer() );
		assertTrue( fieldDescriptor.getAnalyzer() instanceof FooAnalyzer );
	}

	@Test
	public void testFieldDescriptorDefaultFieldBridge() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof StringBridge );
	}

	@Test
	public void testIndexInformation() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Foo.class );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertEquals( "Wrong index name", TEST_INDEX_NAMES.get( 0 ), indexDescriptors.iterator().next().getName() );
	}

	@Test
	public void testSharedIndexInformation() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( Foo.class );
		IndexedTypeDescriptor typeDescriptor = new IndexedTypeDescriptorImpl(
				typeMetadata,
				getDummyShardedIndexManager()
		);
		assertTrue( typeDescriptor.isSharded() );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertTrue( indexDescriptors.size() == 3 );
		for ( IndexDescriptor indexDescriptor : indexDescriptors ) {
			String shardName = indexDescriptor.getName();
			assertTrue( "Missing shard name: " + shardName, TEST_INDEX_NAMES.contains( indexDescriptor.getName() ) );
		}
	}

	@Test
	public void testFieldAnnotationOnFieldAndGetterCreatesTwoFieldDescriptors() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Susfu.class );

		Set<FieldDescriptor> fieldDescriptors = typeDescriptor.getFieldsForProperty( "susfu" );
		assertEquals( "There should be two field descriptors", 2, fieldDescriptors.size() );
	}

	@Test
	public void testRetrievingPropertyDescriptors() {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( Snafu.class );

		Set<PropertyDescriptor> propertyDescriptors = typeDescriptor.getIndexedProperties();
		assertEquals( "There should be 5 properties defined in Snafu", 5, propertyDescriptors.size() );
		Set<String> expectedPropertyNames = new HashSet<String>();
		expectedPropertyNames.add( "id" );
		expectedPropertyNames.add( "snafu" );
		expectedPropertyNames.add( "numericField" );
		expectedPropertyNames.add( "nullValue" );
		expectedPropertyNames.add( "custom" );

		for ( PropertyDescriptor propertyDescriptor : propertyDescriptors ) {
			assertTrue(
					"Unexpected property name: " + propertyDescriptor.getName(),
					expectedPropertyNames.contains( propertyDescriptor.getName() )
			);
		}
	}

	private IndexedTypeDescriptor getTypeDescriptor(Class<?> clazz) {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( clazz );
		return new IndexedTypeDescriptorImpl(
				typeMetadata,
				getDummyUnShardedIndexManager()
		);
	}

	private FieldDescriptor getFieldDescriptor(Class<?> clazz, String fieldName) {
		IndexedTypeDescriptor typeDescriptor = getTypeDescriptor( clazz );
		return typeDescriptor.getIndexedField( fieldName );
	}

	private IndexManager[] getDummyUnShardedIndexManager() {
		IndexManager[] managers = new IndexManager[1];
		managers[0] = new DummyIndexManager( TEST_INDEX_NAMES.get( 0 ) );
		return managers;
	}

	private IndexManager[] getDummyShardedIndexManager() {
		IndexManager[] managers = new IndexManager[3];
		int i = 0;
		for ( String indexName : TEST_INDEX_NAMES ) {
			managers[i] = new DummyIndexManager( indexName );
			i++;
		}
		return managers;
	}

	@Indexed
	public static class Foo {
		@DocumentId
		private long id;
	}

	@Indexed
	@Boost(42.0f)
	@DynamicBoost(impl = DoublingBoost.class)
	public static class Fubar {
		@DocumentId
		private long id;
	}

	@Indexed
	public static class Snafu {
		@DocumentId
		private long id;

		@Field(name = "my-snafu",
				index = Index.NO,
				store = Store.YES,
				analyze = Analyze.NO,
				norms = Norms.NO,
				termVector = TermVector.WITH_POSITIONS,
				boost = @Boost(10.0f))
		private String snafu;

		@Field
		@NumericField(precisionStep = 16)
		private int numericField;

		@Field(indexNullAs = "snafu")
		private String nullValue;

		@Field
		@org.hibernate.search.annotations.Analyzer(impl = FooAnalyzer.class)
		private String custom;
	}

	public static class Susfu {
		@DocumentId
		private long id;

		@Field
		private String susfu;

		@Field
		public String getSusfu() {
			return susfu;
		}
	}

	public static class DoublingBoost implements BoostStrategy {
		@Override
		public float defineBoost(Object value) {
			return 2.0f;
		}
	}

	private static class DummyIndexManager implements IndexManager {
		private final String indexName;

		public DummyIndexManager(String indexName) {
			this.indexName = indexName;
		}

		@Override
		public String getIndexName() {
			return indexName;
		}

		@Override
		public ReaderProvider getReaderProvider() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void performOperations(List<LuceneWork> queue, IndexingMonitor monitor) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void initialize(String indexName, Properties properties, WorkerBuildContext context) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void destroy() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public Set<Class<?>> getContainedTypes() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public Similarity getSimilarity() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void setSimilarity(Similarity newSimilarity) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public Analyzer getAnalyzer(String name) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void setSearchFactory(SearchFactoryImplementor boundSearchFactory) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void addContainedEntity(Class<?> entity) {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public void optimize() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}

		@Override
		public LuceneWorkSerializer getSerializer() {
			throw new UnsupportedOperationException( "Not supported in dummy index manager" );
		}
	}
}


