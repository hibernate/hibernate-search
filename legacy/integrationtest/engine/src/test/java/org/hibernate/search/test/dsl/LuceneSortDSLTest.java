/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2588")
@Category(SkipOnElasticsearch.class) // Elasticsearch doesn't support non-metadata-providing field bridges
public class LuceneSortDSLTest {
	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntry.class )
			.withProperty( Environment.INDEX_UNINVERTING_ALLOWED, "true" );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void prepareTestData() {
		IndexedEntry entry0 = new IndexedEntry( 0 )
				.setUniqueDoubleField( 2d );
		IndexedEntry entry1 = new IndexedEntry( 1 )
				.setUniqueDoubleField( 1d );
		IndexedEntry entry2 = new IndexedEntry( 2 );
		IndexedEntry entry3 = new IndexedEntry( 3 )
				.setUniqueDoubleField( 3d );

		helper.add(
				entry0,
				entry1,
				entry2,
				entry3
		);
	}

	private QueryBuilder builder() {
		return helper.queryBuilder( IndexedEntry.class );
	}

	@Test
	public void singleField_numericFieldBridge_nonMetadataProviding() throws Exception {
		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.asc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.desc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 1, 2 );
	}

	@Test
	public void singleField_numericFieldBridge_nonMetadataProviding_missingValue_use() throws Exception {
		Sort sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.asc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.desc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 2, 1 );
	}

	@Test(expected = ClassCastException.class)
	public void singleField_numericFieldBridge_nonMetadataProviding_missingValue_use_nonRaw() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "nonMetadataProvidingFieldBridgedNumericField", SortField.Type.DOUBLE )
						.onMissingValue().use( new WrappedDoubleValue( 1.5d ) )
				.createSort();

		sfHolder.getSearchFactory().createHSQuery( query, IndexedEntry.class )
				.sort( sort )
				.queryEntityInfos();
	}

	private AssertBuildingHSQueryContext assertQueryAll(Sort sort) {
		return helper.assertThat().from( IndexedEntry.class ).sort( sort );
	}

	public static class WrappedDoubleValue {
		final Double value;

		public WrappedDoubleValue(Double value) {
			super();
			this.value = value;
		}
	}

	public static class WrappedDoubleValueNonMetadataProvidingFieldBridge implements org.hibernate.search.bridge.FieldBridge, StringBridge {

		@Override
		public String objectToString(Object object) {
			if ( object == null ) {
				return null;
			}
			return object.toString();
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value == null ) {
				return;
			}

			Double doubleValue = ((WrappedDoubleValue) value).value;
			if ( doubleValue == null ) {
				return;
			}

			luceneOptions.addNumericFieldToDocument( name, doubleValue, document );
			luceneOptions.addNumericDocValuesFieldToDocument( name, doubleValue, document );
		}

	}

	@Indexed
	public static class IndexedEntry {

		@DocumentId
		int id;

		@Field(name = "nonMetadataProvidingFieldBridgedNumericField", bridge = @FieldBridge(impl = WrappedDoubleValueNonMetadataProvidingFieldBridge.class))
		@NumericField(forField = "nonMetadataProvidingFieldBridgedNumericField")
		WrappedDoubleValue fieldBridgedNumericField;

		public IndexedEntry() {
		}

		public IndexedEntry(int id) {
			super();
			this.id = id;
		}

		public IndexedEntry setUniqueDoubleField(Double uniqueDoubleField) {
			this.fieldBridgedNumericField = new WrappedDoubleValue( uniqueDoubleField );
			return this;
		}
	}
}
