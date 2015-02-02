/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.impl.ConnectedTermMatchingContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//DO NOT AUTO INDENT THIS FILE.
//MY DSL IS BEAUTIFUL, DUMB INDENTATION IS SCREWING IT UP
public class DSLTest extends SearchTestBase {
	private static final Log log = LoggerFactory.make();

	private final Calendar calendar = Calendar.getInstance();

	private FullTextSession fullTextSession;
	private Date february;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testUseOfFieldBridge() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.keyword().onField( "monthValue" ).matching( 2 ).createQuery();
		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();
	}

	@Test
	public void testUseOfCustomFieldBridgeInstance() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		ConnectedTermMatchingContext termMatchingContext = (ConnectedTermMatchingContext) monthQb
				.keyword()
				.onField( MonthClassBridge.FIELD_NAME_1 );

		Query query = termMatchingContext
				.withFieldBridge( new String2FieldBridgeAdaptor( new RomanNumberFieldBridge() ) )
				.matching( 2 )
				.createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );
		transaction.commit();
	}

	@Test
	public void testUseOfMultipleCustomFieldBridgeInstances() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//Rather complex code here as we're not exposing the #withFieldBridge methods on the public interface
		final ConnectedTermMatchingContext field1Context = (ConnectedTermMatchingContext) monthQb
				.keyword()
				.onField( MonthClassBridge.FIELD_NAME_1 );

		final ConnectedTermMatchingContext field2Context = (ConnectedTermMatchingContext) field1Context
					.withFieldBridge( new String2FieldBridgeAdaptor( new RomanNumberFieldBridge() ) )
				.andField( MonthClassBridge.FIELD_NAME_2 );

		Query query = field2Context
					.withFieldBridge( new String2FieldBridgeAdaptor( new RomanNumberFieldBridge() ) )
					.matching( 2 )
				.createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );
		transaction.commit();
	}

	@Test
	public void testTermQueryOnAnalyzer() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//regular term query
		Query query = monthQb.keyword().onField( "mythology" ).matching( "cold" ).createQuery();

		assertEquals( 0, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query based on several words
		query = monthQb.keyword().onField( "mythology" ).matching( "colder darker" ).createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query applying the analyzer and generating one term per word
		query = monthQb.keyword().onField( "mythology_stem" ).matching( "snowboard" ).createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query applying the analyzer and generating several terms per word
		query = monthQb.keyword().onField( "mythology_ngram" ).matching( "snobored" ).createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//term query not using analyzers
		query = monthQb.keyword().onField( "mythology" ).ignoreAnalyzer().matching( "Month" ).createQuery();

		assertEquals( 0, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();
	}

	@Test
	public void testFuzzyAndWildcardQuery() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();


		//fuzzy search with custom threshold and prefix
		Query query = monthQb
				.keyword()
					.fuzzy()
						.withThreshold( .8f )
						.withPrefixLength( 1 )
					.onField( "mythology" )
					.matching( "calder" )
					.createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//fuzzy search on multiple fields
		query = monthQb
				.keyword()
				.fuzzy()
				.withThreshold( .8f )
				.withPrefixLength( 1 )
				.onFields( "mythology", "history" )
				.matching( "showboarding" )
				.createQuery();

		assertEquals( 2, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		//wildcard query
		query = monthQb
				.keyword()
					.wildcard()
					.onField( "mythology" )
					.matching( "mon*" )
					.createQuery();

		assertEquals( 3, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testQueryCustomization() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();


		//combined query, January and february both contain whitening but February in a longer text
		Query query = monthQb
				.bool()
				.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
				.should( monthQb.keyword().onField( "history" ).matching( "whitening" ).createQuery() )
				.createQuery();

		List<Month> results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//boosted query, January and february both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb
				.bool()
				.should( monthQb.keyword().onField( "mythology" ).matching( "whitening" ).createQuery() )
				.should( monthQb.keyword().onField( "history" ).boostedTo( 30 ).matching( "whitening" ).createQuery() )
				.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		//FIXME add other method tests besides boostedTo

		transaction.commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMultipleFields() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//combined query, January and february both contain whitening but February in a longer text
		Query query = monthQb.keyword()
				.onField( "mythology" )
				.andField( "history" )
				.matching( "whitening" )
				.createQuery();

		List<Month> results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//combined query, January and february both contain whitening but February in a longer text
		query = monthQb.keyword()
				.onFields( "mythology", "history" )
					.boostedTo( 30 )
				.matching( "whitening" ).createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//boosted query, January and february both contain whitening but February in a longer text
		//since history is boosted, February should come first though
		query = monthQb.keyword()
				.onField( "mythology" )
				.andField( "history" )
					.boostedTo( 30 )
				.matching( "whitening" )
				.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );

		transaction.commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBoolean() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//must
		Query query = monthQb
				.bool()
				.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.createQuery();

		List<Month> results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 1, results.size() );
		assertEquals( "January", results.get( 0 ).getName() );

		//must not + all
		query = monthQb
				.bool()
					.should( monthQb.all().createQuery() )
					.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
						.not()
					.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );
		assertEquals( "March", results.get( 1 ).getName() );

		//implicit must not + all (not recommended)
		query = monthQb
				.bool()
					.must( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
						.not()
					.createQuery();
		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );
		assertEquals( "March", results.get( 1 ).getName() );

		//all except (recommended)
		query = monthQb
				.all()
					.except( monthQb.keyword().onField( "mythology" ).matching( "colder" ).createQuery() )
				.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertEquals( 2, results.size() );
		assertEquals( "February", results.get( 0 ).getName() );
		assertEquals( "March", results.get( 1 ).getName() );


		transaction.commit();
	}

	@Test(expected = SearchException.class)
	public void testIllegalBooleanJunction() {
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		//forgetting to set any condition on the boolean, an exception shall be thrown:
		BooleanJunction<BooleanJunction> booleanJunction = monthQb.bool();
		assertTrue( booleanJunction.isEmpty() );
		Query query = booleanJunction.createQuery();
		Assert.fail( "should not reach this point" );
	}

	@Test
	public void testRangeQueryFromTo() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		Date from = calendar.getTime();
		calendar.set( 1910, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.from( from )
					.to( to ).excludeLimit()
					.createQuery();

		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb
				.range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.from( DateTools.round( from, DateTools.Resolution.MINUTE ) )
					.to( DateTools.round( to, DateTools.Resolution.MINUTE ) )
						.excludeLimit()
					.createQuery();
		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );
		transaction.commit();
	}

	@Test
	public void testRangeQueryBelow() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1800, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.below( to )
					.createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "March", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb
				.range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.below( DateTools.round( to, DateTools.Resolution.MINUTE ) )
					.createQuery();

		hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "March", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb.range()
				.onField( "raindropInMm" )
				.below( 0.24d )
				.createQuery();

		assertTrue( query.getClass().isAssignableFrom( NumericRangeQuery.class ) );

		List<?> results = fullTextSession.createFullTextQuery( query, Month.class ).list();

		assertEquals( "test range numeric ", 1, results.size() );
		assertEquals( "test range numeric ", "January", ( (Month) results.get( 0 ) ).getName() );

		transaction.commit();
	}

	@Test
	public void testRangeQueryAbove() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 10 + 1900, 2, 12, 0, 0, 0 );
		Date to = calendar.getTime();

		Query query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( to )
					.createQuery();
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		query = monthQb
				.range()
					.onField( "estimatedCreation" )
						.ignoreFieldBridge()
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( DateTools.round( to, DateTools.Resolution.MINUTE ) )
					.createQuery();
		hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		transaction.commit();
	}

	@Test
	public void testRangeQueryAboveInclusive() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		// test the limits, inclusive
		Query query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( february )
					.createQuery();
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( "Wrong number of query results", 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		transaction.commit();
	}

	@Test
	public void testRangeQueryAboveExclusive() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		// test the limits, exclusive
		Query query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
						.above( february ).excludeLimit()
					.createQuery();
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 0, hibQuery.getResultSize() );

		transaction.commit();
	}

	@Test
	public void testPhraseQuery() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb
				.phrase()
					.onField( "mythology" )
					.sentence( "colder and whitening" )
					.createQuery();

		assertEquals(
				"test exact phrase", 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize()
		);

		query = monthQb
				.phrase()
					.onField( "mythology" )
					.sentence( "Month whitening" )
					.createQuery();

		assertEquals( "test slop", 0, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb
				.phrase()
					.withSlop( 3 )
					.onField( "mythology" )
					.sentence( "Month whitening" )
					.createQuery();

		assertEquals( "test slop", 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb
				.phrase()
					.onField( "mythology" )
					.sentence( "whitening" )
					.createQuery();

		assertEquals(
				"test one term optimization",
				1,
				fullTextSession.createFullTextQuery( query, Month.class ).getResultSize()
		);


		//Does not work as the NGram filter does not seem to be skipping posiional increment between ngrams.
//		query = monthQb
//				.phrase()
//					.onField( "mythology_ngram" )
//					.sentence( "snobored" )
//					.createQuery();
//
//		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		transaction.commit();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1074")
	public void testPhraseQueryWithNoTermsAfterAnalyzerApplication() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.
				phrase()
				.onField( "mythology" )
				.sentence( "and" )
				.createQuery();

		assertEquals(
				"there should be no results, since all terms are stop words",
				0,
				fullTextSession.createFullTextQuery( query, Month.class ).getResultSize()
		);
		transaction.commit();
	}

	@Test
	public void testNumericRangeQueries() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb
				.range()
					.onField( "raindropInMm" )
					.from( 0.23d )
					.to( 0.24d )
					.createQuery();

		assertTrue( query.getClass().isAssignableFrom( NumericRangeQuery.class ) );

		List<?> results = fullTextSession.createFullTextQuery( query, Month.class ).list();

		assertEquals( "test range numeric ", 1, results.size() );
		assertEquals( "test range numeric ", "January", ( (Month) results.get( 0 ) ).getName() );


		transaction.commit();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1378")
	public void testNumericRangeQueryAbove() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//inclusive
		Query query = monthQb
				.range()
					.onField( "raindropInMm" )
					.above( 0.231d )
					.createQuery();

		assertTrue( query.getClass().isAssignableFrom( NumericRangeQuery.class ) );

		List<?> results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertThat( results ).onProperty( "name" ).containsOnly( "January", "February", "March" );

		//exclusive
		query = monthQb
				.range()
					.onField( "raindropInMm" )
					.above( 0.231d )
					.excludeLimit()
					.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertThat( results ).onProperty( "name" ).containsOnly( "February", "March" );

		transaction.commit();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-1378")
	public void testNumericRangeQueryBelow() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		//inclusive
		Query query = monthQb
				.range()
					.onField( "raindropInMm" )
					.below( 0.435d )
					.createQuery();

		assertTrue( query.getClass().isAssignableFrom( NumericRangeQuery.class ) );

		List<?> results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertThat( results ).onProperty( "name" ).containsOnly( "January", "February", "March" );

		//exclusive
		query = monthQb
				.range()
					.onField( "raindropInMm" )
					.below( 0.435d )
					.excludeLimit()
					.createQuery();

		results = fullTextSession.createFullTextQuery( query, Month.class ).list();
		assertThat( results ).onProperty( "name" ).containsOnly( "January" );

		transaction.commit();
	}

	@Test
	public void testNumericFieldsTermQuery() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.keyword()
				.onField( "raindropInMm" )
				.matching( 0.231d )
				.createQuery();

		assertTrue( query.getClass().isAssignableFrom( NumericRangeQuery.class ) );

		assertEquals(
				"test term numeric ", 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize()
		);

		transaction.commit();
	}

	@Test
	public void testFieldBridge() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();
		Query query = monthQb.keyword()
				.onField( "monthRomanNumber" )
				.matching( 2 )
				.createQuery();
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Month.class );
		List<?> results = fullTextQuery.list();
		assertEquals( 1, results.size() );
		Month february = (Month) results.get( 0 );
		assertEquals( 2, february.getMonthValue() );
		transaction.commit();
	}

	@Test
	public void testSpatialQueries() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		Coordinates coordinates = Point.fromDegrees( 24d, 31.5d );
		Query query = builder
				.spatial()
					.onField( "location" )
					.within( 51, Unit.KM )
						.ofCoordinates( coordinates )
					.createQuery();

		List<?> results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test spatial hash based spatial query", 1, results.size() );
		assertEquals( "test spatial hash based spatial query", "Bozo", ( (POI) results.get( 0 ) ).getName() );

		query = builder
				.spatial()
					.onField( "location" )
					.within( 500, Unit.KM )
						.ofLatitude( 48.858333d ).andLongitude( 2.294444d )
					.createQuery();
		results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test spatial hash based spatial query", 1, results.size() );
		assertEquals( "test spatial hash based spatial query", "Tour Eiffel", ( (POI) results.get( 0 ) ).getName() );

		transaction.commit();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-703" )
	public void testPolymorphicQueryForUnindexedSuperTypeReturnsIndexedSubType() {
		Transaction transaction = fullTextSession.beginTransaction();

		final QueryBuilder builder = fullTextSession
				.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Object.class )
				.get();

		Query query = builder.all().createQuery();
		List<?> results = fullTextSession.createFullTextQuery( query, Object.class ).list();

		assertEquals( "expected all instances of all indexed types", 10, results.size() );

		transaction.commit();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-703" )
	public void testPolymorphicQueryWithKeywordTermForUnindexedSuperTypeReturnsIndexedSubType() {
		Transaction transaction = fullTextSession.beginTransaction();

		final QueryBuilder builder = fullTextSession
				.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Car.class )
				.get();

		Query query = builder.keyword().onField( "name" ).matching( "Morris" ).createQuery();
		List<?> results = fullTextSession.createFullTextQuery( query ).list();

		assertEquals( "expected one instance of indexed sub-type", 1, results.size() );
		assertEquals( 180, ( (SportsCar) results.get( 0 ) ).getEnginePower() );

		transaction.commit();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-703" )
	public void testObtainingBuilderForUnindexedTypeWithoutIndexedSubTypesCausesException() {
		Transaction transaction = fullTextSession.beginTransaction();

		try {
			fullTextSession
				.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Animal.class )
				.get();

			fail( "Obtaining a builder not allowed for unindexed type without any indexed sub-types." );
		}
		catch (SearchException e) {
			// success
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1791")
	public void testUsingMatchQueryOnNumericDocumentIdGeneratesTermQuery() throws Exception {
		// making sure a string based TermQuery is used
		QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.keyword()
				.onField( "id" )
				.matching( 1 )
				.createQuery();
		assertTrue( "A string based TermQuery is expected, but got a " + query.getClass(), query instanceof TermQuery );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1791")
	public void testUsingRangeQueryOnNumericDocumentIdGeneratesTermRangeQuery() throws Exception {
		QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.range()
				.onField( "id" )
				.from( 1 )
				.to( 3 )
				.createQuery();
		assertTrue(
				"A string based TermQuery is expected, but got a " + query.getClass(), query instanceof TermRangeQuery
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1791")
	public void testUsingMatchingQueryOnNumericFieldCreatesNumericRangeQuery() throws Exception {
		// making sure a NumericRangeQuery is used
		QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Day.class ).get();

		Query query = monthQb.keyword()
				.onField( "idNumeric" )
				.matching( 2 )
				.createQuery();

		assertTrue(
				"A NumericRangeQuery is expected, but got a " + query.getClass(), query instanceof NumericRangeQuery
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1791")
	public void testUseMatchQueryOnEmbeddedNumericIdCreatesTermQuery() throws Exception {
		QueryBuilder coffeeQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Coffee.class ).get();

		Query query = coffeeQb.keyword()
				.onField( "brand.id" )
				.matching( 1 )
				.createQuery();

		assertTrue(
				"A TermQuery is expected, but got a " + query.getClass(), query instanceof TermQuery
		);
	}

	private void outputQueryAndResults(boolean outputLogs, Coffee originalInstance, Query mltQuery, List<Object[]> results) {
		// set to true to display results
		if ( outputLogs ) {
			StringBuilder builder = new StringBuilder( "Initial coffee: " )
					.append( originalInstance ) .append( "\n\n" )
					.append( "Query: " ).append( mltQuery.toString() ).append( "\n\n" )
					.append( "Matching coffees" ).append( "\n" );
			for ( Object[] entry : results ) {
				builder.append( "    Score: " ).append( entry[1] );
				builder.append( " | Coffee: " ).append( entry[0] ).append( "\n" );
			}
			log.debug( builder.toString() );
		}
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
		calendar.set( Calendar.MILLISECOND, 0 );
		Date january = calendar.getTime();
		fullTextSession.persist(
				new Month(
						"January",
						1,
						"Month of colder and whitening",
						"Historically colder than any other month in the northern hemisphere",
						january,
						0.231d
				)
		);
		calendar.set( 100 + 1900, 2, 12, 0, 0, 0 );
		february = calendar.getTime();
		fullTextSession.persist(
				new Month(
						"February",
						2,
						"Month of snowboarding",
						"Historically, the month where we make babies while watching the whitening landscape",
						february,
						0.435d
				)
		);
		calendar.set( 1800, 2, 12, 0, 0, 0 );
		Date march = calendar.getTime();
		fullTextSession.persist(
				new Month(
						"March",
						3,
						"Month of fake spring",
						"Historically, the month in which we actually find time to go snowboarding.",
						march,
						0.435d
				)
		);

		POI poi = new POI( 1, "Tour Eiffel", 48.858333d, 2.294444d, "Monument" );
		fullTextSession.persist( poi );
		poi = new POI( 2, "Bozo", 24d, 32d, "Monument" );
		fullTextSession.persist( poi );

		Car car = new SportsCar( 1, "Leyland", 100 );
		fullTextSession.persist( car );

		car = new SportsCar( 2, "Morris", 180 );
		fullTextSession.persist( car );

		Day day = new Day( 1, 1 );
		fullTextSession.persist( day );

		day = new Day( 2, 2 );
		fullTextSession.persist( day );

		CoffeeBrand brand = new CoffeeBrand();
		brand.setName( "Tasty, Inc." );
		fullTextSession.persist( brand );

		Coffee coffee = new Coffee();
		coffee.setName( "Peruvian Gold" );
		coffee.setBrand( brand );
		fullTextSession.persist( coffee );

		tx.commit();
		fullTextSession.clear();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Month.class,
				POI.class,
				Car.class,
				SportsCar.class,
				Animal.class,
				Day.class,
				CoffeeBrand.class,
				Coffee.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( Environment.MODEL_MAPPING, MappingFactory.class.getName() );
	}

	public static class MappingFactory {
		@Factory
		public SearchMapping build() {
			SearchMapping mapping = new SearchMapping();
			mapping
					.analyzerDef( "stemmer", StandardTokenizerFactory.class )
					.filter( StandardFilterFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( StopFilterFactory.class )
					.filter( SnowballPorterFilterFactory.class )
					.param( "language", "English" )
					.analyzerDef( "ngram", StandardTokenizerFactory.class )
					.filter( StandardFilterFactory.class )
					.filter( LowerCaseFilterFactory.class )
					.filter( StopFilterFactory.class )
					.filter( NGramFilterFactory.class )
					.param( "minGramSize", "3" )
					.param( "maxGramSize", "3" );
			return mapping;
		}
	}
}
