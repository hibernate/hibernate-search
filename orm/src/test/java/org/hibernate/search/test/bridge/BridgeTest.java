/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.builtin.StringEncodingCalendarBridge;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BridgeTest extends SearchTestBase {
	@Test
	public void testDefaultAndNullBridges() throws Exception {
		Cloud cloud = new Cloud();
		cloud.setMyDate( null );
		cloud.setDouble1( null );
		cloud.setDouble2( 2.1d );
		cloud.setIntegerv1( null );
		cloud.setIntegerv2( 2 );
		cloud.setFloat1( null );
		cloud.setFloat2( 2.1f );
		cloud.setLong1( null );
		cloud.setLong2( 2l );
		cloud.setString( null );
		cloud.setType( CloudType.DOG );
		cloud.setChar1( null );
		cloud.setChar2( 'P' );
		cloud.setStorm( false );
		cloud.setClazz( Cloud.class );
		cloud.setUri( new URI( "http://www.hibernate.org" ) );
		cloud.setUrl( new URL( "http://www.hibernate.org" ) );
		cloud.setUuid( UUID.fromString( "f49c6ba8-8d7f-417a-a255-d594dddf729f" ) );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		Query query;
		List result;

		BooleanQuery booleanQuery = new BooleanQuery.Builder()
				.add( NumericRangeQuery.newDoubleRange( "double2", 2.1, 2.1, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newFloatRange( "float2", 2.1f, 2.1f, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newIntRange( "integerv2", 2, 3, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newLongRange( "long2", 2l, 3l, true, true ), BooleanClause.Occur.MUST )
				.add( new TermQuery( new Term( "type", "dog" ) ), BooleanClause.Occur.MUST )
				.add( new TermQuery( new Term( "storm", "false" ) ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "find primitives and do not fail on null", 1, result.size() );

		booleanQuery = new BooleanQuery.Builder()
				.add( NumericRangeQuery.newDoubleRange( "double1", 2.1, 2.1, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newFloatRange( "float1", 2.1f, 2.1f, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newIntRange( "integerv1", 2, 3, true, true ), BooleanClause.Occur.MUST )
				.add( NumericRangeQuery.newLongRange( "long1", 2l, 3l, true, true ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "null elements should not be stored", 0, result.size() ); //the query is dumb because restrictive

		query = parser.parse( "type:dog" );
		result = session.createFullTextQuery( query ).setProjection( "type" ).list();
		assertEquals( "Enum projection works", 1, result.size() ); //the query is dumb because restrictive

		query = new TermQuery( new Term( "clazz", Cloud.class.getName() ) );
		result = session.createFullTextQuery( query ).setProjection( "clazz" ).list();
		assertEquals( "Clazz projection works", 1, result.size() );
		assertEquals(
				"Clazz projection works",
				Cloud.class.getName(),
				( (Class) ( (Object[]) result.get( 0 ) )[0] ).getName()
		);

		BooleanQuery bQuery = new BooleanQuery.Builder()
				.add( new TermQuery( new Term( "uri", "http://www.hibernate.org" ) ), BooleanClause.Occur.MUST )
				.add( new TermQuery( new Term( "url", "http://www.hibernate.org" ) ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( bQuery ).setProjection( "clazz" ).list();
		assertEquals( "Clazz projection works", 1, result.size() );

		bQuery = new BooleanQuery.Builder()
				.add( new TermQuery( new Term( "uuid", "f49c6ba8-8d7f-417a-a255-d594dddf729f" ) ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( bQuery ).setProjection( "clazz" ).list();
		assertEquals( "Clazz projection works", 1, result.size() );

		query = parser.parse( "char1:[" + String.valueOf( Character.MIN_VALUE ) + " TO " + String.valueOf( Character.MAX_VALUE ) + "]" );
		result = session.createFullTextQuery( query ).setProjection( "char1" ).list();
		assertEquals( "Null elements should not be stored, CharacterBridge is not working", 0, result.size() );

		query = parser.parse( "char2:P" );
		result = session.createFullTextQuery( query ).setProjection( "char2" ).list();
		assertEquals( "Wrong results number, CharacterBridge is not working", 1, result.size() );
		assertEquals( "Wrong result, CharacterBridge is not working", 'P', ( (Object[]) result.get( 0 ) )[0] );

		tx.commit();
		s.close();

	}

	@Test
	public void testCustomBridges() throws Exception {
		Cloud cloud = new Cloud();
		cloud.setCustomFieldBridge( "This is divided by 2" );
		cloud.setCustomStringBridge( "This is div by 4" );
		cloud.setChar2( 's' );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", TestConstants.simpleAnalyzer );
		Query query;
		List result;

		query = parser.parse( "customFieldBridge:This AND customStringBridge:This" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "Properties not mapped", 1, result.size() );

		query = parser.parse( "customFieldBridge:by AND customStringBridge:is" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "Custom types not taken into account", 0, result.size() );

		tx.commit();
		s.close();

	}

	@Test
	@SkipForDialect(PostgreSQL81Dialect.class)//PosgreSQL doesn't allow storing null with these column types
	public void testDateBridge() throws Exception {
		Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ROOT ); //for the sake of tests
		c.set( 2000, Calendar.DECEMBER, 15, 3, 43, 2 );
		c.set( Calendar.MILLISECOND, 5 );
		Date date = new Date( c.getTimeInMillis() );

		Cloud cloud = new Cloud();
		cloud.setMyDate( date ); //5 millisecond
		cloud.setDateDay( date );
		cloud.setDateHour( date );
		cloud.setDateMillisecond( date );
		cloud.setDateMinute( date );
		cloud.setDateMonth( date );
		cloud.setDateSecond( date );
		cloud.setDateYear( date );

		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		Date myDate = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		NumericRangeQuery numericRangeQuery = NumericRangeQuery.newLongRange(
				"myDate", myDate.getTime(), myDate.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateDay = DateTools.round( date, DateTools.Resolution.DAY );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateDay", dateDay.getTime(), dateDay.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMonth = DateTools.round( date, DateTools.Resolution.MONTH );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateMonth", dateMonth.getTime(), dateMonth.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateYear = DateTools.round( date, DateTools.Resolution.YEAR );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateYear", dateYear.getTime(), dateYear.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateHour = DateTools.round( date, DateTools.Resolution.HOUR );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateHour", dateHour.getTime(), dateHour.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMinute = DateTools.round( date, DateTools.Resolution.MINUTE );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateMinute", dateMinute.getTime(), dateMinute.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateSecond = DateTools.round( date, DateTools.Resolution.SECOND );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateSecond", dateSecond.getTime(), dateSecond.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMillisecond = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"dateMillisecond", dateMillisecond.getTime(), dateMillisecond.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		BooleanQuery booleanQuery = booleanQueryBuilder.build();
		List result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "Date not found or not properly truncated", 1, result.size() );

		tx.commit();
		s.close();
	}

	@Test
	public void testCalendarBridge() throws Exception {
		Cloud cloud = new Cloud();
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ROOT ); //for the sake of tests
		calendar.set( 2000, 11, 15, 3, 43, 2 );
		calendar.set( Calendar.MILLISECOND, 5 );

		cloud.setMyCalendar( calendar ); // 5 millisecond
		cloud.setCalendarDay( calendar );
		cloud.setCalendarHour( calendar );
		cloud.setCalendarMillisecond( calendar );
		cloud.setCalendarMinute( calendar );
		cloud.setCalendarMonth( calendar );
		cloud.setCalendarSecond( calendar );
		cloud.setCalendarYear( calendar );
		cloud.setChar2( 's' );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		Date date = calendar.getTime();
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		Date myDate = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		NumericRangeQuery numericRangeQuery = NumericRangeQuery.newLongRange(
				"myCalendar", myDate.getTime(), myDate.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateDay = DateTools.round( date, DateTools.Resolution.DAY );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarDay", dateDay.getTime(), dateDay.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMonth = DateTools.round( date, DateTools.Resolution.MONTH );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarMonth", dateMonth.getTime(), dateMonth.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateYear = DateTools.round( date, DateTools.Resolution.YEAR );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarYear", dateYear.getTime(), dateYear.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateHour = DateTools.round( date, DateTools.Resolution.HOUR );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarHour", dateHour.getTime(), dateHour.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMinute = DateTools.round( date, DateTools.Resolution.MINUTE );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarMinute", dateMinute.getTime(), dateMinute.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateSecond = DateTools.round( date, DateTools.Resolution.SECOND );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarSecond", dateSecond.getTime(), dateSecond.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMillisecond = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		numericRangeQuery = NumericRangeQuery.newLongRange(
				"calendarMillisecond", dateMillisecond.getTime(), dateMillisecond.getTime(), true, true
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		BooleanQuery booleanQuery = booleanQueryBuilder.build();
		List result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "Calendar not found or not properly truncated", 1, result.size() );

		tx.commit();
		s.close();

		//now unit-test the bridge directly:

		StringEncodingCalendarBridge bridge = new StringEncodingCalendarBridge();
		HashMap<String, String> bridgeParams = new HashMap<String, String>();
		bridgeParams.put( "resolution", Resolution.YEAR.toString() );
		bridge.setParameterValues( bridgeParams );
		assertEquals( "2000", bridge.objectToString( calendar ) );
		bridgeParams.put( "resolution", Resolution.DAY.toString() );
		bridge.setParameterValues( bridgeParams );
		assertEquals( "20001215", bridge.objectToString( calendar ) );
	}

	@Test
	@Category(SkipOnElasticsearch.class) // Elasticsearch uses a specific encoding for dates
	@SkipForDialect(PostgreSQL81Dialect.class)//PosgreSQL doesn't allow storing null with these column types
	public void testDateBridgeStringEncoding() throws Exception {
		Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ROOT ); //for the sake of tests
		c.set( 2000, Calendar.DECEMBER, 15, 3, 43, 2 );
		c.set( Calendar.MILLISECOND, 5 );
		Date date = new Date( c.getTimeInMillis() );

		Cloud cloud = new Cloud();
		cloud.setDateDay( date );

		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		TermQuery termQuery = new TermQuery( new Term( "dateDayStringEncoding",
				DateTools.dateToString( date, DateTools.Resolution.DAY ) ) );

		List result = session.createFullTextQuery( termQuery ).list();
		assertEquals( "Date not found or not properly truncated", 1, result.size() );

		tx.commit();
		s.close();
	}

	@Test
	@Category(SkipOnElasticsearch.class) // Elasticsearch uses a specific encoding for calendars
	public void testCalendarBridgeStringEncoding() throws Exception {
		Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "Europe/Rome" ), Locale.ROOT ); //for the sake of tests
		c.set( 2000, Calendar.DECEMBER, 15, 3, 43, 2 );
		c.set( Calendar.MILLISECOND, 5 );

		Cloud cloud = new Cloud();
		cloud.setCalendarDay( c );

		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		TermQuery termQuery = new TermQuery( new Term( "calendarDayStringEncoding",
				DateTools.dateToString( c.getTime(), DateTools.Resolution.DAY ) ) );

		List result = session.createFullTextQuery( termQuery ).list();
		assertEquals( "Calendar not found or not properly truncated", 1, result.size() );

		tx.commit();
		s.close();
	}

	@Test
	public void testIncorrectSetBridge() throws Exception {
		IncorrectSet incorrect = new IncorrectSet();
		incorrect.setSubIncorrect( new IncorrectSet.SubIncorrect() );
		incorrect.getSubIncorrect().setName( "This is a name not a class" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( incorrect );
			s.flush();
			s.flushToIndexes();
			fail( "Incorrect bridge should fail" );
		}
		catch (BridgeException e) {
			tx.rollback();
		}
		catch (HibernateException e) {
			final Throwable throwable = e.getCause();
			if ( throwable instanceof BridgeException ) {
				//expected
				assertTrue( throwable.getMessage().contains( "class: " + IncorrectSet.class.getName() ) );
				assertTrue( throwable.getMessage().contains( "path: subIncorrect.name" ) );
				tx.rollback();
			}
			else {
				e.printStackTrace();
				fail( "Incorrect bridge should raise a SearchException: " + e.toString() );
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail( "Incorrect bridge should raise a SearchException" );
		}
		s.close();
	}

	@Test
	public void testIncorrectGetBridge() throws Exception {
		IncorrectGet incorrect = new IncorrectGet();
		incorrect.setSubIncorrect( new IncorrectGet.SubIncorrect() );
		incorrect.getSubIncorrect().setName( "This is a name not a class" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( incorrect );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		final QueryBuilder builder = s.getSearchFactory().buildQueryBuilder().forEntity( IncorrectGet.class ).get();
		final Query query = builder.keyword().onField( "subIncorrect.name" ).matching( "name" ).createQuery();

		try {
			final FullTextQuery textQuery = s.createFullTextQuery( query, IncorrectGet.class ).setProjection( "subIncorrect.name" );
			textQuery.list();
			fail( "Incorrect bridge should fail" );
		}
		catch (BridgeException e) {
			tx.rollback();
		}
		catch (HibernateException e) {
			final Throwable throwable = e.getCause();
			if ( throwable instanceof BridgeException ) {
				//expected
				//System.out.println( throwable.getMessage() );
				assertTrue( throwable.getMessage().contains( "class: " + IncorrectGet.class.getName() ) );
				assertTrue( throwable.getMessage().contains( "path: subIncorrect.name" ) );
				tx.rollback();
			}
			else {
				e.printStackTrace();
				fail( "Incorrect bridge should raise a SearchException: " + e.toString() );
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail( "Incorrect bridge should raise a SearchException" );
		}
		s.close();
	}

	@Test
	public void testIncorrectObjectToStringBridge() throws Exception {
		IncorrectObjectToString incorrect = new IncorrectObjectToString();
		incorrect.setName( "test" );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		try {
			s.persist( incorrect );
			s.flush();
			s.flushToIndexes();
			fail( "Incorrect bridge should fail" );
		}
		catch (BridgeException e) {
			tx.rollback();
		}
		catch (HibernateException e) {
			final Throwable throwable = e.getCause();
			if ( throwable instanceof BridgeException ) {
				//expected
				assertTrue( throwable.getMessage().contains( "class: " + IncorrectObjectToString.class.getName() ) );
				assertTrue( throwable.getMessage().contains( "path: id" ) );
				tx.rollback();
			}
			else {
				e.printStackTrace();
				fail( "Incorrect bridge should raise a SearchException: " + e.toString() );
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			fail( "Incorrect bridge should raise a SearchException" );
		}
		s.close();
	}


	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Cloud.class,
				IncorrectSet.class,
				IncorrectGet.class,
				IncorrectObjectToString.class
		};
	}


	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}
}
