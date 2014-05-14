/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.fest.assertions.Condition;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.cfg.SearchMapping;
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
import org.junit.Before;
import org.junit.Test;

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
	private Date january;
	private Date february;
	private Date march;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@After
	public void tearDown() throws Exception {
		cleanUpTestData();
		super.tearDown();
	}

	@Test
	public void testUseOfFieldBridge() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		Query query = monthQb.keyword().onField( "monthValue" ).matching( 2 ).createQuery();
		assertEquals( 1, fullTextSession.createFullTextQuery( query, Month.class ).getResultSize() );

		query = monthQb.keyword()
				.onField( "monthValue" )
					.ignoreFieldBridge()
				.matching( "2" )
				.createQuery();
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

	@Test
	public void testRangeQueryFromTo() throws Exception {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder monthQb = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Month.class ).get();

		calendar.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		calendar.set( 1900, 2, 12, 0, 0, 0 );
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
					.from( DateTools.dateToString( from, DateTools.Resolution.MINUTE ) )
					.to( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) )
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
					.below( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) )
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
					.above( DateTools.dateToString( to, DateTools.Resolution.MINUTE ) )
					.createQuery();
		hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		// test the limits, inclusive
		query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( february )
					.createQuery();
		hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
		assertEquals( 1, hibQuery.getResultSize() );
		assertEquals( "February", ( (Month) hibQuery.list().get( 0 ) ).getName() );

		// test the limits, exclusive
		query = monthQb
				.range()
					.onField( "estimatedCreation" )
					.andField( "justfortest" )
						.ignoreFieldBridge().ignoreAnalyzer()
					.above( february ).excludeLimit()
					.createQuery();
		hibQuery = fullTextSession.createFullTextQuery( query, Month.class );
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
					.onCoordinates( "location" )
					.within( 51, Unit.KM )
						.ofCoordinates( coordinates )
					.createQuery();

		List<?> results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test spatial hash based spatial query", 1, results.size() );
		assertEquals( "test spatial hash based spatial query", "Bozo", ( (POI) results.get( 0 ) ).getName() );

		query = builder
				.spatial()
					.onCoordinates( "location" )
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

		assertEquals( "expected all instances of all indexed types", 29, results.size() );

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
	@SuppressWarnings( "unchecked" )
	public void testMoreLikeThisBasicBehavior() {
		boolean outputLogs = true;
		Transaction transaction = fullTextSession.beginTransaction();
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			Query mltQuery = qb
					.moreLikeThis()
					.favorSignificantTermsWithFactor( 1 )
					.comparingAllFields()
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			assertThat( results ).isNotEmpty();

			Set<Term> terms = new HashSet<Term>( 100 );
			mltQuery.extractTerms( terms );
			assertThat( terms )
					.describedAs( "internalDescription should be ignored" )
					.doesNotSatisfy(
							new Condition<Collection<?>>() {
								@Override
								public boolean matches(Collection<?> value) {
									for ( Term term : (Collection<Term>) value ) {
										if ( "internalDescription".equals( term.field() ) ) {
											return true;
										}
									}
									return false;
								}
							}
					);
			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, results );

			//custom fields
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			assertThat( results ).isNotEmpty();
			assertThat( mltQuery instanceof BooleanQuery );
			BooleanQuery topMltQuery = (BooleanQuery) mltQuery;
			// FIXME: I'd prefer a test that uses data instead of how the query is actually built
			assertThat( topMltQuery.getClauses() ).onProperty( "query.boost" ).contains( 1f, 10f );

			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, results );

			//using non compatible field
			try {
				qb
						.moreLikeThis()
						.comparingField( "summary" )
						.andField( "internalDescription" )
						.toEntityWithId( decaffInstance.getId() )
						.createQuery();
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.as( "Internal description is neither stored nor store termvectors" )
						.contains( "internalDescription" );
			}
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings( "unchecked" )
	public void testMoreLikeThisToEntity() {
		boolean outputLogs = true;
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// query results to compare toEntity() results against
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// pass entity itself in a managed state
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( decaffInstance )
					.createQuery();
			List<Object[]> entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, entityResults );

			// pass entity itself with a matching id but different values
			// the id should take precedene
			Coffee nonMatchingOne = (Coffee) results.get( results.size() - 1 )[0];
			Coffee copyOfDecaffInstance = new Coffee();
			copyOfDecaffInstance.setId( decaffInstance.getId() );
			copyOfDecaffInstance.setInternalDescription( nonMatchingOne.getInternalDescription() );
			copyOfDecaffInstance.setName( nonMatchingOne.getName() );
			copyOfDecaffInstance.setDescription( nonMatchingOne.getDescription() );
			copyOfDecaffInstance.setIntensity( nonMatchingOne.getIntensity() );
			copyOfDecaffInstance.setSummary( nonMatchingOne.getSummary() );
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( copyOfDecaffInstance )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, entityResults );

			// pass entity itself with the right values but no id
			copyOfDecaffInstance = new Coffee();
			copyOfDecaffInstance.setInternalDescription( decaffInstance.getInternalDescription() );
			copyOfDecaffInstance.setName( decaffInstance.getName() );
			copyOfDecaffInstance.setDescription( decaffInstance.getDescription() );
			copyOfDecaffInstance.setIntensity( decaffInstance.getIntensity() );
			copyOfDecaffInstance.setSummary( decaffInstance.getSummary() );
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntity( copyOfDecaffInstance )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();

			// query from id and from the managed entity should match
			assertThat( entityResults ).isNotEmpty();
			assertThat( entityResults ).hasSize( results.size() );
			for ( int index = 0; index < entityResults.size(); index++ ) {
				Object[] real = entityResults.get( index );
				Object[] expected = results.get( index );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}

			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, entityResults );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings( "unchecked" )
	public void testMoreLikeThisExcludingEntityBeingCompared() {
		boolean outputLogs = true;
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> results;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );

			// exclude comparing entity
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			results = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			mltQuery = qb
					.moreLikeThis()
					.excludeEntityUsedForComparison()
					.comparingField( "summary" ).boostedTo( 10f )
					.andField( "description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			List<Object[]> resultsWoComparingEntity = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( resultsWoComparingEntity ).hasSize( results.size() - 1 );
			for ( int index = 0; index < resultsWoComparingEntity.size(); index++ ) {
				Object[] real = resultsWoComparingEntity.get( index );
				Object[] expected = results.get( index + 1 );
				assertThat( real[1] ).isEqualTo( expected[1] );
				assertThat( ( (Coffee) real[0] ).getId() ).isEqualTo( ( (Coffee) expected[0] ).getId() );
			}
			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, resultsWoComparingEntity );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings( "unchecked" )
	public void testMoreLikeThisOnCompressedFields() {
		boolean outputLogs = true;
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> entityResults;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// using compressed field
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "brand.description" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( entityResults ).isNotEmpty();
			Long matchingElements = (Long) fullTextSession.createQuery(
					"select count(*) from Coffee c where c.brand.name like '%pony'"
			).uniqueResult();
			assertThat( entityResults ).hasSize( matchingElements.intValue() );
			float score = -1;
			for ( Object[] element : entityResults ) {
				if ( score == -1 ) {
					score = (Float) element[1];
				}
				assertThat( element[1] ).as( "All scores should be equal as the same brand is used" ).isEqualTo( score );
			}
			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, entityResults );
		}
		finally {
			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings( "unchecked" )
	public void testMoreLikeThisOnEmbeddedFields() {
		boolean outputLogs = true;
		Transaction transaction = fullTextSession.beginTransaction();
		Query mltQuery;
		List<Object[]> entityResults;
		try {
			QueryBuilder qb = getCoffeeQueryBuilder();
			Coffee decaffInstance = getDecaffInstance( qb );
			// using fields from IndexedEmbedded
			mltQuery = qb
					.moreLikeThis()
					.comparingField( "brand.name" )
					.toEntityWithId( decaffInstance.getId() )
					.createQuery();
			entityResults = (List<Object[]>) fullTextSession
					.createFullTextQuery( mltQuery, Coffee.class )
					.setProjection( ProjectionConstants.THIS, ProjectionConstants.SCORE )
					.list();
			assertThat( entityResults ).isNotEmpty();
			Long matchingElements = (Long) fullTextSession.createQuery(
					"select count(*) from Coffee c where c.brand.name like '%pony'"
			).uniqueResult();
			assertThat( entityResults ).hasSize( matchingElements.intValue() );
			float score = -1;
			for ( Object[] element : entityResults ) {
				if ( score == -1 ) {
					score = (Float) element[1];
				}
				assertThat( element[1] ).as( "All scores should be equal as the same brand is used" ).isEqualTo( score );
			}
			outputQueryAndResults( outputLogs, decaffInstance, mltQuery, entityResults );

			// using indexed embedded id from document
			try {
				qb
						.moreLikeThis()
						.comparingField( "brand.id" )
						.toEntityWithId( decaffInstance.getId() )
						.createQuery();
			}
			catch (SearchException e) {
				assertThat( e.getMessage() )
						.as( "Field cannot be used" )
						.contains( "brand.id" );
			}
		}
		finally {
			transaction.commit();
		}
	}

	private QueryBuilder getCoffeeQueryBuilder() {
		return fullTextSession.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Coffee.class )
				.get();
	}

	private Coffee getDecaffInstance(QueryBuilder qb) {
		Query decaff = qb.keyword().onField( "name" ).matching( "Decaffeinato" ).createQuery();
		return (Coffee) fullTextSession.createFullTextQuery( decaff, Coffee.class )
				.list()
				.get( 0 );
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
		january = calendar.getTime();
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
		march = calendar.getTime();
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

		CoffeeBrand brandPony = new CoffeeBrand();
		brandPony.setName( "My little pony" );
		brandPony.setDescription( "Sells goods for horseback riding and good coffee blends" );
		fullTextSession.persist( brandPony );
		CoffeeBrand brandMonkey = new CoffeeBrand();
		brandMonkey.setName( "Monkey Monkey Do" );
		brandPony.setDescription( "Offers mover services via monkeys instead of trucks for difficult terrains. Coffees from this brand make monkeys work much faster." );
		fullTextSession.persist( brandMonkey );
		createCoffee(
				"Kazaar",
				"EXCEPTIONALLY INTENSE AND SYRUPY",
				"A daring blend of two Robustas from Brazil and Guatemala, specially prepared for Nespresso, and a separately roasted Arabica from South America, Kazaar is a coffee of exceptional intensity. Its powerful bitterness and notes of pepper are balanced by a full and creamy texture.",
				12,
				brandMonkey
		);
		createCoffee(
				"Dharkan",
				"LONG ROASTED AND VELVETY",
				"This blend of Arabicas from Latin America and Asia fully unveils its character thanks to the technique of long roasting at a low temperature. Its powerful personality reveals intense roasted notes together with hints of bitter cocoa powder and toasted cereals that express themselves in a silky and velvety txture.",
				11,
				brandPony
		);
		createCoffee(
				"Ristretto",
				"POWERFUL AND CONTRASTING",
				"A blend of South American and East African Arabicas, with a touch of Robusta, roasted separately to create the subtle fruity note of this full-bodied, intense espresso.",
				10,
				brandMonkey
		);
		createCoffee(
				"Arpeggio",
				"INTENSE AND CREAMY",
				"A dark roast of pure South and Central American Arabicas, Arpeggio has a strong character and intense body, enhanced by cocoa notes.",
				9,
				brandPony
		);
		createCoffee(
				"Roma",
				"FULL AND BALANCED",
				"The balance of lightly roasted South and Central American Arabicas with Robusta, gives Roma sweet and woody notes and a full, lasting taste on the palate.",
				8,
				brandMonkey
		);
		createCoffee(
				"Livanto",
				"ROUND AND BALANCED",
				"A pure Arabica from South and Central America, Livanto is a well-balanced espresso characterised by a roasted caramelised note.",
				6,
				brandMonkey
		);
		createCoffee(
				"Capriccio",
				"RICH AND DISTINCTIVE",
				"Blending South American Arabicas with a touch of Robusta, Capriccio is an espresso with a rich aroma and a strong typical cereal note.",
				5,
				brandMonkey
		);
		createCoffee(
				"Volluto",
				"SWEET AND LIGHT",
				"A pure and lightly roasted Arabica from South America, Volluto reveals sweet and biscuity flavours, reinforced by a little acidity and a fruity note.",
				4,
				brandMonkey
		);
		createCoffee(
				"Cosi",
				"LIGHT AND LEMONY",
				"Pure, lightly roasted East African, Central and South American Arabicas make Cosi a light-bodied espresso with refreshing citrus notes.",
				3,
				brandMonkey
		);
		createCoffee(
				"Indriya from India",
				"POWERFUL AND SPICY",
				"Indriya from India is the noble marriage of Arabicas with a hint of Robusta from southern India. It is a full-bodied espresso, which has a distinct personality with notes of spices.",
				10,
				brandMonkey
		);
		createCoffee(
				"Rosabaya de Colombia",
				"FRUITY AND BALANCED",
				"This blend of fine, individually roasted Colombian Arabicas, develops a subtle acidity with typical red fruit and winey notes.",
				6,
				brandMonkey
		);
		createCoffee(
				"Dulsão do Brasil",
				"SWEET AND SATINY SMOOTH",
				"A pure Arabica coffee, Dulsão do Brasil is a delicate blend of red and yellow Bourbon beans from Brazil. Its satiny smooth, elegantly balanced flavor is enhanced with a note of delicately toasted grain.",
				4,
				brandMonkey
		);
		createCoffee(
				"Bukeela ka Ethiopia",
				"",
				"This delicate Lungo expresses a floral bouquet reminiscent of jasmine, white lily, bergamot and orange blossom together with notes of wood. A pure Arabica blend composed of two very different coffees coming from the birthplace of coffee, Ethiopia. The blend’s coffees are roasted separately: one portion short and dark to guarantee the body, the other light but longer to preserve the delicate notes.",
				3,
				brandMonkey
		);
		createCoffee(
				"Fortissio Lungo",
				"RICH AND INTENSE",
				"Made from Central and South American Arabicas with just a hint of Robusta, Fortissio Lungo is an intense full-bodied blend with bitterness, which develops notes of dark roasted beans.",
				7,
				brandMonkey
		);
		createCoffee(
				"Vivalto Lungo",
				"COMPLEX AND BALANCED",
				"Vivalto Lungo is a balanced coffee made from a complex blend of separately roasted South American and East African Arabicas, combining roasted and subtle floral notes.",
				4,
				brandMonkey
		);
		createCoffee(
				"Linizio Lungo",
				"ROUND AND SMOOTH",
				"Mild and well-rounded on the palate, Linizio Lungo is a blend of fine Arabicas enhancing malt and cereal notes.",
				4,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato Intenso",
				"DENSE AND POWERFUL",
				"Dark roasted South American Arabicas with a touch of Robusta bring out the subtle cocoa and roasted cereal notes of this full-bodied decaffeinated espresso.",
				7,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato Lungo",
				"LIGHT AND FULL-FLAVOURED",
				"The slow roasting of this blend of South American Arabicas with a touch of Robusta gives Decaffeinato Lungo a smooth, creamy body and roasted cereal flavour.",
				3,
				brandMonkey
		);
		createCoffee(
				"Decaffeinato",
				"FRUITY AND DELICATE",
				"A blend of South American Arabicas reinforced with just a touch of Robusta is lightly roasted to reveal an aroma of red fruit.",
				2,
				brandPony
		);
		createCoffee(
				"Caramelito",
				"CARAMEL FLAVOURED",
				"The sweet flavour of caramel softens the roasted notes of the Livanto Grand Cru. This delicate gourmet marriage evokes the creaminess of soft toffee.",
				6,
				brandMonkey
		);
		createCoffee(
				"Ciocattino",
				"CHOCOLATE FLAVOURED",
				"Dark and bitter chocolate notes meet the caramelized roast of the Livanto Grand Cru. A rich combination reminiscent of a square of dark chocolate.",
				6,
				brandMonkey
		);
		createCoffee(
				"Vanilio",
				"VANILLA FLAVOURED",
				"A balanced harmony between the rich and the velvety aromas of vanilla and the mellow flavour of the Livanto Grand Cru. A blend distinguished by its full flavour, infinitely smooth and silky on the palate.",
				6,
				brandMonkey
		);

		tx.commit();
		fullTextSession.clear();
	}

	private void createCoffee(String title, String summary, String description, int intensity, CoffeeBrand brand) {
		Coffee coffee = new Coffee();
		coffee.setName( title );
		coffee.setSummary( summary );
		coffee.setDescription( description );
		coffee.setIntensity( intensity );
		coffee.setInternalDescription(
				"Same internal description of coffee and blend that would make things look quite the same."
		);
		coffee.setBrand( brand );
		fullTextSession.persist( coffee );
	}

	private void cleanUpTestData() {
		if ( !fullTextSession.isOpen() ) {
			return;
		}
		Transaction tx = fullTextSession.getTransaction();
		if ( tx.isActive() ) {
			//to not hide reason for test failures, as it otherwise causes a nested transaction not supported exception
			tx.commit();
		}
		tx = fullTextSession.beginTransaction();
		@SuppressWarnings("unchecked")
		final List<Object> results = fullTextSession.createQuery( "from " + Object.class.getName() ).list();
		assertEquals( 31, results.size() );

		for ( Object entity : results ) {
			fullTextSession.delete( entity );
		}

		tx.commit();
		fullTextSession.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Month.class,
				POI.class,
				Car.class,
				SportsCar.class,
				Animal.class,
				Coffee.class,
				CoffeeBrand.class
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
