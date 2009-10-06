/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.bridge;

import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class BridgeTest extends SearchTestCase {
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
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		Query query;
		List result;

		query = parser.parse(
				"double2:[2.1 TO 2.1] AND float2:[2.1 TO 2.1] " +
						"AND integerv2:[2 TO 2.1] AND long2:[2 TO 2.1] AND type:\"dog\" AND storm:false"
		);

		result = session.createFullTextQuery( query ).list();
		assertEquals( "find primitives and do not fail on null", 1, result.size() );

		query = parser.parse( "double1:[2.1 TO 2.1] OR float1:[2.1 TO 2.1] OR integerv1:[2 TO 2.1] OR long1:[2 TO 2.1]" );
		result = session.createFullTextQuery( query ).list();
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
				( ( Class ) ( ( Object[] ) result.get( 0 ) )[0] ).getName()
		);

		BooleanQuery bQuery = new BooleanQuery();
		bQuery.add( new TermQuery( new Term( "uri", "http://www.hibernate.org" ) ), BooleanClause.Occur.MUST );
		bQuery.add( new TermQuery( new Term( "url", "http://www.hibernate.org" ) ), BooleanClause.Occur.MUST );

		result = session.createFullTextQuery( bQuery ).setProjection( "clazz" ).list();
		assertEquals( "Clazz projection works", 1, result.size() );

		query = parser.parse( "char1:[" + String.valueOf( Character.MIN_VALUE ) + " TO " + String.valueOf( Character.MAX_VALUE ) + "]" );
		result = session.createFullTextQuery( query ).setProjection( "char1" ).list();
		assertEquals( "Null elements should not be stored, CharacterBridge is not working", 0, result.size() );

		query = parser.parse( "char2:P" );
		result = session.createFullTextQuery( query ).setProjection( "char2" ).list();
		assertEquals( "Wrong results number, CharacterBridge is not working", 1, result.size() );
		assertEquals( "Wrong result, CharacterBridge is not working", 'P', ( ( Object[] ) result.get( 0 ) )[0] );

		s.delete( s.get( Cloud.class, cloud.getId() ) );
		tx.commit();
		s.close();

	}

	public void testCustomBridges() throws Exception {
		Cloud cloud = new Cloud();
		cloud.setCustomFieldBridge( "This is divided by 2" );
		cloud.setCustomStringBridge( "This is div by 4" );
		org.hibernate.Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cloud );
		s.flush();
		tx.commit();

		tx = s.beginTransaction();
		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", new SimpleAnalyzer() );
		Query query;
		List result;

		query = parser.parse( "customFieldBridge:This AND customStringBridge:This" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "Properties not mapped", 1, result.size() );

		query = parser.parse( "customFieldBridge:by AND customStringBridge:is" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "Custom types not taken into account", 0, result.size() );

		s.delete( s.get( Cloud.class, cloud.getId() ) );
		tx.commit();
		s.close();

	}

	public void testDateBridge() throws Exception {
		Cloud cloud = new Cloud();
		Calendar c = GregorianCalendar.getInstance();
		c.setTimeZone( TimeZone.getTimeZone( "GMT" ) ); //for the sake of tests
		c.set( 2000, 11, 15, 3, 43, 2 );
		c.set( Calendar.MILLISECOND, 5 );

		Date date = new Date( c.getTimeInMillis() );
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
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		Query query;
		List result;

		query = parser.parse(
				"myDate:[19900101 TO 20060101]"
						+ " AND dateDay:[20001214 TO 2000121501]"
						+ " AND dateMonth:[200012 TO 20001201]"
						+ " AND dateYear:[2000 TO 200001]"
						+ " AND dateHour:[20001214 TO 2000121503]"
						+ " AND dateMinute:[20001214 TO 200012150343]"
						+ " AND dateSecond:[20001214 TO 20001215034302]"
						+ " AND dateMillisecond:[20001214 TO 20001215034302005]"
		);
		result = session.createFullTextQuery( query ).list();
		assertEquals( "Date not found or not property truncated", 1, result.size() );

		s.delete( s.get( Cloud.class, cloud.getId() ) );
		tx.commit();
		s.close();

	}

	protected Class[] getMappings() {
		return new Class[] {
				Cloud.class
		};
	}


	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, SimpleAnalyzer.class.getName() );
	}
}
