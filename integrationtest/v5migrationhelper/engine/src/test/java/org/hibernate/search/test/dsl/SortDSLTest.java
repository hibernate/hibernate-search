/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * @author Emmanuel Bernard
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-1872")
public class SortDSLTest {
	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntry.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void prepareTestData() {
		IndexedEntry entry0 = new IndexedEntry( 0 )
				.setTextField(
						"infrequent1 infrequent2 infrequent1"
								+ " inMultipleDocsWithUniqueScores"
								+ " inMultipleDocsWithVariousScores inMultipleDocsWithVariousScores"
				)
				.setNonUniqueIntgerField( 1 )
				.setUniqueIntegerField( 3 )
				.setUniqueDoubleField( 2d )
				/*
				 * Distances:
				 * - to (24,32) with arc method: 10.16km
				 * - to (24,32) with plane method: 11.12km (exact same as entry 1)
				 */
				.setLocation( 24.0d, 31.9d );
		IndexedEntry entry1 = new IndexedEntry( 1 )
				.setTextField(
						"inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores"
								+ " inMultipleDocsWithVariousScores"
				)
				.setNonUniqueIntgerField( 2 )
				.setUniqueIntegerField( 1 )
				.setUniqueDoubleField( 1d )
				/*
				 * Distances:
				 * - to (24,32) with arc method: 11.12km
				 * - to (24,32) with plane method: 11.12km (exact same as entry 0)
				 */
				.setLocation( 23.9d, 32.0d )
				.setPrevious( entry0 );
		entry0.setNext( entry1 );
		IndexedEntry entry2 = new IndexedEntry( 2 )
				.setNonUniqueIntgerField( 1 )
				.setPrevious( entry1 );
		entry1.setNext( entry2 );
		IndexedEntry entry3 = new IndexedEntry( 3 )
				.setTextField(
						"infrequent1"
								+ " inMultipleDocsWithUniqueScores inMultipleDocsWithUniqueScores"
								+ " inMultipleDocsWithVariousScores"
				)
				.setNonUniqueIntgerField( 1 )
				.setUniqueIntegerField( 4 )
				.setUniqueDoubleField( 3d )
				/*
				 * Distances:
				 * - to (24,32) with arc method: 15.06km
				 * - to (24,32) with plane method: 15.73km
				 */
				.setLocation( 23.9d, 32.1d )
				.setPrevious( entry2 );
		entry2.setNext( entry3 );

		entry3.setNext( entry0 );
		entry0.setPrevious( entry3 );

		helper.add(
				entry0,
				entry1,
				entry2,
				entry3
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
		helper.assertThatQuery( query ).from( IndexedEntry.class )
				.sort( sort ).matchesExactlyIds( 0, 3 );

		sort = builder().sort()
				.byScore()
				.asc()
				.createSort();
		helper.assertThatQuery( query ).from( IndexedEntry.class )
				.sort( sort ).matchesExactlyIds( 3, 0 );

		sort = builder().sort()
				.byScore()
				.desc()
				.createSort();
		helper.assertThatQuery( query ).from( IndexedEntry.class )
				.sort( sort ).matchesExactlyIds( 0, 3 );
	}

	@Test
	public void docID() throws Exception {
		Sort sort = builder().sort()
				.byIndexOrder()
				.createSort();
		// Index order is not deterministic
		assertQueryAll( sort ).matchesUnorderedIds( 0, 1, 2, 3 );

		sort = builder().sort()
				.byIndexOrder()
				.asc()
				.createSort();
		// Index order is not deterministic
		assertQueryAll( sort ).matchesUnorderedIds( 0, 1, 2, 3 );

		sort = builder().sort()
				.byIndexOrder()
				.desc()
				.createSort();
		// Index order is not deterministic
		assertQueryAll( sort ).matchesUnorderedIds( 0, 1, 2, 3 );
	}

	@Test
	public void singleField() throws Exception {
		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 0, 3, 2 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.asc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 0, 3, 2 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.desc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 1, 2 );
	}

