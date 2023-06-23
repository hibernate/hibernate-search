/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.testsupport.AnalysisNames;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.PortedToSearch6;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.hamcrest.CoreMatchers;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//DO NOT AUTO INDENT THIS FILE.
//MY DSL IS BEAUTIFUL, DUMB INDENTATION IS SCREWING IT UP
public class DSLTest {
	private final Calendar calendar = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Month.class, Car.class,
			SportsCar.class, Animal.class, Day.class, Coffee.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	private Date february;

	@Before
	public void setUp() throws Exception {
		indexTestData();
	}

	@Test
	public void testUseOfFieldBridge() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb.keyword().onField( "monthValue" ).matching( 2 ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	public void testTermQueryOnAnalyzer() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//regular term query
		Query query = monthQb.keyword().onField( "mythology" ).matching( "cold" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		//term query based on several words
		query = monthQb.keyword().onField( "mythology" ).matching( "colder darker" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		//term query applying the analyzer and generating one term per word
		query = monthQb.keyword().onField( "mythology_stem" ).matching( "snowboard" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		//term query applying the analyzer and generating several terms per word
		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snobored" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		//term query not using analyzers
		query = monthQb.keyword().onField( "mythology" ).ignoreAnalyzer().matching( "Month" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2785")
	public void testTermQueryOnNormalizer() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb.keyword().onField( "name" ).matching( "February" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "name" ).matching( "february" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_normalized" ).matching( "Month of fake spring" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_normalized" ).matching( "month of fake spring" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_normalized" ).matching( "Month" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_normalized" ).matching( "month" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testFuzzyQuery() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );


		//fuzzy search with custom threshold and prefix
		Query query = monthQb
				.keyword()
				.fuzzy()
				.withEditDistanceUpTo( 1 )
				.withPrefixLength( 1 )
				.onField( "mythology" )
				.matching( "calder" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testFuzzyQueryOnMultipleFields() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.keyword()
				.fuzzy()
				.withEditDistanceUpTo( 2 )
				.withPrefixLength( 1 )
				.onFields( "mythology", "history" )
				.matching( "showboarding" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 2 );
	}

	@Test
	public void testWildcardQuery() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.keyword()
				.wildcard()
				.onField( "mythology" )
				.matching( "mon*" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1811")
	public void testWildcardQueryOnMultipleFields() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.keyword()
				.wildcard()
				.onFields( "mythology", "history" )
				.matching( "snowbo*" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 3 );
	}

	@Test
	public void testQueryCustomization() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );


		//combined query, January and February both contain whitening but February in a longer text
		Query query = monthQb
				.bool()
				.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
				.should( monthQb.keyword().onField( "history" ).matching( "whitening" ).createQuery() )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1, 2 );

		//boosted query, January and February both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb
				.bool()
				.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
				.should( monthQb.keyword().onField( "history" ).boostedTo( 30 ).matching( "whitening" ).createQuery() )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 1 );

		//FIXME add other method tests besides boostedTo
	}

	@Test
	public void testMultipleFields() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//combined query, January and February both contain whitening but February in a longer text
		Query query = monthQb.keyword()
				.onField( "mythology" )
				.andField( "history" )
				.matching( "whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1, 2 );

		//combined query, January and February both contain whitening but February in a longer text
		query = monthQb.keyword()
				.onFields( "mythology", "history" )
				.boostedTo( 30 )
				.matching( "whitening" ).createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1, 2 );

		//boosted query, January and February both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb.keyword()
				.onField( "mythology" )
				.andField( "history" )
				.boostedTo( 30 )
				.matching( "whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 1 );
	}

	@Test
	public void testBoolean() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//must
		Query query = monthQb
				.bool()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );

