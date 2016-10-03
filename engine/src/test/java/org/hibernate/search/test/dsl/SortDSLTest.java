/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-1872")
public class SortDSLTest {
	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntry.class );

	@Before
	public void prepareTestData() {
		storeData(
				new IndexedEntry( 0 )
				.setMainData(
						"infrequent1 infrequent2 infrequent1 inMultipleDocsWithUniqueScores inMultipleDocsWithVariousScores inMultipleDocsWithVariousScores",
						1, 2d
				)
				/*
				 * Distances:
				 * - to (24,32) with arc method: 10.16km
				 * - to (24,32) with plane method: 11.12km (exact same as entry 1)
				 */
				.setLocation( 24.0d, 31.9d )
		);
		storeData(
				new IndexedEntry( 1 )
				.setMainData(
						"inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores inMultipleDocsWithVariousScores",
						2, 1d
				)
				/*
				 * Distances:
				 * - to (24,32) with arc method: 11.12km
				 * - to (24,32) with plane method: 11.12km (exact same as entry 0)
				 */
				.setLocation( 23.9d, 32.0d )
		);
		storeData(
				new IndexedEntry( 2 )
				.setMainData( null, 1, null)
		);
		storeData(
				new IndexedEntry( 3 )
				.setMainData(
						"infrequent1 inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores inMultipleDocsWithVariousScores",
						1, 3d
				)
				/*
				 * Distances:
				 * - to (24,32) with arc method: 15.06km
				 * - to (24,32) with plane method: 15.73km
				 */
				.setLocation( 23.9d, 32.1d )
		);
	}

	private QueryBuilder builder() {
		return sfHolder.getSearchFactory().buildQueryBuilder().forEntity( IndexedEntry.class ).get();
	}

	@Test
	public void score() throws Exception {
		Query query = builder().keyword()
				.onField( "textField" )
				.matching( "infrequent1" )
				.createQuery();

		Sort sort = builder().sort()
				.byScore()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 3 )
		);

		sort = builder().sort()
				.byScore()
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0 )
		);

		sort = builder().sort()
				.byScore()
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 3 )
		);
	}

	@Test
	public void docID() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byIndexOrder()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 2, 3 )
		);

		sort = builder().sort()
				.byIndexOrder()
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 2, 3 )
		);

		sort = builder().sort()
				.byIndexOrder()
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 2, 1, 0 )
		);
	}

	@Test
	public void singleField() throws Exception {
		Query query = builder().all().createQuery();

		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "uniqueNumericField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1, 2 )
		);
	}

	@Test
	public void singleField_missingValue_use() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "uniqueNumericField" )
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 2, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.asc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 2, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.desc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 2, 1 )
		);
	}

	@Test
	public void singleField_stringFieldBridge() throws Exception {
		Query query = builder().all().createQuery();

		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "fieldBridgedStringField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedStringField" )
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedStringField" )
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1, 2 )
		);
	}

	@Test(expected = SearchException.class)
	public void singleField_stringFieldBridge_missingValue_use() throws Exception {
		builder().sort()
				.byField( "fieldBridgedStringField" )
						.onMissingValue().use( "1.5" )
				.createSort();
	}

	@Test
	public void singleField_numericFieldBridge() throws Exception {
		Query query = builder().all().createQuery();

		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1, 2 )
		);
	}

	@Test
	public void singleField_numericFieldBridge_missingValue_use() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 2, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.asc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 2, 0, 3 )
		);

		sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.desc()
						.onMissingValue().use( 1.5d )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 2, 1 )
		);
	}

	@Test(expected = ClassCastException.class)
	public void singleField_numericFieldBridge_missingValue_use_nonRaw() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "fieldBridgedNumericField", SortField.Type.DOUBLE )
						.onMissingValue().use( new WrappedDoubleValue( 1.5d ) )
				.createSort();
		query( query, sort );
	}

	@Test
	public void singleField_missingValue_sortFirst() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "uniqueNumericField" )
						.onMissingValue().sortFirst()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.asc()
						.onMissingValue().sortFirst()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.desc()
						.onMissingValue().sortFirst()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 3, 0, 1 )
		);
	}

	@Test
	public void singleField_missingValue_sortLast() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "uniqueNumericField" )
						.onMissingValue().sortLast()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 0, 3, 2 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.asc()
						.onMissingValue().sortLast()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 0, 3, 2 )
		);

		sort = builder().sort()
				.byField( "uniqueNumericField" )
						.desc()
						.onMissingValue().sortLast()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1, 2 )
		);
	}

	@Test
	public void multipleFields() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byField( "nonUniqueNumericField" )
				.andByField( "uniqueNumericField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 0, 3, 1 )
		);

		sort = builder().sort()
				.byField( "nonUniqueNumericField" )
				.andByField( "uniqueNumericField" )
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 0, 3, 1 )
		);

		sort = builder().sort()
				.byField( "nonUniqueNumericField" )
				.andByField( "uniqueNumericField" )
				.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 2, 1 )
		);
	}

	@Test
	public void distance() throws Exception {
		Query query = builder().all().createQuery();

		Sort sort = builder().sort()
				.byDistance()
						.onField( "location_hash" )
						.fromLatitude( 24 ).andLongitude( 32 )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 3, 2 )
		);

		sort = builder().sort()
				.byDistance()
						.onField( "location_hash" )
						.fromLatitude( 24 ).andLongitude( 32 )
						.asc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 3, 2 )
		);

		sort = builder().sort()
				.byDistance()
						.onField( "location_hash" )
						.fromLatitude( 24 ).andLongitude( 32 )
						.desc()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 3, 1, 0 )
		);
	}

	@Test
	public void nativeLucene() throws Exception {
		Query query = builder().all().createQuery();

		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byNative( new SortField( "uniqueNumericField", SortField.Type.DOUBLE ) )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byNative( new SortField( "uniqueNumericField", SortField.Type.DOUBLE, false ) )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 2, 1, 0, 3 )
		);

		sort = builder().sort()
				.byNative( new SortField( "uniqueNumericField", SortField.Type.DOUBLE, true ) )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1, 2 )
		);
	}

	@Test
	public void fieldThenScore() throws Exception {
		Query query = builder().keyword()
				.onField( "textField" )
				.matching( "inMultipleDocsWithUniqueScores" )
				.createQuery();

		Sort sort = builder().sort()
				.byField( "nonUniqueNumericField" )
				.andByScore()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1 )
		);

		sort = builder().sort()
				.byField( "nonUniqueNumericField" )
						.asc()
				.andByScore()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 3, 0, 1 )
		);

		sort = builder().sort()
				.byField( "nonUniqueNumericField" )
						.desc()
				.andByScore()
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 3, 0 )
		);
	}

	@Test
	public void scoreThenField() throws Exception {
		Query query = builder().keyword()
				.onField( "textField" )
				.matching( "inMultipleDocsWithVariousScores" )
				.createQuery();

		Sort sort = builder().sort()
				.byScore()
				.andByField( "uniqueNumericField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 3 )
		);

		sort = builder().sort()
				.byScore()
						.asc()
				.andByField( "uniqueNumericField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 1, 3, 0 )
		);

		sort = builder().sort()
				.byScore()
						.desc()
				.andByField( "uniqueNumericField" )
				.createSort();
		assertThat(
				query( query, sort ),
				returnsIDsInOrder( 0, 1, 3 )
		);
	}

	private Matcher<List<EntityInfo>> returnsIDsInOrder(Integer ... idsInOrder) {
		final List<Integer> idsInOrderList = Arrays.asList( idsInOrder );
		return new TypeSafeMatcher<List<EntityInfo>>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a list containing exactly (and in the same order) " )
						.appendValue( idsInOrderList );
			}
			@Override
			protected void describeMismatchSafely(List<EntityInfo> item, Description mismatchDescription) {
				mismatchDescription.appendText( "was " ).appendValue( toIds( item ) );
			}

			private List<Object> toIds(List<EntityInfo> entityInfos) {
				List<Object> result = new ArrayList<>();
				for ( EntityInfo entityInfo : entityInfos ) {
					result.add( entityInfo.getProjection()[0] );
				}
				return result;
			}

			@Override
			protected boolean matchesSafely(List<EntityInfo> actual) {
				return idsInOrderList.equals( toIds( actual ) );
			}
		};
	}

	private List<EntityInfo> query(Query luceneQuery, Sort sort) {
		ExtendedSearchIntegrator sf = sfHolder.getSearchFactory();
		HSQuery hsQuery = sf.createHSQuery( luceneQuery, IndexedEntry.class );
		return hsQuery
				.projection( "id" )
				.sort( sort )
				.queryEntityInfos();
	}

	private void storeData(IndexedEntry entry) {
		Work work = new Work( entry, entry.id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		sfHolder.getSearchFactory().getWorker().performWork( work, tc );
		tc.end();
	}

	public static class WrappedDoubleValue {
		final Double value;

		public WrappedDoubleValue(Double value) {
			super();
			this.value = value;
		}
	}

	public static class WrappedDoubleValueFieldBridge implements org.hibernate.search.bridge.FieldBridge, StringBridge {

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
		}

	}

	public static class WrappedStringValue {
		final String value;

		public WrappedStringValue(String value) {
			super();
			this.value = value;
		}
	}

	public static class WrappedStringValueFieldBridge implements org.hibernate.search.bridge.FieldBridge, StringBridge {

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

			String stringValue = ((WrappedStringValue) value).value;
			if ( stringValue == null ) {
				return;
			}

			luceneOptions.addFieldToDocument( name, stringValue, document );
		}

	}

	@Indexed
	@Spatial(name = "location_hash", spatialMode = SpatialMode.HASH)
	public static class IndexedEntry implements Coordinates {

		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		int id;

		@Field
		String textField;

		@Field
		@SortableField
		Integer nonUniqueNumericField;

		@Field
		@SortableField
		Double uniqueNumericField;

		@Field(bridge = @FieldBridge(impl = WrappedStringValueFieldBridge.class))
		@SortableField
		WrappedStringValue fieldBridgedStringField;

		@Field(bridge = @FieldBridge(impl = WrappedDoubleValueFieldBridge.class))
		@SortableField
		@NumericField
		WrappedDoubleValue fieldBridgedNumericField;

		Double latitude;

		Double longitude;

		public IndexedEntry() {
		}

		public IndexedEntry(int id) {
			super();
			this.id = id;
		}

		@Override
		public Double getLatitude() {
			return latitude;
		}

		@Override
		public Double getLongitude() {
			return longitude;
		}

		public IndexedEntry setMainData(String textField, Integer nonUniqueNumericField, Double uniqueNumericField) {
			this.textField = textField;
			this.nonUniqueNumericField = nonUniqueNumericField;
			this.uniqueNumericField = uniqueNumericField;
			this.fieldBridgedNumericField = new WrappedDoubleValue( uniqueNumericField );
			this.fieldBridgedStringField = new WrappedStringValue(
					uniqueNumericField == null ? null : String.valueOf( uniqueNumericField )
			);
			return this;
		}

		public IndexedEntry setLocation(Double latitude, Double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			return this;
		}
	}
}