	@Test
	public void singleField_double_missingValue_use() throws Exception {
		Sort sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.asc()
				.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.desc()
				.onMissingValue().use( 1.5d )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 2, 1 );
	}

	@Test
	public void singleField_integer_missingValue_use() throws Exception {
		Sort sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.onMissingValue().use( 2 )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.asc()
				.onMissingValue().use( 2 )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 2, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.desc()
				.onMissingValue().use( 2 )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 2, 1 );
	}

	@Test
	public void singleField_double_missingValue_sortFirst() throws Exception {
		Sort sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.asc()
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.desc()
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 3, 0, 1 );
	}

	@Test
	public void singleField_integer_missingValue_sortFirst() throws Exception {
		Sort sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.asc()
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "uniqueIntegerField" )
				.desc()
				.onMissingValue().sortFirst()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 3, 0, 1 );
	}

	@Test
	public void singleField_missingValue_sortLast() throws Exception {
		Sort sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.onMissingValue().sortLast()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 0, 3, 2 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.asc()
				.onMissingValue().sortLast()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 1, 0, 3, 2 );

		sort = builder().sort()
				.byField( "uniqueDoubleField" )
				.desc()
				.onMissingValue().sortLast()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 1, 2 );
	}

	@Test
	public void multipleFields() throws Exception {
		Sort sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.andByField( "uniqueDoubleField" )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 0, 3, 2, 1 );

		sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.andByField( "uniqueDoubleField" )
				.asc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 0, 3, 2, 1 );

		sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.andByField( "uniqueDoubleField" )
				.desc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 3, 0, 2, 1 );
	}

	@Test
	public void distance() throws Exception {
		Sort sort = builder().sort()
				.byDistance()
				.onField( "location" )
				.fromLatitude( 24 ).andLongitude( 32 )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 0, 1, 3, 2 );

		sort = builder().sort()
				.byDistance()
				.onField( "location" )
				.fromLatitude( 24 ).andLongitude( 32 )
				.asc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 0, 1, 3, 2 );

		sort = builder().sort()
				.byDistance()
				.onField( "location" )
				.fromLatitude( 24 ).andLongitude( 32 )
				.desc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 3, 1, 0 );
	}

	@Test
	public void fieldThenScore() throws Exception {
		Query query = builder().keyword()
				.onField( "textField" )
				.matching( "inMultipleDocsWithUniqueScores" )
				.createQuery();

		Sort sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.andByScore()
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 3, 0, 1 );

		sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.asc()
				.andByScore()
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 3, 0, 1 );

		sort = builder().sort()
				.byField( "nonUniqueIntegerField" )
				.desc()
				.andByScore()
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 1, 3, 0 );
	}

	@Test
	public void scoreThenField() throws Exception {
		Query query = builder().keyword()
				.onField( "textField" )
				.matching( "inMultipleDocsWithVariousScores" )
				.createQuery();

		Sort sort = builder().sort()
				.byScore()
				.andByField( "uniqueDoubleField" )
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 0, 1, 3 );

		sort = builder().sort()
				.byScore()
				.asc()
				.andByField( "uniqueDoubleField" )
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 1, 3, 0 );

		sort = builder().sort()
				.byScore()
				.desc()
				.andByField( "uniqueDoubleField" )
				.createSort();
		assertQuery( query, sort ).matchesExactlyIds( 0, 1, 3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2587")
	public void embeddedField() throws Exception {
		// Missing value is not provided; the missing values should be considered as 0

		Sort sort = builder().sort()
				.byField( "previous.uniqueDoubleField" )
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "previous.uniqueDoubleField" )
				.asc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 2, 1, 0, 3 );

		sort = builder().sort()
				.byField( "previous.uniqueDoubleField" )
				.desc()
				.createSort();
		assertQueryAll( sort ).matchesExactlyIds( 0, 1, 2, 3 );
	}

	private AssertBuildingHSQueryContext assertQueryAll(Sort sort) {
		return helper.assertThatQuery()
				.from( IndexedEntry.class )
				.sort( sort );
	}

	private AssertBuildingHSQueryContext assertQuery(Query query, Sort sort) {
		return helper.assertThatQuery( query )
				.from( IndexedEntry.class )
				.sort( sort );
	}

	@Indexed
	@Spatial(name = "location")
	public static class IndexedEntry implements Coordinates {

		@DocumentId
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		int id;

		@Field
		String textField;

		@Field
		@SortableField
		Integer nonUniqueIntegerField;

		@Field
		@SortableField
		Double uniqueDoubleField;

		@Field
		@SortableField
		Integer uniqueIntegerField;

		@IndexedEmbedded(depth = 1)
		IndexedEntry previous;

		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "previous")))
		IndexedEntry next;

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

		public IndexedEntry setTextField(String textField) {
			this.textField = textField;
			return this;
		}

		public IndexedEntry setNonUniqueIntgerField(Integer nonUniqueIntegerField) {
			this.nonUniqueIntegerField = nonUniqueIntegerField;
			return this;
		}

		public IndexedEntry setUniqueIntegerField(Integer uniqueIntegerField) {
			this.uniqueIntegerField = uniqueIntegerField;
			return this;
		}

		public IndexedEntry setUniqueDoubleField(Double uniqueDoubleField) {
			this.uniqueDoubleField = uniqueDoubleField;
			return this;
		}

		public IndexedEntry setLocation(Double latitude, Double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
			return this;
		}

		public IndexedEntry setPrevious(IndexedEntry previous) {
			this.previous = previous;
			return this;
		}

		public IndexedEntry setNext(IndexedEntry next) {
			this.next = next;
			return this;
		}
	}
}
