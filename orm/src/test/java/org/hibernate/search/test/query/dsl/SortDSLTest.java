/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.hibernate.Session;
import org.hibernate.Transaction;
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
import org.hibernate.search.query.dsl.sort.DistanceMethod;
import org.hibernate.search.query.dsl.sort.MultiValuedMode;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class SortDSLTest extends SearchTestBase {
	private static final Log log = LoggerFactory.make();

	private final Calendar calendar = GregorianCalendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.ROOT );

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

		Sort score = monthQb.sort().byScore().asc().createSort();
		Sort docid = monthQb.sort().byIndexOrder().createSort();
		Sort singleField = monthQb.sort().byField( "foo" ).createSort();
		Sort missingValue = monthQb.sort().byField( "foo" ).onMissingValue().use( "foo" ).ignoreFieldBridge().createSort();
		Sort multipleFieldsAndScore = monthQb.sort().byField( "foo" ).andByField( "bar" ).desc().andByScore().createSort();
		Sort distance = monthQb.sort().byField( "foo" ).fromCoordinates( null ).createSort();
		Sort distanceAndScore = monthQb.sort().byField( "foo" ).fromLatitude( 0 ).andLongitude( 0 )
				.desc()
				.in( Unit.KM ).withComputeMethod( DistanceMethod.ARC )
				.onMissingValue().sortFirst()
				.andByScore().createSort();
		Sort distanceByQuery = monthQb.sort().byDistanceFromSpatialQuery( null ).asc().andByScore().createSort();
		Sort comparator = monthQb.sort().byField( "foo" ).withComparator( null ).createSort();
		Sort nativesort = monthQb.sort().byNative( SortField.FIELD_SCORE ).andByField( "foo" ).desc().andByNative( SortField.FIELD_DOC ).createSort();
		Sort multiValued = monthQb.sort().byField( "notes" ).treatMultiValuedAs( MultiValuedMode.AVG ).createSort();

		transaction.commit();
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
		final Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
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
						0.231d,
						"jan",
						"<escaped>Month</escaped> of <em>colder</em> and <strong>whitening</strong>"
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
						0.435d,
						"feb",
						"Month of <em>snowboarding</em>"
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
						0.435d,
						"-mar",
						"Month of <strong>fake</strong> spring"
				)
		);

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
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Month.class,
				Car.class,
				SportsCar.class,
				Animal.class,
				Day.class,
				CoffeeBrand.class,
				Coffee.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.MODEL_MAPPING, MappingFactory.class.getName() );
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
					.param( "maxGramSize", "3" )
					.analyzerDef( "htmlStrip", StandardTokenizerFactory.class )
					.charFilter( HTMLStripCharFilterFactory.class )
					.param( "escapedTags", "escaped" )
					.filter( LowerCaseFilterFactory.class );
			return mapping;
		}
	}
}
