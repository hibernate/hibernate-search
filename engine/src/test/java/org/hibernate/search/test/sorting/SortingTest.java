/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test to verify we apply the right sorting strategy for non-trivial mapped entities
 *
 * @author Sanne Grinovero
 */
public class SortingTest {

	private static final String SORT_TYPE_ERROR_CODE = "HSEARCH000307";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class, UnsortableToy.class );

	@Test
	public void testSortingOnNumericInt() {
		// Index all testData:
		storeTestingData(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
			);

		// Non sorted, expect results in indexing order:
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		assertSortedResults( query, null, 0, 1, 2, 3);

		// Sorting Age as string:
		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 1, 0, 3, 2 );

		// Sorting Age as Int (numeric):
		Sort sortAsInt = new Sort( new SortField( "ageForIntSorting", SortField.Type.INT ) );
		assertSortedResults( query, sortAsInt, 0, 3, 2, 1 );
	}

	@Test
	public void testSortingOnString() {
		// Index all testData:
		storeTestingData(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
			);

		// Sorting Name
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		Sort sortAsString = new Sort( new SortField( "name", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 3, 2, 1, 0 );
	}

	@Test
	public void testSortingOnEmbeddedString() {
		// Index all testData:
		storeTestingData(
				new Person( 0, 3, "Three", new CuddlyToy( "Hippo" ) ),
				new Person( 1, 10, "Ten", new CuddlyToy( "Giraffe" ) ),
				new Person( 2, 9, "Nine", new CuddlyToy( "Gorilla" ) ),
				new Person( 3, 5, "Five" , new CuddlyToy( "Alligator" ) )
			);

		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		Sort sortAsString = new Sort( new SortField( "favoriteCuddlyToy.type", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 3, 1, 2, 0 );
	}

	/**
	 * Sortable fields within an embedded to-many association should be ignored. They should not prevent other sort
	 * fields from working, though.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2000")
	public void testSortingForTypeWithSortableFieldWithinEmbeddedToManyAssociation() {
		// Index all testData:
		storeTestingData(
				new Person(
						0,
						3,
						"Three",
						Arrays.asList(
								new Friend( new CuddlyToy( "Hippo" ) ),
								new Friend( new CuddlyToy( "Giraffe" ) )
						)
				),
				new Person(
						1,
						10,
						"Ten",
						Arrays.asList(
								new Friend( new CuddlyToy( "Gorilla" ) ),
								new Friend( new CuddlyToy( "Alligator" ) )
						)
				)
			);

		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().all().createQuery();
		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( query, sortAsString, 1, 0 );
	}

	@Test
	public void testSortOnNullableNumericField() throws Exception {
		storeTestingData(
				new Person( 1, 25, "name1" ),
				new Person( 2, 22, null ),
				new Person( 3, null, "name3" )
			);

		HSQuery nameQuery = queryForValueNullAndSorting( "name", SortField.Type.STRING );
		assertEquals( nameQuery.queryEntityInfos().size(), 1 );

		HSQuery ageQuery = queryForValueNullAndSorting( "ageForNullChecks", SortField.Type.INT );
		assertEquals( ageQuery.queryEntityInfos().size(), 1 );
	}

	@Test
	public void testExceptionSortingStringFieldAsNumeric() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		storeTestingData( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "description", SortField.Type.DOUBLE ) );
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( entityType ) ).sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testExceptionSortingNumericFieldWithStringType() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		storeTestingData( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "longValue", SortField.Type.STRING ) );
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( entityType ) ).sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testExceptionSortingNumericFieldWithWrontType() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		storeTestingData( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "longValue", SortField.Type.INT ) );
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( entityType ) ).sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testSortOnNullableNumericFieldArray() throws Exception {
		storeTestingData(
				new Person( 1, 25, "name1", 1, 2, 3 ),
				new Person( 2, 22, "name2", 1, null, 3 ),
				new Person( 3, 23, "name3", null, null, null )
			);

		Query rangeQuery = queryForRangeOnFieldSorted( 0, 2, "array" );
		Sort sortAsInt = new Sort( new SortField( "array", SortField.Type.INT ) );
		assertNumberOfResults( 2, rangeQuery, sortAsInt );
	}

	private void assertNumberOfResults(int expectedResultsNumber, Query query, Sort sort) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( Person.class ) );
		if ( sort != null ) {
			hsQuery.sort( sort );
		}
		assertEquals( expectedResultsNumber, hsQuery.queryResultSize() );
	}

	private Query queryForRangeOnFieldSorted(int min, int max, String fieldName) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( Person.class ).get();
		return queryBuilder
				.range()
				.onField( fieldName )
				.from( min )
				.to( max )
				.createQuery();
	}

	private void storeTestingData(Person... testData) {
		Worker worker = factoryHolder.getSearchFactory().getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		for ( int i = 0; i < testData.length; i++ ) {
			Person p = testData[i];
			worker.performWork( new Work( p, p.id, WorkType.INDEX ), tc);
		}
		tc.end();
	}

	private void storeTestingData(UnsortableToy... testData) {
		Worker worker = factoryHolder.getSearchFactory().getWorker();
		TransactionContextForTest tc = new TransactionContextForTest();
		for ( int i = 0; i < testData.length; i++ ) {
			UnsortableToy toy = testData[i];
			worker.performWork( new Work( toy, toy.id, WorkType.INDEX ), tc);
		}
		tc.end();
	}

	private void assertSortedResults(Query query, Sort sort, int... expectedIds) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( Person.class ) );
		if ( sort != null ) {
			hsQuery.sort( sort );
		}
		assertEquals( expectedIds.length, hsQuery.queryResultSize() );
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		assertEquals( expectedIds.length, queryEntityInfos.size() );
		for ( int i = 0; i < expectedIds.length; i++ ) {
			EntityInfo entityInfo = queryEntityInfos.get( i );
			assertNotNull( entityInfo );
			assertEquals( expectedIds[i], entityInfo.getId() );
		}
	}

	private HSQuery queryForValueNullAndSorting(String fieldName, SortField.Type sortType) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( Person.class ).get();
		Query query = queryBuilder
				.keyword()
				.onField( fieldName )
				.matching( null )
				.createQuery();

		HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
		Sort sort = new Sort( new SortField( fieldName, sortType ) );
		hsQuery.targetedEntities( Arrays.<Class<?>>asList( Person.class ) ).sort( sort );
		return hsQuery;
	}

	@Indexed
	private class Person {

		@DocumentId
		final int id;

		@SortableFields({
				@org.hibernate.search.annotations.SortableField(forField = "ageForStringSorting"),
				@org.hibernate.search.annotations.SortableField(forField = "ageForIntSorting")
		})
		@Fields({
			@Field(name = "ageForStringSorting", store = Store.YES, analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class) ),
			@Field(name = "ageForIntSorting", store = Store.YES, analyze = Analyze.NO),
			@Field(name = "ageForNullChecks", store = Store.YES, analyze = Analyze.NO, indexNullAs = "-1")
		})
		final Integer age;

		@org.hibernate.search.annotations.SortableField
		@Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
		final String name;

		@IndexedEmbedded
		final CuddlyToy favoriteCuddlyToy;

		@IndexedEmbedded
		final List<Friend> friends;

		@Field
		@IndexedEmbedded//TODO improve error message when this is missing
		Integer[] array;

		Person(int id, Integer age, String name, CuddlyToy favoriteCuddlyToy) {
			this.id = id;
			this.age = age;
			this.name = name;
			this.favoriteCuddlyToy = favoriteCuddlyToy;
			this.friends = new ArrayList<Friend>();
		}

		Person(int id, Integer age, String name, List<Friend> friends) {
			this.id = id;
			this.age = age;
			this.name = name;
			this.favoriteCuddlyToy = null;
			this.friends = friends;
		}

		Person(int id, Integer age, String name, Integer... arrayItems) {
			this.id = id;
			this.age = age;
			this.name = name;
			this.array = arrayItems;
			this.favoriteCuddlyToy = null;
			this.friends = new ArrayList<Friend>();
		}
	}

	private static class Friend {

		@IndexedEmbedded
		final CuddlyToy favoriteCuddlyToy;

		public Friend(CuddlyToy favoriteCuddlyToy) {
			this.favoriteCuddlyToy = favoriteCuddlyToy;
		}
	}

	private class CuddlyToy {

		@org.hibernate.search.annotations.SortableField
		@Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
		String type;

		public CuddlyToy(String type) {
			this.type = type;
		}
	}

	@Indexed
	private class UnsortableToy {

		@DocumentId
		String id;

		@org.hibernate.search.annotations.SortableField
		@Field(store = Store.YES, analyze = Analyze.YES)
		String description;

		@org.hibernate.search.annotations.SortableField
		@Field(store = Store.YES, analyze = Analyze.NO)
		Long longValue;

		@org.hibernate.search.annotations.SortableField
		@Field(store = Store.YES, analyze = Analyze.NO)
		Integer integerValue;

		public UnsortableToy(String id, String description, Long longValue, Integer integerValue) {
			this.id = id;
			this.description = description;
			this.longValue = longValue;
			this.integerValue = integerValue;
		}
	}
}