		//must not + all
		query = monthQb
				.bool()
				.should( monthQb.all().createQuery() )
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.not()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 3 );

		//implicit must not + all (not recommended)
		query = monthQb
				.bool()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.not()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 3 );

		//all except (recommended)
		query = monthQb
				.all()
				.except( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2, 3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2034")
	public void testBooleanWithoutScoring() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//must + disable scoring
		Query query = monthQb
				.bool()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.disableScoring()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );
		assertTrue( query instanceof BooleanQuery );
		BooleanQuery bq = (BooleanQuery) query;
		BooleanClause firstBooleanClause = bq.clauses().get( 0 );
		assertFalse( firstBooleanClause.isScoring() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2037")
	public void testBooleanWithOnlyNegationQueries() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//must + disable scoring
		Query query = monthQb
				.bool()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.not() //expectation: exclude January
				.must( monthQb.keyword().onField( "mythology" ).matching( "snowboarding" ).createQuery() )
				.not() //expectation: exclude February
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 3 );

		assertTrue( query instanceof BooleanQuery );
		BooleanQuery bq = (BooleanQuery) query;
		BooleanClause firstBooleanClause = bq.clauses().get( 0 );
		assertFalse( firstBooleanClause.isScoring() );
	}

	@Test(expected = SearchException.class)
	public void testIllegalBooleanJunction() {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );
		//forgetting to set any condition on the boolean, an exception shall be thrown:
		BooleanJunction<?> booleanJunction = monthQb.bool();
		assertTrue( booleanJunction.isEmpty() );
		booleanJunction.createQuery();
		fail( "should not reach this point" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2565")
	public void testBooleanWithNullClauses() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		// must/should with null clauses
		Query query = monthQb
				.bool()
				.must( null )
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.should( null )
				.createQuery();

		assertThat( query, CoreMatchers.instanceOf( BoostQuery.class ) );

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );

		// must not / filter with null clauses
		query = monthQb
				.bool()
				.must( null ).not()
				.must( null ).disableScoring()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.createQuery();

		assertThat( query, CoreMatchers.instanceOf( BoostQuery.class ) );

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );
	}

	@Test
	public void testRangeQueryFromTo() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		Date from = calendar.getTime();
		calendar.set( 1910, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.from( from )
				.to( to ).excludeLimit()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This only works because of a Lucene-specific hack in org.hibernate.search.bridge.util.impl.NumericFieldUtils.createNumericRangeQuery
	public void testRangeQueryFromToIgnoreFieldBridge() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		Date from = calendar.getTime();
		calendar.set( 1910, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.from( from )
				.to( to )
				.excludeLimit()
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	public void testRangeQueryBelow() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1800, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.below( to )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 3 );

		query = monthQb.range()
				.onField( "raindropInMm" )
				.below( 0.24d )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This only works because of a Lucene-specific hack in org.hibernate.search.bridge.util.impl.NumericFieldUtils.createNumericRangeQuery
	public void testRangeQueryBelowIgnoreFieldBridge() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1800, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.below( to )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 3 );
	}

	@Test
	public void testRangeQueryAbove() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1900, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.above( to )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2 );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This only works because of a Lucene-specific hack in org.hibernate.search.bridge.util.impl.NumericFieldUtils.createNumericRangeQuery
	public void testRangeQueryAboveIgnoreFieldBridge() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1900, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.above( to )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2 );
	}

	@Test
	public void testRangeQueryAboveInclusive() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		// test the limits, inclusive
		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.above( february )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 2 );
	}

	@Test
	public void testRangeQueryAboveExclusive() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		// test the limits, exclusive
		Query query = monthQb
				.range()
				.onField( "estimatedCreation" )
				.above( february ).excludeLimit()
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).matchesNone();
	}

	@Test
	@Category(PortedToSearch6.class)
	public void testPhraseQuery() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.phrase()
				.onField( "mythology" )
				.sentence( "Month whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).as( "test slop" ).hasResultSize( 0 );

		query = monthQb
				.phrase()
				.withSlop( 3 )
				.onField( "mythology" )
				.sentence( "Month whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).as( "test slop" ).hasResultSize( 1 );

		query = monthQb
				.phrase()
				.onField( "mythology" )
				.sentence( "whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class )
				.as( "test one term optimization" )
				.hasResultSize( 1 );


		//Does not work as the NGram filter does not seem to be skipping posiional increment between ngrams.
		//		query = monthQb
		//				.phrase()
		//					.onField( "mythology_ngram" )
		//					.sentence( "snobored" )
		//					.createQuery();
		//
		//		helper.assertThat( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2785")
	public void testPhraseQueryWithNormalizer() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		// Phrase queries on a normalized (non-tokenized) field will only work with single-word queries

		Query query = monthQb
				.phrase()
				.onField( "name" )
				.sentence( "February" )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb
				.phrase()
				.onField( "name" )
				.sentence( "february" )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb
				.phrase()
				.onField( "mythology_normalized" )
				.sentence( "Month whitening" )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb
				.phrase()
				.onField( "mythology_normalized" )
				.sentence( "month whitening" )
				.createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2479")
	public void testPhraseQueryTermCreation() throws Exception {
		String testCaseText = "Test the Test test of your test test to test test test of test and Test budgeting.";
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.phrase()
				.onField( "mythology" )
				.sentence( testCaseText )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).as( "test term ordering" ).hasResultSize( 0 );
	}

	@Test
	public void testPhraseQueryWithStopWords() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.phrase()
				.onField( "mythology" )
				.sentence( "colder and whitening" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1074")
	public void testPhraseQueryWithNoTermsAfterAnalyzerApplication() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		// we use mythology_stem here as the default analyzer for Elasticsearch does not include a stopwords filter
		Query query = monthQb.phrase()
				.onField( "mythology_stem" )
				.sentence( "and" )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class )
				.as( "there should be no results, since all terms are stop words" )
				.matchesNone();
	}

	@Test
	public void testNumericRangeQueries() {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb
				.range()
				.onField( "raindropInMm" )
				.from( 0.23d )
				.to( 0.24d )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesExactlyIds( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1378")
	public void testNumericRangeQueryAbove() {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//inclusive
		Query query = monthQb
				.range()
				.onField( "raindropInMm" )
				.above( 0.231d )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesUnorderedIds( 1, 2, 3 );

		//exclusive
		query = monthQb
				.range()
				.onField( "raindropInMm" )
				.above( 0.231d )
				.excludeLimit()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesUnorderedIds( 2, 3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1378")
	public void testNumericRangeQueryBelow() {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//inclusive
		Query query = monthQb
				.range()
				.onField( "raindropInMm" )
				.below( 0.435d )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesUnorderedIds( 1, 2, 3 );

		//exclusive
		query = monthQb
				.range()
				.onField( "raindropInMm" )
				.below( 0.435d )
				.excludeLimit()
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).matchesUnorderedIds( 1 );
	}

	@Test
	public void testNumericFieldsTermQuery() {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		Query query = monthQb.keyword()
				.onField( "raindropInMm" )
				.matching( 0.231d )
				.createQuery();

		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-703")
	public void testPolymorphicQueryForUnindexedSuperTypeReturnsIndexedSubType() {
		final QueryBuilder builder = helper.queryBuilder( Object.class );

		Query query = builder.all().createQuery();
		helper.assertThatQuery( query ).from( Object.class )
				.as( "expected all instances of all indexed types" )
				.hasResultSize( 8 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-703")
	public void testPolymorphicQueryWithKeywordTermForUnindexedSuperTypeReturnsIndexedSubType() {
		final QueryBuilder builder = helper.queryBuilder( Car.class );

		Query query = builder.keyword().onField( "name" ).matching( "Morris" ).createQuery();
		helper.assertThatQuery( query ).matchesExactlyIds( 2 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-703")
	public void testObtainingBuilderForUnindexedTypeWithoutIndexedSubTypesCausesException() {
		try {
			helper.queryBuilder( Animal.class );

			fail( "Obtaining a builder not allowed for unindexed type without any indexed sub-types." );
		}
		catch (SearchException e) {
			// success
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2199")
	public void testCharFilters() throws Exception {
		final QueryBuilder monthQb = helper.queryBuilder( Month.class );

		//regular query
		Query query = monthQb.keyword().onField( "htmlDescription" ).matching( "strong" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 2 );

		query = monthQb.keyword().onField( "htmlDescription" ).matching( "em" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 2 );

		//using the HTMLStripCharFilter
		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "strong" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "em" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "month" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 3 );

		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "spring" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "fake" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "htmlDescription_htmlStrip" ).matching( "escaped" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3233")
	public void testOverridesForField() {
		final QueryBuilder monthQb = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( Month.class )
				.overridesForField( "mythology_ngram", AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP )
				.get();

		// If the analyzer is correctly overridden, only ngram of size 3 should produce results

		Query query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snowboard" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snowboar" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snowboa" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snowbo" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snowb" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snow" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 0 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "sno" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "now" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "owb" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "wbo" ).createQuery();
		helper.assertThatQuery( query ).from( Month.class ).hasResultSize( 1 );

		// etc.
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3233")
	public void testOverridesForFieldUnknownAnalyzer() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "Unknown analyzer: 'invalid_analyzer_name'" );

		QueryBuilder monthQb = sfHolder.getSearchFactory().buildQueryBuilder().forEntity( Month.class )
				.overridesForField( "mythology_ngram", "invalid_analyzer_name" )
				.get();

		monthQb.keyword().onField( "mythology_ngram" ).matching( "sno" ).createQuery();
	}

	private void indexTestData() {
		final Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		Date january = calendar.getTime();
		helper.add(
				new Month(
						"January",
						1,
						"Month of colder and whitening",
						"Historically colder than any other month in the northern hemisphere",
						january,
						0.231d,
						"<escaped>Month</escaped> of <em>colder</em> and <strong>whitening</strong>"
				)
		);
		calendar.set( 100 + 1900, 2, 12, 0, 0, 0 );
		february = calendar.getTime();
		helper.add(
				new Month(
						"February",
						2,
						"Month of snowboarding",
						"Historically, the month where we make babies while watching the whitening landscape",
						february,
						0.435d,
						"Month of <em>snowboarding</em>"
				)
		);
		calendar.set( 1800, 2, 12, 0, 0, 0 );
		Date march = calendar.getTime();
		helper.add(
				new Month(
						"March",
						3,
						"Month of fake spring",
						"Historically, the month in which we actually find time to go snowboarding.",
						march,
						0.435d,
						"Month of <strong>fake</strong> spring"
				)
		);

		Car car = new SportsCar( 1, "Leyland", 100 );
		helper.add( car );

		car = new SportsCar( 2, "Morris", 180 );
		helper.add( car );

		Day day = new Day( 1, 1 );
		helper.add( day );

		day = new Day( 2, 2 );
		helper.add( day );

		CoffeeBrand brand = new CoffeeBrand();
		brand.setId( 0 );
		brand.setName( "Tasty, Inc." );

		Coffee coffee = new Coffee();
		coffee.setId( "Peruvian Gold" );
		coffee.setName( "Peruvian Gold" );
		coffee.setBrand( brand );
		helper.add( coffee );
	}
}
