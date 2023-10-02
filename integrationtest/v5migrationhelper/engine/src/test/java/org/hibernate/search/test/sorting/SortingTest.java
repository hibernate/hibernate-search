/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.AnalysisNames;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Test to verify we apply the right sorting strategy for non-trivial mapped entities
 *
 * @author Sanne Grinovero
 */
class SortingTest {

	private static final String SORT_TYPE_ERROR_CODE = "HSEARCH000307";

	@RegisterExtension
	public final SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class, UnsortableToy.class );

	private final SearchITHelper helper = new SearchITHelper( factoryHolder );

	@Test
	void testSortingOnNumericInt() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
		);

		// Sorting Age as string:
		Sort sortAsString = builder().sort().byField( "ageForStringSorting" ).createSort();
		assertSortedResults( sortAsString, 1, 0, 3, 2 );
		// Also check reverse, to ensure this wasn't just luck
		sortAsString = builder().sort().byField( "ageForStringSorting" ).desc().createSort();
		assertSortedResults( sortAsString, 2, 3, 0, 1 );

		// Sorting Age as Int (numeric):
		Sort sortAsInt = builder().sort().byField( "ageForIntSorting" ).createSort();
		assertSortedResults( sortAsInt, 0, 3, 2, 1 );
		// Also check reverse, to ensure this wasn't just luck
		sortAsInt = builder().sort().byField( "ageForIntSorting" ).desc().createSort();
		assertSortedResults( sortAsInt, 1, 2, 3, 0 );
	}

	@Test
	void testSortingOnString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
		);

		// Sorting Name
		Sort sortAsString = builder().sort().byField( "name" ).createSort();
		assertSortedResults( sortAsString, 3, 2, 1, 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2376")
	void testSortingOnCollatedString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Éléonore" ),
				new Person( 1, 10, "édouard" ),
				new Person( 2, 9, "Edric" ),
				new Person( 3, 5, "aaron" ),
				new Person( 4, 7, " zach" )
		);

		// Sorting by collated name
		Sort sortAsString = builder().sort().byField( "collatedName" ).createSort();
		assertSortedResults( sortAsString, 4, 3, 1, 2, 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2376")
	void testAnalyzedSortableStoredField() {
		Person person = new Person( 0, 3, "Éléonore" );

		// Index all testData:
		helper.index( person );

		/*
		 * Check the stored value is the value *before* analysis
		 * This check makes sens mainly because we use DocValues for sorting, and
		 * so should field value storage.
		 */
		Query query = factoryHolder.getSearchFactory().buildQueryBuilder().forEntity( Person.class ).get().keyword()
				.onField( "id" )
				.matching( person.id )
				.createQuery();
		assertStoredValueEquals( query, "collatedName", person.name );
	}

	@Test
	void testSortingOnEmbeddedString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three", new CuddlyToy( "Hippo" ) ),
				new Person( 1, 10, "Ten", new CuddlyToy( "Giraffe" ) ),
				new Person( 2, 9, "Nine", new CuddlyToy( "Gorilla" ) ),
				new Person( 3, 5, "Five", new CuddlyToy( "Alligator" ) )
		);

		Sort sortAsString = builder().sort().byField( "favoriteCuddlyToy.type" ).createSort();
		assertSortedResults( sortAsString, 3, 1, 2, 0 );
	}

	/**
	 * Sortable fields within an embedded to-many association should be ignored. They should not prevent other sort
	 * fields from working, though.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2000")
	void testSortingForTypeWithSortableFieldWithinEmbeddedToManyAssociation() {
		// Index all testData:
		helper.index(
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

		Sort sortAsString = builder().sort().byField( "ageForStringSorting" ).createSort();
		assertSortedResults( sortAsString, 1, 0 );
	}

	@Test
	void testSortOnNullableNumericFieldArray() throws Exception {
		helper.index(
				new Person( 1, 25, "name1", 1, 2, 3 ),
				new Person( 2, 22, "name2", 1, null, 3 ),
				new Person( 3, 23, "name3", null, null, null )
		);

		Query rangeQuery = queryForRangeOnFieldSorted( 0, 2, "array" );
		Sort sortAsInt = builder().sort().byField( "array" ).createSort();
		helper.assertThatQuery( rangeQuery )
				.from( Person.class )
				.sort( sortAsInt )
				.hasResultSize( 2 );
	}

	private Query queryForRangeOnFieldSorted(int min, int max, String fieldName) {
		SearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( Person.class ).get();
		return queryBuilder
				.range()
				.onField( fieldName )
				.from( min )
				.to( max )
				.createQuery();
	}

	private QueryBuilder builder() {
		return helper.queryBuilder( Person.class );
	}

	private void assertSortedResults(Sort sort, int... expectedIds) {
		helper.assertThatQuery()
				.from( Person.class )
				.sort( sort )
				.matchesExactlyIds( expectedIds );
	}

	private void assertStoredValueEquals(Query query, String fieldName, Object expectedValue) {
		helper.assertThatQuery( query )
				.from( Person.class )
				.projecting( fieldName )
				.matchesExactlySingleProjections( expectedValue );
	}

	@Indexed
	private class Person {

		public static final String COLLATING_NORMALIZER_NAME = AnalysisNames.NORMALIZER_LOWERCASE_ASCIIFOLDING;

		@DocumentId
		@Field
		final int id;

		@SortableFields({
				@org.hibernate.search.annotations.SortableField(forField = "ageForStringSorting"),
				@org.hibernate.search.annotations.SortableField(forField = "ageForIntSorting"),
				@org.hibernate.search.annotations.SortableField(forField = "ageForNullChecks")
		})
		@Fields({
				@Field(name = "ageForIntSorting", store = Store.YES, analyze = Analyze.NO),
				@Field(name = "ageForNullChecks", store = Store.YES, analyze = Analyze.NO, indexNullAs = "-1")
		})
		final Integer age;
		@Field(name = "ageForStringSorting", store = Store.YES, analyze = Analyze.NO)
		@SortableField(forField = "ageForStringSorting")
		final String ageAsString;

		@SortableFields({
				@org.hibernate.search.annotations.SortableField(forField = "name"),
				@org.hibernate.search.annotations.SortableField(forField = "collatedName")
		})
		@Fields({
				@Field(name = "name", store = Store.YES, analyze = Analyze.NO, indexNullAs = "_null_"),
				@Field(name = "collatedName", store = Store.YES,
						normalizer = @Normalizer(definition = COLLATING_NORMALIZER_NAME))
		})
		final String name;

		@IndexedEmbedded
		final CuddlyToy favoriteCuddlyToy;

		@IndexedEmbedded
		final List<Friend> friends;

		@Field
		@SortableField
		Integer[] array;

		Person(int id, Integer age, String name, CuddlyToy favoriteCuddlyToy) {
			this.id = id;
			this.age = age;
			this.ageAsString = age == null ? null : age.toString();
			this.name = name;
			this.favoriteCuddlyToy = favoriteCuddlyToy;
			this.friends = new ArrayList<Friend>();
		}

		Person(int id, Integer age, String name, List<Friend> friends) {
			this.id = id;
			this.age = age;
			this.ageAsString = age == null ? null : age.toString();
			this.name = name;
			this.favoriteCuddlyToy = null;
			this.friends = friends;
		}

		Person(int id, Integer age, String name, Integer... arrayItems) {
			this.id = id;
			this.age = age;
			this.ageAsString = age == null ? null : age.toString();
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
		@Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = "_null_")
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
		@Field(store = Store.YES, analyze = Analyze.NO)
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
