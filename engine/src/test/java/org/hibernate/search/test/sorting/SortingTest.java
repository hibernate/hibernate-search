/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.SortableFields;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
	public final SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Person.class, UnsortableToy.class );

	private final SearchITHelper helper = new SearchITHelper( factoryHolder );

	@Test
	public void testSortingOnNumericInt() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
		);

		// Sorting Age as string:
		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( sortAsString, 1, 0, 3, 2 );
		// Also check reverse, to ensure this wasn't just luck
		sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING, true ) );
		assertSortedResults( sortAsString, 2, 3, 0, 1 );

		// Sorting Age as Int (numeric):
		Sort sortAsInt = new Sort( new SortField( "ageForIntSorting", SortField.Type.INT ) );
		assertSortedResults( sortAsInt, 0, 3, 2, 1 );
		// Also check reverse, to ensure this wasn't just luck
		sortAsInt = new Sort( new SortField( "ageForIntSorting", SortField.Type.INT, true ) );
		assertSortedResults( sortAsInt, 1, 2, 3, 0 );
	}

	@Test
	public void testSortingOnString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three" ),
				new Person( 1, 10, "Ten" ),
				new Person( 2, 9, "Nine" ),
				new Person( 3, 5, "Five" )
		);

		// Sorting Name
		Sort sortAsString = new Sort( new SortField( "name", SortField.Type.STRING ) );
		assertSortedResults( sortAsString, 3, 2, 1, 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2376")
	public void testSortingOnCollatedString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Éléonore" ),
				new Person( 1, 10, "édouard" ),
				new Person( 2, 9, "Edric" ),
				new Person( 3, 5, "aaron" ),
				new Person( 4, 7, " zach" )
		);

		// Sorting by collated name
		Sort sortAsString = new Sort( new SortField( "collatedName", SortField.Type.STRING ) );
		assertSortedResults( sortAsString, 4, 3, 1, 2, 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2376")
	public void testAnalyzedSortableStoredField() {
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
	@TestForIssue(jiraKey = "HSEARCH-2376")
	@Category(SkipOnElasticsearch.class) // Elasticsearch handles sorts on multi-value fields differently (the default is unclear, but it provides "sort modes" like min, max, avg, etc.)
	public void testSortingOnTokenizedString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "elizabeth" ),
				new Person( 1, 10, "zach other" ),
				new Person( 2, 9, " edric" ),
				new Person( 3, 5, "bob" ),
				new Person( 4, 10, "zach Aaron" )
		);

		// Sorting by tokenized name: ensure only the first token is taken into account
		Sort sortAsString = new Sort(
				new SortField( "tokenizedName", SortField.Type.STRING ),
				SortField.FIELD_DOC // Stabilize the sort for the two zachs
				);
		assertSortedResults( sortAsString, 3, 2, 0, 1, 4 );
	}

	@Test
	public void testSortingOnEmbeddedString() {
		// Index all testData:
		helper.index(
				new Person( 0, 3, "Three", new CuddlyToy( "Hippo" ) ),
				new Person( 1, 10, "Ten", new CuddlyToy( "Giraffe" ) ),
				new Person( 2, 9, "Nine", new CuddlyToy( "Gorilla" ) ),
				new Person( 3, 5, "Five" , new CuddlyToy( "Alligator" ) )
		);

		Sort sortAsString = new Sort( new SortField( "favoriteCuddlyToy.type", SortField.Type.STRING ) );
		assertSortedResults( sortAsString, 3, 1, 2, 0 );
	}

	/**
	 * Sortable fields within an embedded to-many association should be ignored. They should not prevent other sort
	 * fields from working, though.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2000")
	public void testSortingForTypeWithSortableFieldWithinEmbeddedToManyAssociation() {
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

		Sort sortAsString = new Sort( new SortField( "ageForStringSorting", SortField.Type.STRING ) );
		assertSortedResults( sortAsString, 1, 0 );
	}

	@Test
	public void testSortOnNullableNumericField() throws Exception {
		helper.index(
				new Person( 1, 25, "name1" ),
				new Person( 2, 22, null ),
				new Person( 3, null, "name3" )
		);

		HSQuery nameQuery = queryForValueNullAndSorting( "name", SortField.Type.STRING );
		helper.assertThat( nameQuery ).hasResultSize( 1 );

		HSQuery ageQuery = queryForValueNullAndSorting( "ageForNullChecks", SortField.Type.INT );
		helper.assertThat( ageQuery ).hasResultSize( 1 );
	}

	@Test
	public void testExceptionSortingStringFieldAsNumeric() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		helper.index( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "description", SortField.Type.DOUBLE ) );
		HSQuery hsQuery = integrator.createHSQuery( query, entityType );
		hsQuery.sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testExceptionSortingNumericFieldWithStringType() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		helper.index( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "longValue", SortField.Type.STRING ) );
		HSQuery hsQuery = integrator.createHSQuery( query, entityType );
		hsQuery.sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testExceptionSortingNumericFieldWithWrongType() throws Exception {
		thrown.expect( SearchException.class );
		thrown.expectMessage( SORT_TYPE_ERROR_CODE );

		helper.index( new UnsortableToy( "111", "Teddy Bear", 300L, 555 ) );
		Class<?> entityType = UnsortableToy.class;

		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( entityType ).get();
		Query query = queryBuilder
				.keyword()
				.onField( "description" )
				.matching( "Teddy Bear" )
				.createQuery();

		Sort sort = new Sort( new SortField( "longValue", SortField.Type.INT ) );
		HSQuery hsQuery = integrator.createHSQuery( query, entityType );
		hsQuery.sort( sort );
		hsQuery.queryEntityInfos().size();
	}

	@Test
	public void testSortOnNullableNumericFieldArray() throws Exception {
		helper.index(
				new Person( 1, 25, "name1", 1, 2, 3 ),
				new Person( 2, 22, "name2", 1, null, 3 ),
				new Person( 3, 23, "name3", null, null, null )
		);

		Query rangeQuery = queryForRangeOnFieldSorted( 0, 2, "array" );
		Sort sortAsInt = new Sort( new SortField( "array", SortField.Type.INT ) );
		helper.assertThat( rangeQuery )
				.from( Person.class )
				.sort( sortAsInt )
				.hasResultSize( 2 );
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

	private void assertSortedResults(Sort sort, int... expectedIds) {
		helper.assertThat()
				.from( Person.class )
				.sort( sort )
				.matchesExactlyIds( expectedIds );
	}

	private void assertStoredValueEquals(Query query, String fieldName, Object expectedValue) {
		helper.assertThat( query )
				.from( Person.class )
				.projecting( fieldName )
				.matchesExactlySingleProjections( expectedValue );
	}

	private HSQuery queryForValueNullAndSorting(String fieldName, SortField.Type sortType) {
		ExtendedSearchIntegrator integrator = factoryHolder.getSearchFactory();
		QueryBuilder queryBuilder = integrator.buildQueryBuilder().forEntity( Person.class ).get();
		Query query = queryBuilder
				.keyword()
				.onField( fieldName )
				.matching( null )
				.createQuery();

		HSQuery hsQuery = integrator.createHSQuery( query, Person.class );
		Sort sort = new Sort( new SortField( fieldName, sortType ) );
		hsQuery.sort( sort );
		return hsQuery;
	}

	@NormalizerDef(
			name = Person.COLLATING_NORMALIZER_NAME,
			filters = {
					@TokenFilterDef(factory = ASCIIFoldingFilterFactory.class),
					@TokenFilterDef(factory = LowerCaseFilterFactory.class)
			}
	)
	@AnalyzerDef(
			name = Person.TOKENIZING_ANALYZER_NAME,
			tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class)
	)
	@Indexed
	private class Person {

		public static final String COLLATING_NORMALIZER_NAME = "org_hibernate_search_test_sorting_SortingTest_Person_collatingNormalizer";
		public static final String TOKENIZING_ANALYZER_NAME = "org_hibernate_search_test_sorting_SortingTest_Person_tokenizingAnalyzer";

		@DocumentId
		final int id;

		@SortableFields({
				@org.hibernate.search.annotations.SortableField(forField = "ageForStringSorting"),
				@org.hibernate.search.annotations.SortableField(forField = "ageForIntSorting"),
				@org.hibernate.search.annotations.SortableField(forField = "ageForNullChecks")
		})
		@Fields({
			@Field(name = "ageForStringSorting", store = Store.YES, analyze = Analyze.NO, bridge = @FieldBridge(impl = IntegerBridge.class) ),
			@Field(name = "ageForIntSorting", store = Store.YES, analyze = Analyze.NO),
			@Field(name = "ageForNullChecks", store = Store.YES, analyze = Analyze.NO, indexNullAs = "-1")
		})
		final Integer age;

		@SortableFields({
				@org.hibernate.search.annotations.SortableField(forField = "name"),
				@org.hibernate.search.annotations.SortableField(forField = "collatedName"),
				@org.hibernate.search.annotations.SortableField(forField = "tokenizedName")
		})
		@Fields({
				@Field(name = "name", store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN),
				@Field(name = "collatedName", store = Store.YES, normalizer = @Normalizer(definition = COLLATING_NORMALIZER_NAME)),
				@Field(name = "tokenizedName", store = Store.YES, analyzer = @Analyzer(definition = TOKENIZING_ANALYZER_NAME))
		})
		final String name;

		@IndexedEmbedded
		final CuddlyToy favoriteCuddlyToy;

		@IndexedEmbedded
		final List<Friend> friends;

		@Field
		@SortableField
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
