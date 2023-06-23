/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.Test;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

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
		cloud.setLong2( 2L );
		cloud.setString( null );
		cloud.setType( CloudType.DOG );
		cloud.setChar1( null );
		cloud.setChar2( 'P' );
		cloud.setStorm( false );
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
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		Query query;
		List result;

		BooleanQuery booleanQuery = new BooleanQuery.Builder()
				.add( DoublePoint.newExactQuery( "double2", 2.1 ), BooleanClause.Occur.MUST )
				.add( FloatPoint.newExactQuery( "float2", 2.1f ), BooleanClause.Occur.MUST )
				.add( IntPoint.newRangeQuery( "integerv2", 2, 3 ), BooleanClause.Occur.MUST )
				.add( LongPoint.newRangeQuery( "long2", 2L, 3L ), BooleanClause.Occur.MUST )
				.add( new TermQuery( new Term( "type", "dog" ) ), BooleanClause.Occur.MUST )
				.add( IntPoint.newExactQuery( "storm", 0 ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "find primitives and do not fail on null", 1, result.size() );

		booleanQuery = new BooleanQuery.Builder()
				.add( DoublePoint.newRangeQuery( "double1", 2.1, 2.1 ), BooleanClause.Occur.MUST )
				.add( FloatPoint.newRangeQuery( "float1", 2.1f, 2.1f ), BooleanClause.Occur.MUST )
				.add( IntPoint.newRangeQuery( "integerv1", 2, 3 ), BooleanClause.Occur.MUST )
				.add( LongPoint.newRangeQuery( "long1", 2L, 3L ), BooleanClause.Occur.MUST )
				.build();

		result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "null elements should not be stored", 0, result.size() ); //the query is dumb because restrictive

		query = parser.parse( "type:dog" );
		result = session.createFullTextQuery( query ).setProjection( "type" ).list();
		assertEquals( "Enum projection works", 1, result.size() ); //the query is dumb because restrictive

		query = new TermQuery( new Term( "uri", "http://www.hibernate.org" ) ); //the query is dumb because restrictive
		result = session.createFullTextQuery( query ).setProjection( "uri" ).list();
		assertEquals( "URI works", 1, result.size() );

		query = new TermQuery( new Term( "url", "http://www.hibernate.org" ) ); //the query is dumb because restrictive
		result = session.createFullTextQuery( query ).setProjection( "url" ).list();
		assertEquals( "URL works", 1, result.size() );

		query = new TermQuery( new Term( "uuid", "f49c6ba8-8d7f-417a-a255-d594dddf729f" ) );
		result = session.createFullTextQuery( query ).setProjection( "uuid" ).list();
		assertEquals( "UUID works", 1, result.size() );

		query = parser.parse(
				"char1:[" + String.valueOf( Character.MIN_VALUE ) + " TO " + String.valueOf( Character.MAX_VALUE - 2 ) + "]" );
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
		cloud.setChar2( 's' ); // Avoid errors with PostgreSQL ("invalid byte sequence for encoding "UTF8": 0x00")

		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );

		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

		Date myDate = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		Query numericRangeQuery = LongPoint.newRangeQuery(
				"myDate", myDate.getTime(), myDate.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateDay = DateTools.round( date, DateTools.Resolution.DAY );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateDay", dateDay.getTime(), dateDay.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMonth = DateTools.round( date, DateTools.Resolution.MONTH );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateMonth", dateMonth.getTime(), dateMonth.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateYear = DateTools.round( date, DateTools.Resolution.YEAR );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateYear", dateYear.getTime(), dateYear.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateHour = DateTools.round( date, DateTools.Resolution.HOUR );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateHour", dateHour.getTime(), dateHour.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMinute = DateTools.round( date, DateTools.Resolution.MINUTE );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateMinute", dateMinute.getTime(), dateMinute.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateSecond = DateTools.round( date, DateTools.Resolution.SECOND );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateSecond", dateSecond.getTime(), dateSecond.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMillisecond = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		numericRangeQuery = LongPoint.newRangeQuery(
				"dateMillisecond", dateMillisecond.getTime(), dateMillisecond.getTime()
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
		cloud.setChar2( 's' ); // Avoid errors with PostgreSQL ("invalid byte sequence for encoding "UTF8": 0x00")
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
		Query numericRangeQuery = LongPoint.newRangeQuery(
				"myCalendar", myDate.getTime(), myDate.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateDay = DateTools.round( date, DateTools.Resolution.DAY );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarDay", dateDay.getTime(), dateDay.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMonth = DateTools.round( date, DateTools.Resolution.MONTH );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarMonth", dateMonth.getTime(), dateMonth.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateYear = DateTools.round( date, DateTools.Resolution.YEAR );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarYear", dateYear.getTime(), dateYear.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateHour = DateTools.round( date, DateTools.Resolution.HOUR );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarHour", dateHour.getTime(), dateHour.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMinute = DateTools.round( date, DateTools.Resolution.MINUTE );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarMinute", dateMinute.getTime(), dateMinute.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateSecond = DateTools.round( date, DateTools.Resolution.SECOND );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarSecond", dateSecond.getTime(), dateSecond.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		Date dateMillisecond = DateTools.round( date, DateTools.Resolution.MILLISECOND );
		numericRangeQuery = LongPoint.newRangeQuery(
				"calendarMillisecond", dateMillisecond.getTime(), dateMillisecond.getTime()
		);
		booleanQueryBuilder.add( numericRangeQuery, BooleanClause.Occur.MUST );

		BooleanQuery booleanQuery = booleanQueryBuilder.build();
		List result = session.createFullTextQuery( booleanQuery ).list();
		assertEquals( "Calendar not found or not properly truncated", 1, result.size() );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Cloud.class
		};
	}

}
