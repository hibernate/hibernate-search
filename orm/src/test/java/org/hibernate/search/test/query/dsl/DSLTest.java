/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query.dsl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.NGramFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.apache.solr.analysis.StopFilterFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.bridge.builtin.impl.String2FieldBridgeAdaptor;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.impl.ConnectedTermMatchingContext;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//DO NOT AUTO INDENT THIS FILE.
//MY DSL IS BEAUTIFUL, DUMB INDENTATION IS SCREWING IT UP
public class DSLTest extends SearchTestCase {
	private final Calendar calendar = Calendar.getInstance();

	private FullTextSession fullTextSession;
	private Date january;
	private Date february;
	private Date march;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Override
	public void tearDown() throws Exception {
		cleanUpTestData();
		super.tearDown();
	}

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

		assertEquals( "test quad tree based spatial query", 1, results.size() );
		assertEquals( "test quad tree based spatial query", "Bozo", ( (POI) results.get( 0 ) ).getName() );

		query = builder
				.spatial()
					.onCoordinates( "location" )
					.within( 500, Unit.KM )
						.ofLatitude( 48.858333d ).andLongitude( 2.294444d )
					.createQuery();
		results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test quad tree based spatial query", 1, results.size() );
		assertEquals( "test quad tree based spatial query", "Tour Eiffel", ( (POI) results.get( 0 ) ).getName() );

		transaction.commit();
	}

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

		assertEquals( "expected all instances of all indexed types", 7, results.size() );

		transaction.commit();
	}

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

		tx.commit();
		fullTextSession.clear();
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
		assertEquals( 7, results.size() );

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
				Animal.class
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
